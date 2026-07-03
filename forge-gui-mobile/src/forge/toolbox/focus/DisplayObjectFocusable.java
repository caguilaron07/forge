package forge.toolbox.focus;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Rectangle;

import forge.toolbox.FDisplayObject;

/**
 * Adapts an {@link FDisplayObject} to {@link Focusable} without changing the widget base class.
 */
public class DisplayObjectFocusable implements Focusable {
    private final FDisplayObject owner;

    public DisplayObjectFocusable(FDisplayObject owner0) {
        owner = owner0;
    }

    public static DisplayObjectFocusable forObject(FDisplayObject owner0) {
        if (owner0 instanceof Focusable focusable) {
            return new DelegatingFocusable(focusable, owner0);
        }
        return new DisplayObjectFocusable(owner0);
    }

    public FDisplayObject getOwner() {
        return owner;
    }

    @Override
    public Rectangle getFocusBounds() {
        return owner.screenPos;
    }

    @Override
    public boolean isFocusable() {
        return owner.isEnabled() && owner.isVisible();
    }

    @Override
    public void onFocusGained() {
        owner.setHovered(true);
    }

    @Override
    public void onFocusLost() {
        owner.setHovered(false);
    }

    @Override
    public boolean onFocusActivate() {
        return owner.keyDown(Keys.BUTTON_A) || owner.keyDown(Keys.ENTER);
    }

    private static final class DelegatingFocusable extends DisplayObjectFocusable {
        private final Focusable delegate;

        private DelegatingFocusable(Focusable delegate0, FDisplayObject owner0) {
            super(owner0);
            delegate = delegate0;
        }

        @Override
        public Rectangle getFocusBounds() {
            return delegate.getFocusBounds();
        }

        @Override
        public boolean isFocusable() {
            return delegate.isFocusable();
        }

        @Override
        public void onFocusGained() {
            delegate.onFocusGained();
        }

        @Override
        public void onFocusLost() {
            delegate.onFocusLost();
        }

        @Override
        public boolean onFocusActivate() {
            return delegate.onFocusActivate();
        }
    }
}
