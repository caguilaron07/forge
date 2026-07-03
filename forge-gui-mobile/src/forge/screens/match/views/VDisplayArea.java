package forge.screens.match.views;

import forge.screens.match.MatchPadInput;
import forge.screens.match.MatchScreen;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FScrollPane;

import java.util.Arrays;

public abstract class VDisplayArea extends FScrollPane {
    private FDisplayObject selectedChild;
    private int selectedIndex = -1;
    public VDisplayArea() {
        setVisible(false); //hide by default
    }
    public abstract int getCount();
    public abstract void update();

    protected int getPadSelectedIndex() {
        return selectedIndex;
    }

    private boolean selectFilteredRelative(int step) {
        int childCount = getChildCount();
        if (childCount < 1) {
            selectedIndex = -1;
            return false;
        }
        int start = selectedIndex < 0 ? -1 : selectedIndex;
        for (int offset = 1; offset <= childCount; offset++) {
            int next = Math.floorMod(start + step * offset, childCount);
            FDisplayObject child = getChildAt(next);
            if (child instanceof FCardPanel panel
                    && !MatchPadInput.isValidFieldPadTarget(panel.getCard())) {
                continue;
            }
            selectedIndex = next;
            if (selectedChild != null) {
                selectedChild.setHovered(false);
            }
            selectedChild = child;
            selectedChild.setHovered(true);
            scrollIntoView(selectedChild);
            MatchScreen.setPotentialListener(Arrays.asList(selectedChild));
            return true;
        }
        return false;
    }

    public void setNextSelected(int val) {
        if (getChildCount() < 1) {
            selectedIndex = -1;
            return;
        }
        if (MatchPadInput.isFieldPadSelectionFiltered() && selectFilteredRelative(val)) {
            return;
        }
        if (selectedIndex == -1) {
            selectedIndex++;
            if (selectedChild != null)
                selectedChild.setHovered(false);
            selectedChild = getChildAt(selectedIndex);
            selectedChild.setHovered(true);
            scrollIntoView(selectedChild);
            MatchScreen.setPotentialListener(Arrays.asList(selectedChild));
            return;
        }
        if (selectedIndex+val < getChildCount()) {
            selectedIndex+=val;
            if (selectedChild != null)
                selectedChild.setHovered(false);
            selectedChild = getChildAt(selectedIndex);
            selectedChild.setHovered(true);
            scrollIntoView(selectedChild);
            MatchScreen.setPotentialListener(Arrays.asList(selectedChild));
        }
    }
    public void setPreviousSelected(int val) {
        if (getChildCount() < 1) {
            selectedIndex = -1;
            return;
        }
        if (MatchPadInput.isFieldPadSelectionFiltered() && selectFilteredRelative(-val)) {
            return;
        }
        if (selectedIndex-val > -1) {
            selectedIndex-=val;
            if (selectedChild != null)
                selectedChild.setHovered(false);
            selectedChild = getChildAt(selectedIndex);
            selectedChild.setHovered(true);
            scrollIntoView(selectedChild);
            MatchScreen.setPotentialListener(Arrays.asList(selectedChild));
        }
    }
    public void tapChild() {
        if (selectedChild instanceof FCardPanel)
            VCardDisplayArea.CardAreaPanel.get(((FCardPanel) selectedChild).getCard()).selectCard(false);
        else if (selectedChild instanceof VManaPool.ManaLabel)
            ((VManaPool.ManaLabel) selectedChild).activate();
    }
    public void showZoom() {
        if (selectedChild instanceof FCardPanel)
            VCardDisplayArea.CardAreaPanel.get(((FCardPanel) selectedChild).getCard()).showZoom();
    }
}
