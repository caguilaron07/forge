package forge.toolbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.util.ControllerGlyphs;
import forge.util.Utils;

/**
 * On-screen controller affordances for classic (non-Adventure) UI.
 */
public final class PadHints {
    public static final float BAR_HEIGHT = Utils.scale(22);
    private static final float GLYPH_SIZE = Utils.scale(16);
    private static final float PADDING = Utils.scale(6);
    private static final float GAP = Utils.scale(10);
    private static final FSkinFont LABEL_FONT = FSkinFont.get(11);

    private static TextureAtlas keysAtlas;

    public record Hint(int keyCode, String label) {
    }

    public static final Hint[] MENU_DEFAULT = {
            new Hint(Keys.DPAD_UP, "Navigate"),
            new Hint(Keys.BUTTON_A, "Select"),
            new Hint(Keys.BUTTON_B, "Back"),
    };

    public static final Hint[] TAB_SCREEN = {
            new Hint(Keys.DPAD_UP, "Navigate"),
            new Hint(Keys.BUTTON_A, "Select"),
            new Hint(Keys.BUTTON_B, "Back"),
            new Hint(Keys.BUTTON_L1, "Prev tab"),
            new Hint(Keys.BUTTON_R1, "Next tab"),
    };

    public static final Hint[] MATCH_DEFAULT = {
            new Hint(Keys.DPAD_UP, "Navigate"),
            new Hint(Keys.BUTTON_A, "Select"),
            new Hint(Keys.BUTTON_B, "Cancel"),
            new Hint(Keys.BUTTON_Y, "Zoom"),
            new Hint(Keys.BUTTON_L1, "Prev player"),
            new Hint(Keys.BUTTON_R1, "Next player"),
            new Hint(Keys.BUTTON_L2, "OK"),
            new Hint(Keys.BUTTON_R2, "Cancel"),
    };

    private PadHints() {
    }

    public enum BarPosition {
        TOP, BOTTOM
    }

    public static void drawBar(Graphics g, float width, float height, Hint... hints) {
        drawBar(g, width, height, BarPosition.BOTTOM, hints);
    }

    public static void drawBar(Graphics g, float width, float height, BarPosition position, Hint... hints) {
        if (!Forge.hasGamepad() || !Forge.isLandscapeMode() || hints == null || hints.length == 0) {
            return;
        }

        float y = position == BarPosition.TOP ? 0 : height - BAR_HEIGHT;
        FSkinColor backColor = FSkinColor.get(Colors.CLR_THEME2).alphaColor(0.92f);
        FSkinColor textColor = FSkinColor.get(Colors.CLR_TEXT);
        g.fillRect(backColor, 0, y, width, BAR_HEIGHT);

        float x = PADDING;
        float centerY = y + BAR_HEIGHT / 2;
        for (Hint hint : hints) {
            drawGlyph(g, hint.keyCode, x, centerY - GLYPH_SIZE / 2, GLYPH_SIZE);
            x += GLYPH_SIZE + Utils.scale(2);
            float labelWidth = LABEL_FONT.getBounds(hint.label).width + Utils.scale(4);
            g.drawText(hint.label, LABEL_FONT, textColor, x, y, labelWidth, BAR_HEIGHT, false, Align.left, true);
            x += labelWidth + GAP;
            if (x > width - PADDING) {
                break;
            }
        }
    }

    private static void drawGlyph(Graphics g, int keyCode, float x, float y, float size) {
        TextureRegion region = getGlyphRegion(keyCode);
        if (region != null) {
            g.drawImage(region, x, y, size, size);
            return;
        }
        FSkinColor textColor = FSkinColor.get(Colors.CLR_TEXT);
        g.drawText(ControllerGlyphs.getShortLabel(keyCode), LABEL_FONT, textColor,
                x, y, size, size, false, Align.center, true);
    }

    private static TextureRegion getGlyphRegion(int keyCode) {
        String regionName = ControllerGlyphs.getAtlasRegionName(keyCode);
        if (regionName == null) {
            return null;
        }
        try {
            if (keysAtlas == null) {
                keysAtlas = new TextureAtlas(Gdx.files.internal("skin/keys.atlas"));
            }
            return keysAtlas.findRegion(regionName);
        } catch (Exception ignored) {
            return null;
        }
    }
}
