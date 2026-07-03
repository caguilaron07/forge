package forge.screens.match;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Input.Keys;

import forge.Forge;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.gamemodes.match.input.Input;
import forge.gamemodes.match.input.InputAttack;
import forge.gamemodes.match.input.InputBlock;
import forge.gamemodes.match.input.InputPayMana;
import forge.gamemodes.match.input.InputSelectTargets;
import forge.interfaces.IGameController;
import forge.player.PlayerControllerHuman;
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
        Input input = getCurrentInput();
        return input instanceof InputAttack || input instanceof InputSelectTargets;
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
}
