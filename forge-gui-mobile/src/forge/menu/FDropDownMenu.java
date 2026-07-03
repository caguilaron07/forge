package forge.menu;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Input.Keys;

import forge.Forge;
import forge.Graphics;
import forge.toolbox.focus.FocusNavigator;

public abstract class FDropDownMenu extends FDropDown {
    protected final List<FMenuItem> items = new ArrayList<>();
    private int selected = -1;
    private final FocusNavigator padFocus = new FocusNavigator();

    public FDropDownMenu() {
    }

    @Override
    protected boolean autoHide() {
        return true;
    }

    protected abstract void buildMenu();

    @Override
    protected ScrollBounds updateAndGetPaneSize(float maxWidth, float maxVisibleHeight) {
        clear();
        items.clear();
        padFocus.clear();

        buildMenu();

        //ensure text is all aligned if some items have icons and others don't
        boolean allowForIcon = false;
        for (FMenuItem item : items) {
            if (item.hasIcon()) {
                allowForIcon = true;
                break;
            }
        }
        for (FMenuItem item : items) {
            item.setAllowForIcon(allowForIcon);
            padFocus.register(item);
        }

        //determine needed width of menu
        float width = determineMenuWidth();
        if (width > maxWidth) {
            width = maxWidth;
        }

        //set bounds for each item
        float y = 0;
        for (FMenuItem item : items) {
            item.setBounds(0, y, width, FMenuItem.HEIGHT);
            y += FMenuItem.HEIGHT;
        }

        if (Forge.hasGamepad() && !items.isEmpty()) {
            if (selected < 0 || selected >= items.size()) {
                selected = 0;
            }
            applySelection();
            padFocus.setFocusedIndex(selected);
        }

        return new ScrollBounds(width, y);
    }

    protected float determineMenuWidth() {
        float width = 0;
        for (FMenuItem item : items) {
            float minWidth = item.getMinWidth();
            if (width < minWidth) {
                width = minWidth;
            }
        }
        return width;
    }

    public void addItem(FMenuItem item) {
        if (item.isVisible()) {
            items.add(add(item));
        }
    }

    public void clearItems() {
        clear();
        items.clear();
        padFocus.clear();
    }

    @Override
    public boolean tap(float x, float y, int count) {
        super.tap(x, y, count);
        return !(getDropDownOwner() instanceof FSubMenu); //return false so owning sub menu can be hidden
    }

    @Override
    public void setNextSelected() {
        selectNextItem();
    }

    @Override
    public void setPreviousSelected() {
        selectPreviousItem();
    }

    @Override
    public boolean keyDown(int keyCode) {
        if (Forge.hasGamepad() && isVisible() && !items.isEmpty()) {
            padFocus.setScrollPane(this);
            switch (keyCode) {
                case Keys.DPAD_UP:
                case Keys.DPAD_DOWN:
                case Keys.DPAD_LEFT:
                case Keys.DPAD_RIGHT:
                    if (padFocus.handleKey(keyCode)) {
                        selected = padFocus.getFocusedIndex();
                        return true;
                    }
                    break;
                case Keys.BUTTON_A:
                    if (selected < 0 || selected >= items.size()) {
                        selected = 0;
                        applySelection();
                        padFocus.setFocusedIndex(selected);
                    }
                    selectedChild = items.get(selected);
                    tapChild();
                    return true;
                case Keys.BUTTON_B:
                case Keys.ESCAPE:
                    if (autoHide()) {
                        hide();
                    }
                    return true;
                default:
                    break;
            }
        }
        return super.keyDown(keyCode);
    }

    @Override
    protected void drawOverlay(Graphics g) {
        if (Forge.hasGamepad() && isVisible()) {
            padFocus.drawFocusRing(g);
        }
        super.drawOverlay(g);
    }

    private void selectNextItem() {
        if (items.isEmpty()) {
            return;
        }
        selected = (selected + 1) % items.size();
        applySelection();
        padFocus.setFocusedIndex(selected);
        super.setNextSelected();
    }

    private void selectPreviousItem() {
        if (items.isEmpty()) {
            return;
        }
        selected = selected <= 0 ? items.size() - 1 : selected - 1;
        applySelection();
        padFocus.setFocusedIndex(selected);
        super.setPreviousSelected();
    }

    private void applySelection() {
        clearHighlight();
        if (selected >= 0 && selected < items.size()) {
            items.get(selected).setHovered(true);
        }
    }

    private void clearHighlight() {
        for (FMenuItem item : items) {
            item.setHovered(false);
        }
    }

    @Override
    public void hide() {
        selected = -1;
        padFocus.clear();
        super.hide();
    }
}
