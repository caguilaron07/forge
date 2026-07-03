package forge.toolbox.focus;

import com.badlogic.gdx.math.Rectangle;

/**
 * A UI element that can receive controller/keyboard focus within a {@link FocusNavigator}.
 */
public interface Focusable {
    Rectangle getFocusBounds();

    boolean isFocusable();

    void onFocusGained();

    void onFocusLost();

    /** A / Enter confirms this element. Return true if the activation was consumed. */
    boolean onFocusActivate();
}
