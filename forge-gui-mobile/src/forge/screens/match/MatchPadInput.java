package forge.screens.match;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Input.Keys;

import forge.Forge;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.input.Input;
import forge.gamemodes.match.input.InputAttack;
import forge.gamemodes.match.input.InputBlock;
import forge.gamemodes.match.input.InputPayMana;
import forge.interfaces.IGameController;
import forge.player.PlayerControllerHuman;
import forge.screens.match.views.VField;
import forge.screens.match.views.VPlayerPanel;
import forge.util.ThreadUtil;

/**
 * Gamepad adapters for in-match targeting flows (attack, block, targets, mana).
 */
public final class MatchPadInput {
    private MatchPadInput() {
    }

    public static PlayerControllerHuman getLocalHumanController() {
        IGameController controller = MatchController.instance.getGameController();
        if (controller instanceof PlayerControllerHuman human) {
            return human;
        }
        return null;
    }

    public static Input getCurrentInput() {
        PlayerControllerHuman human = getLocalHumanController();
        if (human == null) {
            return null;
        }
        return human.getInputQueue().getInput();
    }

    public static boolean isPlayerPadNavigationActive() {
        return getCurrentInput() instanceof InputAttack;
    }

    public static boolean isAttackPlayerConfirmActive() {
        return getCurrentInput() instanceof InputAttack;
    }

    public static boolean isBlockAttackerPadActive() {
        return getCurrentInput() instanceof InputBlock;
    }

    public static boolean isFieldPadSelectionFiltered() {
        return MatchController.instance.isSelecting() || getCurrentInput() instanceof InputPayMana;
    }

    public static boolean isValidFieldPadTarget(CardView card) {
        if (card == null) {
            return false;
        }
        if (MatchController.instance.isSelecting()) {
            return MatchController.instance.isSelectable(card);
        }
        if (getCurrentInput() instanceof InputPayMana) {
            return MatchController.instance.isWeaklySelectable(card);
        }
        return true;
    }

    /**
     * @return true if the key was consumed
     */
    public static boolean handleKey(MatchScreen screen, int keyCode) {
        if (!Forge.hasGamepad()) {
            return false;
        }
        if (isPlayerPadNavigationActive()) {
            switch (keyCode) {
                case Keys.DPAD_LEFT:
                case Keys.BUTTON_L1:
                    return cyclePlayerPanels(screen, -1, true);
                case Keys.DPAD_RIGHT:
                case Keys.BUTTON_R1:
                    return cyclePlayerPanels(screen, 1, true);
                default:
                    break;
            }
        }
        if (isBlockAttackerPadActive()) {
            switch (keyCode) {
                case Keys.DPAD_LEFT:
                case Keys.BUTTON_L1:
                    return cycleBlockAttackers(-1);
                case Keys.DPAD_RIGHT:
                case Keys.BUTTON_R1:
                    return cycleBlockAttackers(1);
                default:
                    break;
            }
        }
        return false;
    }

    public static boolean cyclePlayerPanels(MatchScreen screen, int direction, boolean selectPlayer) {
        List<VPlayerPanel> panels = screen.getPlayerPanelsList();
        if (panels.isEmpty()) {
            return false;
        }
        screen.selectedPlayerPanel().hideSelectedTab();
        int selected = Math.floorMod(screen.getSelectedPlayerIndex() + direction, panels.size());
        screen.setSelectedPlayerIndex(selected);
        screen.selectedPlayerPanel().closeSelectedTab();
        screen.selectedPlayerPanel().getSelectedRow().unselectCurrent();
        if (selectPlayer) {
            PlayerView player = screen.selectedPlayerPanel().getPlayer();
            if (player != null) {
                ThreadUtil.invokeInGameThread(() ->
                        MatchController.instance.getGameController().selectPlayer(player, null));
            }
        }
        return true;
    }

    public static boolean confirmPlayerOnSelectedPanel() {
        VPlayerPanel panel = MatchController.getView().selectedPlayerPanel();
        if (panel == null) {
            return false;
        }
        PlayerView player = panel.getPlayer();
        if (player == null) {
            return false;
        }
        ThreadUtil.invokeInGameThread(() ->
                MatchController.instance.getGameController().selectPlayer(player, null));
        return true;
    }

    private static boolean cycleBlockAttackers(int direction) {
        GameView game = MatchController.instance.getGameView();
        if (game == null || game.getCombat() == null) {
            return false;
        }
        List<CardView> attackers = new ArrayList<>();
        for (CardView attacker : game.getCombat().getAttackers()) {
            attackers.add(attacker);
        }
        if (attackers.isEmpty()) {
            return false;
        }
        int currentIndex = -1;
        for (int i = 0; i < attackers.size(); i++) {
            if (MatchController.instance.isHighlighted(attackers.get(i))) {
                currentIndex = i;
                break;
            }
        }
        int nextIndex = currentIndex < 0 ? 0 : Math.floorMod(currentIndex + direction, attackers.size());
        CardView nextAttacker = attackers.get(nextIndex);
        ThreadUtil.invokeInGameThread(() ->
                MatchController.instance.getGameController().selectCard(nextAttacker, null, null));
        return true;
    }

    // ---- Cross-zone cursor (L1/R1) --------------------------------------
    // Step a single focus cursor through an ordered list of zones spanning
    // every player: your hand, your two battlefield rows, then each
    // opponent's two rows. Lands on a zone and selects its first card, so
    // hand / opponent creatures / lands are all reachable without hunting
    // through the menu bar. Within-zone movement (d-pad), A/Y/B, and the
    // target/mana filtering all keep working via the existing handlers.

    private static final int KIND_HAND = 0;
    private static final int KIND_ROW1 = 1;
    private static final int KIND_ROW2 = 2;

    /** @return true if the key was consumed */
    public static boolean cycleZone(MatchScreen screen, int direction) {
        List<VPlayerPanel> panels = screen.getPlayerPanelsList();
        if (panels.isEmpty()) {
            return false;
        }
        List<int[]> slots = buildZoneSlots(screen, panels);
        if (slots.isEmpty()) {
            return false;
        }
        int current = currentSlotIndex(screen, slots);
        // step, skipping empty battlefield rows (the hand is always a valid stop)
        for (int i = 1; i <= slots.size(); i++) {
            int[] slot = slots.get(Math.floorMod(current + direction * i, slots.size()));
            if (slotHasContent(panels, slot)) {
                focusSlot(screen, slot);
                return true;
            }
        }
        return true; //nothing else to land on; still consume the key
    }

    private static List<int[]> buildZoneSlots(MatchScreen screen, List<VPlayerPanel> panels) {
        List<int[]> slots = new ArrayList<>();
        int bottomIdx = panels.indexOf(screen.getBottomPlayerPanel());
        if (bottomIdx >= 0) { //local player's zones first: hand, then both rows
            slots.add(new int[]{bottomIdx, KIND_HAND});
            slots.add(new int[]{bottomIdx, KIND_ROW1});
            slots.add(new int[]{bottomIdx, KIND_ROW2});
        }
        for (int i = 0; i < panels.size(); i++) {
            if (i == bottomIdx) {
                continue;
            }
            slots.add(new int[]{i, KIND_ROW1});
            slots.add(new int[]{i, KIND_ROW2});
        }
        return slots;
    }

    private static int currentSlotIndex(MatchScreen screen, List<int[]> slots) {
        int panelIdx = screen.getSelectedPlayerIndex();
        VPlayerPanel p = screen.selectedPlayerPanel();
        int kind;
        VPlayerPanel.InfoTab tab = p.getSelectedTab();
        if (tab != null && tab == p.getZoneTab(ZoneType.Hand)) {
            kind = KIND_HAND;
        } else {
            kind = (p.getSelectedRow() == p.getField().getRow2()) ? KIND_ROW2 : KIND_ROW1;
        }
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i)[0] == panelIdx && slots.get(i)[1] == kind) {
                return i;
            }
        }
        return 0;
    }

    private static boolean slotHasContent(List<VPlayerPanel> panels, int[] slot) {
        VPlayerPanel p = panels.get(slot[0]);
        switch (slot[1]) {
            case KIND_ROW2:
                return p.getField().getRow2().getChildCount() > 0;
            case KIND_ROW1:
                return p.getField().getRow1().getChildCount() > 0;
            case KIND_HAND:
            default:
                return true; //always allow landing on the hand
        }
    }

    private static void focusSlot(MatchScreen screen, int[] slot) {
        VPlayerPanel old = screen.selectedPlayerPanel();
        old.getSelectedRow().unselectCurrent();
        old.hideSelectedTab();

        screen.setSelectedPlayerIndex(slot[0]);
        VPlayerPanel p = screen.selectedPlayerPanel();
        if (slot[1] == KIND_HAND) {
            p.setSelectedZone(ZoneType.Hand);
            VPlayerPanel.InfoTab tab = p.getSelectedTab();
            if (tab != null && tab.getDisplayArea() != null) {
                tab.getDisplayArea().setNextSelected(1); //select first card in hand
            }
        } else {
            p.hideSelectedTab();
            VField.FieldRow row = (slot[1] == KIND_ROW2) ? p.getField().getRow2() : p.getField().getRow1();
            p.setSelectedRow(row);
            row.selectCurrent();
        }
        screen.revalidate(true);
    }
}
