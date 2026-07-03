package forge.toolbox.focus;

import forge.Forge;
import forge.Graphics;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FScrollPane;

/**
 * Container whose children can be navigated by gamepad using a shared {@link FocusNavigator}.
 */
public class FocusGroup extends FContainer {
    private final FocusNavigator navigator = new FocusNavigator();

    public FocusNavigator getNavigator() {
        return navigator;
    }

    public void setScrollPane(FScrollPane scrollPane) {
        navigator.setScrollPane(scrollPane);
    }

    @Override
    public <T extends FDisplayObject> T add(T child) {
        T added = super.add(child);
        if (child instanceof Focusable focusable) {
            navigator.register(focusable);
        } else if (child.isEnabled() && child.isVisible()) {
            navigator.register(child);
        }
        return added;
    }

    @Override
    public <T extends FDisplayObject> boolean remove(T child) {
        boolean removed = super.remove(child);
        if (removed) {
            navigator.refreshFromChildren(this);
        }
        return removed;
    }

    @Override
    public void clear() {
        navigator.clear();
        super.clear();
    }

    public void refreshFocusables() {
        navigator.refreshFromChildren(this);
    }

    @Override
    public boolean keyDown(int keyCode) {
        if (Forge.hasGamepad() && navigator.handleKey(keyCode)) {
            return true;
        }
        return super.keyDown(keyCode);
    }

    @Override
    protected void drawOverlay(Graphics g) {
        if (Forge.hasGamepad()) {
            navigator.drawFocusRing(g);
        }
        super.drawOverlay(g);
    }
}
