package forge.util;

import com.badlogic.gdx.Input.Keys;

/**
 * Maps libGDX controller key codes to Xbox-style glyph atlas regions.
 * Steam Input and most desktop mappings present a consistent Xbox layout to the app.
 */
public final class ControllerGlyphs {
    /** Atlas region prefix for {@code forge-gui/res/adventure/common/skin/keys.atlas}. */
    public static final String ATLAS_PREFIX = "XBox_";

    private ControllerGlyphs() {
    }

    public static String getButtonSuffix(int keyCode) {
        return switch (keyCode) {
            case Keys.BUTTON_A -> "A";
            case Keys.BUTTON_B -> "B";
            case Keys.BUTTON_X -> "X";
            case Keys.BUTTON_Y -> "Y";
            case Keys.BUTTON_L1 -> "L";
            case Keys.BUTTON_R1 -> "R";
            case Keys.BUTTON_L2 -> "L2";
            case Keys.BUTTON_R2 -> "R2";
            case Keys.BUTTON_SELECT -> "Select";
            case Keys.BUTTON_START -> "Start";
            case Keys.DPAD_UP -> "Up";
            case Keys.DPAD_DOWN -> "Down";
            case Keys.DPAD_LEFT -> "Left";
            case Keys.DPAD_RIGHT -> "Right";
            default -> null;
        };
    }

    public static String getAtlasRegionName(int keyCode) {
        String suffix = getButtonSuffix(keyCode);
        if (suffix == null) {
            return null;
        }
        return ATLAS_PREFIX + suffix;
    }

    public static String getShortLabel(int keyCode) {
        String suffix = getButtonSuffix(keyCode);
        if (suffix == null) {
            return Keys.toString(keyCode);
        }
        return suffix;
    }

    /** Textra markup for Adventure key-hint labels. */
    public static String getTextraMarkup(int keyCode, boolean pressed) {
        String region = getAtlasRegionName(keyCode);
        if (region == null) {
            return "";
        }
        return "[%120][+" + region + (pressed ? "_pressed]" : "]");
    }
}
