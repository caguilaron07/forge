package forge.screens;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Rectangle;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinImage;
import forge.menu.FPopupMenu;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FOptionPane;
import forge.toolbox.focus.Focusable;
import forge.toolbox.focus.FocusNavigator;
import forge.util.Utils;

public abstract class LaunchScreen extends FScreen {
    private static final float MAX_START_BUTTON_HEIGHT = 1.75f * Utils.AVG_FINGER_HEIGHT;
    private float START_BUTTON_RATIO = 0.f;
    private static final float PADDING = FOptionPane.PADDING;

    protected final StartButton btnStart = add(new StartButton());
    private final FocusNavigator padFocus = new FocusNavigator();

    public LaunchScreen(String headerCaption) {
        super(headerCaption);
        padFocus.register(btnStart);
    }
    public LaunchScreen(String headerCaption, FPopupMenu menu) {
        super(headerCaption, menu);
        padFocus.register(btnStart);
    }

    @Override
    protected final void doLayout(float startY, float width, float height) {
        if (Forge.hdstart)
            START_BUTTON_RATIO = FSkinImage.HDBTN_START_UP.getWidth() / FSkinImage.HDBTN_START_UP.getHeight();
        else
            START_BUTTON_RATIO = FSkinImage.BTN_START_UP.getWidth() / FSkinImage.BTN_START_UP.getHeight();

        float buttonWidth = width - 2 * PADDING;
        float buttonHeight = buttonWidth / START_BUTTON_RATIO;
        if (buttonHeight > MAX_START_BUTTON_HEIGHT) {
            buttonHeight = MAX_START_BUTTON_HEIGHT;
            buttonWidth = buttonHeight * START_BUTTON_RATIO;
        }
        btnStart.setBounds((width - buttonWidth) / 2, height - buttonHeight - PADDING, buttonWidth, buttonHeight);

        doLayoutAboveBtnStart(startY, width, height - buttonHeight - 2 * PADDING);
    }

    protected abstract void doLayoutAboveBtnStart(float startY, float width, float height);
    protected abstract void startMatch();

    protected class StartButton extends FDisplayObject implements Focusable {
        private boolean pressed;

        /**
         * Instantiates a new FButton.
         */
        public StartButton() {
        }

        @Override
        public final boolean press(float x, float y) {
            pressed = true;
            return true;
        }

        @Override
        public final boolean release(float x, float y) {
            pressed = false;
            return true;
        }

        @Override
        public final boolean tap(float x, float y, int count) {
            if (count == 1) {
                btnStart.setEnabled(false);
                startMatch();
            }
            return true;
        }

        @Override
        public void draw(Graphics g) {
            if (Forge.hdstart)
                g.drawImage(pressed ? FSkinImage.HDBTN_START_DOWN :
                        isHovered() ? FSkinImage.HDBTN_START_OVER : FSkinImage.HDBTN_START_UP,
                        isHovered() ? -2 : 0, 0, getWidth(), getHeight());
            else
                g.drawImage(pressed ? FSkinImage.BTN_START_DOWN :
                        isHovered() ? FSkinImage.BTN_START_OVER : FSkinImage.BTN_START_UP, 0, 0, getWidth(), getHeight());
            //its must be enabled or you can't start any game modes
            if (!Forge.isLoadingaMatch()) {
                if(!btnStart.isEnabled())
                    btnStart.setEnabled(true);
            }
        }

        @Override
        public Rectangle getFocusBounds() {
            return screenPos;
        }

        @Override
        public boolean isFocusable() {
            return isEnabled() && isVisible();
        }

        @Override
        public void onFocusGained() {
            setHovered(true);
        }

        @Override
        public void onFocusLost() {
            setHovered(false);
        }

        @Override
        public boolean onFocusActivate() {
            tap(0, 0, 1);
            return true;
        }
    }

    @Override
    protected void drawOverlay(Graphics g) {
        if (Forge.hasGamepad()) {
            padFocus.drawFocusRing(g);
        }
    }

    @Override
    public boolean keyDown(int keyCode) {
        if (Forge.hasGamepad() && padFocus.handleKey(keyCode)) {
            return true;
        }
        switch (keyCode) {
        case Keys.ENTER:
        case Keys.SPACE:
        case Keys.BUTTON_A:
            startMatch(); //start match on Enter, Space, or gamepad A
            return true;
        }
        return super.keyDown(keyCode);
    }
}
