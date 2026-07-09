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
    // Step a single focus cursor through an ordered list of stops spanning
    // every player: the avatar (for targeting the player), the hand, the two
    // battlefield rows, then the side-column zone tabs (graveyard, library,
    // flashback, exile). Lands on a stop and selects its first card, so the
    // whole left column and both players' zones are reachable without hunting
    // through the menu bar. Within-zone movement (d-pad), A/Y/B, and the
    // target/mana filtering all keep working via the existing handlers.

    private static final int KIND_PLAYER = 0; //the avatar; A selects/targets the player
    private static final int KIND_ROW1 = 1;
    private static final int KIND_ROW2 = 2;
    private static final int KIND_TAB = 3; //a side-column zone tab (see Slot.zone)

    /** Side-column zone tabs to visit, in left-column order (hand is placed before the rows). */
    private static final ZoneType[] TAB_ZONES = {
            ZoneType.Graveyard, ZoneType.Library, ZoneType.Flashback, ZoneType.Exile
    };

    /**
     * True while the cursor is resting on a player's avatar. Can't be derived
     * from panel state (no tab selected + a row is always the "selected" row),
     * so it's tracked explicitly and cleared whenever the d-pad moves the
     * focus onto a card.
     */
    private static boolean playerFocused = false;

    public static boolean isPlayerFocused() {
        return playerFocused;
    }

    public static void clearPlayerFocus() {
        playerFocused = false;
    }

    private static final class Slot {
        final int panel;
        final int kind;
        final ZoneType zone; //only meaningful for KIND_TAB

        Slot(int panel, int kind, ZoneType zone) {
            this.panel = panel;
            this.kind = kind;
            this.zone = zone;
        }
    }

    /** @return true if the key was consumed */
    public static boolean cycleZone(MatchScreen screen, int direction) {
        List<VPlayerPanel> panels = screen.getPlayerPanelsList();
        if (panels.isEmpty()) {
            return false;
        }
        List<Slot> slots = buildZoneSlots(screen, panels);
        if (slots.isEmpty()) {
            return false;
        }
        int current = currentSlotIndex(screen, slots);
        // step, skipping empty stops (the avatar and hand are always valid)
        for (int i = 1; i <= slots.size(); i++) {
            Slot slot = slots.get(Math.floorMod(current + direction * i, slots.size()));
            if (slotHasContent(panels, slot)) {
                focusSlot(screen, slot);
                return true;
            }
        }
        return true; //nothing else to land on; still consume the key
    }

    private static List<Slot> buildZoneSlots(MatchScreen screen, List<VPlayerPanel> panels) {
        List<Slot> slots = new ArrayList<>();
        int bottomIdx = panels.indexOf(screen.getBottomPlayerPanel());
        if (bottomIdx >= 0) { //local player first
            addPanelSlots(slots, bottomIdx, true);
        }
        for (int i = 0; i < panels.size(); i++) {
            if (i == bottomIdx) {
                continue;
            }
            addPanelSlots(slots, i, false);
        }
        return slots;
    }

    private static void addPanelSlots(List<Slot> slots, int panelIdx, boolean local) {
        slots.add(new Slot(panelIdx, KIND_PLAYER, null));
        if (local) { //only the local player's hand is selectable
            slots.add(new Slot(panelIdx, KIND_TAB, ZoneType.Hand));
        }
        slots.add(new Slot(panelIdx, KIND_ROW1, null));
        slots.add(new Slot(panelIdx, KIND_ROW2, null));
        for (ZoneType zone : TAB_ZONES) {
            slots.add(new Slot(panelIdx, KIND_TAB, zone));
        }
    }

    private static int currentSlotIndex(MatchScreen screen, List<Slot> slots) {
        int panelIdx = screen.getSelectedPlayerIndex();
        VPlayerPanel p = screen.selectedPlayerPanel();
        int kind;
        ZoneType zone = null;
        VPlayerPanel.InfoTab tab = p.getSelectedTab();
        if (playerFocused) {
            kind = KIND_PLAYER;
        } else if (tab != null) {
            kind = KIND_TAB;
            zone = zoneOfTab(p, tab);
        } else {
            kind = (p.getSelectedRow() == p.getField().getRow2()) ? KIND_ROW2 : KIND_ROW1;
        }
        for (int i = 0; i < slots.size(); i++) {
            Slot s = slots.get(i);
            if (s.panel == panelIdx && s.kind == kind && s.zone == zone) {
                return i;
            }
        }
        return 0;
    }

    /** @return the zone whose tab is currently selected on the panel, or null if it isn't one we track. */
    private static ZoneType zoneOfTab(VPlayerPanel p, VPlayerPanel.InfoTab tab) {
        if (tab == p.getZoneTab(ZoneType.Hand)) {
            return ZoneType.Hand;
        }
        for (ZoneType zone : TAB_ZONES) {
            if (tab == p.getZoneTab(zone)) {
                return zone;
            }
        }
        return null;
    }

    private static boolean slotHasContent(List<VPlayerPanel> panels, Slot slot) {
        VPlayerPanel p = panels.get(slot.panel);
        switch (slot.kind) {
            case KIND_PLAYER:
                return true; //the avatar is always a valid stop (for targeting the player)
            case KIND_ROW2:
                return p.getField().getRow2().getChildCount() > 0;
            case KIND_ROW1:
                return p.getField().getRow1().getChildCount() > 0;
            case KIND_TAB:
            default:
                if (slot.zone == ZoneType.Hand) {
                    return true; //always allow landing on the hand
                }
                VPlayerPanel.InfoTab tab = p.getZoneTab(slot.zone);
                return tab != null && tab.getDisplayArea() != null && tab.getDisplayArea().getCount() > 0;
        }
    }

    private static void focusSlot(MatchScreen screen, Slot slot) {
        VPlayerPanel old = screen.selectedPlayerPanel();
        old.getSelectedRow().unselectCurrent();
        old.hideSelectedTab();

        screen.setSelectedPlayerIndex(slot.panel);
        VPlayerPanel p = screen.selectedPlayerPanel();
        playerFocused = (slot.kind == KIND_PLAYER);

        if (slot.kind == KIND_PLAYER) {
            //nothing to select in-panel; the orange avatar selector shows the focus
            p.getSelectedRow().unselectCurrent();
            p.hideSelectedTab();
        } else if (slot.kind == KIND_TAB) {
            p.setSelectedZone(slot.zone);
            VPlayerPanel.InfoTab tab = p.getSelectedTab();
            if (tab != null && tab.getDisplayArea() != null) {
                tab.getDisplayArea().setNextSelected(1); //select first card in the zone
            }
        } else {
            p.hideSelectedTab();
            VField.FieldRow row = (slot.kind == KIND_ROW2) ? p.getField().getRow2() : p.getField().getRow1();
            p.setSelectedRow(row);
            row.selectCurrent();
        }
        screen.revalidate(true);
    }
}
