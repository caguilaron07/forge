package forge.toolbox.focus;

import java.util.List;

import com.badlogic.gdx.math.Rectangle;

/**
 * Geometry-aware focus neighbor search for absolutely-positioned widgets.
 */
public final class FocusGeometry {
    private FocusGeometry() {
    }

    /**
     * @return index of the nearest focusable in {@code direction}, or {@code -1} to stay put
     */
    public static int findNearestNeighbor(List<Rectangle> bounds, int currentIndex, FocusDirection direction) {
        if (bounds == null || bounds.isEmpty() || currentIndex < 0 || currentIndex >= bounds.size()) {
            return -1;
        }

        Rectangle current = bounds.get(currentIndex);
        float cx = current.x + current.width / 2f;
        float cy = current.y + current.height / 2f;

        int bestIndex = -1;
        float bestPrimary = Float.MAX_VALUE;
        float bestDistance = Float.MAX_VALUE;

        for (int i = 0; i < bounds.size(); i++) {
            if (i == currentIndex) {
                continue;
            }
            Rectangle candidate = bounds.get(i);
            float nx = candidate.x + candidate.width / 2f;
            float ny = candidate.y + candidate.height / 2f;

            if (!isInDirection(cx, cy, nx, ny, direction)) {
                continue;
            }

            float primary = primaryAxisDistance(cx, cy, nx, ny, direction);
            float distance = (nx - cx) * (nx - cx) + (ny - cy) * (ny - cy);

            if (primary < bestPrimary || (primary == bestPrimary && distance < bestDistance)) {
                bestPrimary = primary;
                bestDistance = distance;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    static boolean isInDirection(float cx, float cy, float nx, float ny, FocusDirection direction) {
        switch (direction) {
            case UP:
                return ny < cy;
            case DOWN:
                return ny > cy;
            case LEFT:
                return nx < cx;
            case RIGHT:
                return nx > cx;
            default:
                return false;
        }
    }

    static float primaryAxisDistance(float cx, float cy, float nx, float ny, FocusDirection direction) {
        switch (direction) {
            case UP:
            case DOWN:
                return Math.abs(ny - cy);
            case LEFT:
            case RIGHT:
                return Math.abs(nx - cx);
            default:
                return Float.MAX_VALUE;
        }
    }
}
