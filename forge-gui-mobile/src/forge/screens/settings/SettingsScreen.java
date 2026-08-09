package forge.screens.settings;

import java.util.List;
import java.util.stream.Collectors;

import com.badlogic.gdx.Input.Keys;

import forge.Forge;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.screens.FScreen;
import forge.screens.TabPageScreen;
import forge.screens.TabPageScreen.TabPage;
import forge.screens.home.HomeScreen;
import forge.util.Utils;

public class SettingsScreen extends TabPageScreen<SettingsScreen> {
    public static final FSkinFont DESC_FONT = FSkinFont.get(11);
    public static final FSkinColor DESC_COLOR = FSkinColor.get(Colors.CLR_TEXT).alphaColor(0.5f);
    public static final float SETTING_HEIGHT = Utils.AVG_FINGER_HEIGHT + Utils.scale(12);
    public static final float SETTING_PADDING = Utils.scale(5);
    private static final float INSETS_FACTOR = 0.025f;
    private static final float MAX_INSETS = SETTING_HEIGHT * 0.15f;

    private static boolean fromHomeScreen;
    private static SettingsScreen settingsScreen; //keep settings screen around so scroll positions maintained
    private final SettingsPage settingsPage;

    public static void show(boolean fromHomeScreen0) {
        if (settingsScreen == null) {
            settingsScreen = new SettingsScreen();
        }
        fromHomeScreen = fromHomeScreen0;
        Forge.openScreen(settingsScreen);
    }

    public static boolean launchedFromHomeScreen() {
        return fromHomeScreen;
    }

    public static float getInsets(float itemWidth) {
        float insets = itemWidth * INSETS_FACTOR;
        if (insets > MAX_INSETS) {
            insets = MAX_INSETS;
        }
        return insets;
    }

    public SettingsPage getSettingsPage() {
        return settingsPage;
    }

    public static SettingsScreen getSettingsScreen() {
        return settingsScreen;
    }

    @SuppressWarnings("unchecked")
    private SettingsScreen() {
        super(new TabHeader<SettingsScreen>(new TabPage[] {
                new SettingsPage(),
                new FilesPage()
        }, true) {
            @Override
            protected boolean showBackButtonInLandscapeMode() {
                return !fromHomeScreen; //don't show back button if launched from home screen
            }
        });
        settingsPage = (SettingsPage) tabPages.get(0);
    }

    public FScreen getLandscapeBackdropScreen() {
        if (fromHomeScreen) {
            return HomeScreen.instance;
        }
        return null;
    }

    @Override
    public void showMenu() {
        Forge.back(); //hide settings screen when menu button pressed
    }

    @Override
    public boolean keyDown(int keyCode) {
        if (Forge.hasGamepad()) {
            switch (keyCode) {
                case Keys.BUTTON_L1:
                    controllerCycleTabs(-1);
                    return true;
                case Keys.BUTTON_R1:
                    controllerCycleTabs(1);
                    return true;
            }
            TabPage<SettingsScreen> page = getSelectedPage();
            if (page != null && page.keyDown(keyCode)) {
                return true;
            }
        }
        return super.keyDown(keyCode);
    }

    private void controllerCycleTabs(int amount) {
        List<TabPage<SettingsScreen>> visiblePages = tabPages.stream()
                .filter(TabPage::isTabVisible)
                .collect(Collectors.toList());
        if (visiblePages.isEmpty()) {
            return;
        }
        int current = visiblePages.indexOf(getSelectedPage());
        setSelectedPage(visiblePages.get(Math.floorMod(current + amount, visiblePages.size())));
    }
}
