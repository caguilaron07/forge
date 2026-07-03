package forge.toolbox.focus;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Rectangle;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FScrollPane;
import forge.util.Utils;

/**
 * Manages an ordered set of focusable elements with geometry-aware d-pad navigation.
 */
public class FocusNavigator {
    private static final float FOCUS_RING_THICKNESS = Utils.scale(2);

    private final List<Focusable> focusables = new ArrayList<>();
    private int focusedIndex = -1;
    private FScrollPane scrollPane;

    public void setScrollPane(FScrollPane scrollPane0) {
        scrollPane = scrollPane0;
    }

    public void clear() {
        clearFocusVisual();
        focusables.clear();
        focusedIndex = -1;
    }

    public void register(Focusable focusable) {
        if (focusable != null) {
            focusables.add(focusable);
            if (focusedIndex < 0 && focusable.isFocusable()) {
                setFocusedIndex(focusables.size() - 1);
            }
        }
    }

    public void registerDisplayObject(FDisplayObject object) {
        register(DisplayObjectFocusable.forObject(object));
    }

    public void refreshFromChildren(FContainer container) {
        clear();
        for (FDisplayObject child : container.getChildren()) {
            if (child instanceof Focusable focusable) {
                if (focusable.isFocusable()) {
                    register(focusable);
                }
            } else if (child.isEnabled() && child.isVisible()) {
                register(DisplayObjectFocusable.forObject(child));
            }
        }
        if (!focusables.isEmpty()) {
            setFocusedIndex(0);
        }
    }

    public int getFocusedIndex() {
        return focusedIndex;
    }

    public Focusable getFocused() {
        if (focusedIndex < 0 || focusedIndex >= focusables.size()) {
            return null;
        }
        return focusables.get(focusedIndex);
    }

    public void restoreFocus(Focusable target) {
        if (target == null) {
            return;
        }
        for (int i = 0; i < focusables.size(); i++) {
            if (sameFocusTarget(focusables.get(i), target)) {
                setFocusedIndex(i);
                return;
            }
        }
    }

    private static boolean sameFocusTarget(Focusable a, Focusable b) {
        if (a == b) {
            return true;
        }
        FDisplayObject ownerA = focusOwner(a);
        FDisplayObject ownerB = focusOwner(b);
        return ownerA != null && ownerA == ownerB;
    }

    private static FDisplayObject focusOwner(Focusable focusable) {
        if (focusable instanceof DisplayObjectFocusable adapter) {
            return adapter.getOwner();
        }
        if (focusable instanceof FDisplayObject object) {
            return object;
        }
        return null;
    }

    public void setFocusedIndex(int index) {
        if (focusables.isEmpty()) {
            focusedIndex = -1;
            return;
        }
        int next = Math.max(0, Math.min(index, focusables.size() - 1));
        if (next == focusedIndex) {
            return;
        }
        clearFocusVisual();
        focusedIndex = next;
        Focusable focused = getFocused();
        if (focused != null && focused.isFocusable()) {
            focused.onFocusGained();
            scrollFocusedIntoView();
        }
    }

    public boolean handleKey(int keyCode) {
        if (!Forge.hasGamepad() || focusables.isEmpty()) {
            return false;
        }

        switch (keyCode) {
            case Keys.DPAD_UP:
                return move(FocusDirection.UP);
            case Keys.DPAD_DOWN:
                return move(FocusDirection.DOWN);
            case Keys.DPAD_LEFT:
                return move(FocusDirection.LEFT);
            case Keys.DPAD_RIGHT:
                return move(FocusDirection.RIGHT);
            case Keys.BUTTON_A:
            case Keys.ENTER:
                return activate();
            default:
                return false;
        }
    }

    public boolean move(FocusDirection direction) {
        List<Integer> activeIndices = new ArrayList<>();
        List<Rectangle> activeBounds = new ArrayList<>();
        for (int i = 0; i < focusables.size(); i++) {
            Focusable focusable = focusables.get(i);
            if (focusable.isFocusable()) {
                activeIndices.add(i);
                activeBounds.add(focusable.getFocusBounds());
            }
        }
        if (activeIndices.isEmpty()) {
            return false;
        }

        int activeFocused = activeIndices.indexOf(focusedIndex);
        if (activeFocused < 0) {
            setFocusedIndex(activeIndices.get(0));
            return true;
        }

        int nextActive = FocusGeometry.findNearestNeighbor(activeBounds, activeFocused, direction);
        if (nextActive < 0) {
            return true;
        }
        setFocusedIndex(activeIndices.get(nextActive));
        return true;
    }

    public boolean activate() {
        Focusable focused = getFocused();
        if (focused == null || !focused.isFocusable()) {
            return false;
        }
        return focused.onFocusActivate();
    }

    public void drawFocusRing(Graphics g) {
        Focusable focused = getFocused();
        if (focused == null || !focused.isFocusable()) {
            return;
        }

        Rectangle bounds = focused.getFocusBounds();
        if (bounds.width <= 0 || bounds.height <= 0) {
            return;
        }

        FSkinColor ringColor = Forge.isMobileAdventureMode
                ? FSkinColor.get(Colors.ADV_CLR_ACTIVE)
                : FSkinColor.get(Colors.CLR_ACTIVE);
        float outerPad = FOCUS_RING_THICKNESS * 2.5f;
        float innerPad = FOCUS_RING_THICKNESS;
        g.drawRect(FOCUS_RING_THICKNESS * 1.5f, ringColor.alphaColor(0.55f),
                bounds.x - outerPad, bounds.y - outerPad,
                bounds.width + 2 * outerPad, bounds.height + 2 * outerPad);
        g.drawRect(FOCUS_RING_THICKNESS, ringColor,
                bounds.x - innerPad, bounds.y - innerPad,
                bounds.width + 2 * innerPad, bounds.height + 2 * innerPad);
    }

    private void scrollFocusedIntoView() {
        if (scrollPane == null) {
            return;
        }
        Focusable focused = getFocused();
        if (focused instanceof DisplayObjectFocusable adapter) {
            scrollPane.scrollIntoView(adapter.getOwner());
        }
    }

    private void clearFocusVisual() {
        Focusable previous = getFocused();
        if (previous != null) {
            previous.onFocusLost();
        }
    }
}
