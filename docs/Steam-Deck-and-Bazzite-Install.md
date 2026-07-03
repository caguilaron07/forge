_This instruction was written using Bazzite, however should be similar enough for SteamOS._

For **controller-first play** on Steam Deck, use the **libGDX desktop client** (`forge-gui-mobile-dev`) section below. The Waydroid Android APK and legacy Swing desktop installer are alternative paths.

## Forge libGDX Desktop on Steam Deck (Recommended for controller play)

The **libGDX desktop client** (`forge-gui-mobile-dev`) runs natively on SteamOS / Bazzite with full gamepad support in landscape mode (menus, deck editor, lobby, and duels). This is the path used for controller-first play on the Deck OLED.

### Build the desktop JAR

On any Linux machine with Maven and JDK 17+:

```bash
git clone https://github.com/Card-Forge/forge.git
cd forge
mvn -U -B -P windows-linux install -pl forge-gui-mobile-dev -am
```

The runnable fat JAR is produced under `forge-gui-mobile-dev/target/` (name varies with version; look for `*-jar-with-dependencies.jar`).

Copy that JAR (and optionally `forge-gui-mobile-dev/src/main/config/forge-adventure.sh` as a launch template) to the Deck.

### Add Forge to Steam

1. In Steam Desktop Mode: **Add a Game** → **Add a Non-Steam Game**.
2. Choose a launch script or `java` directly. Example `forge-deck.sh` next to the JAR:

```bash
#!/bin/sh
cd "$(dirname "$0")"
exec java -Xmx4G -jar forge-gui-mobile-dev-*-jar-with-dependencies.jar
```

3. Make the script executable: `chmod +x forge-deck.sh`
4. Add `forge-deck.sh` as the non-Steam game entry.

### Steam Input (required for consistent pads)

Enable **Steam Input** for the Forge entry (controller icon in Steam → Manage → Controller Options).

- Use the **Gamepad** template (Xbox-style layout).
- Steam normalizes Stadia, Switch Pro, and other pads to the same virtual gamepad; Forge shows Xbox-style on-screen glyphs.
- **Desktop Mode:** play in landscape with the Deck in the kickstand or an external display if desired.
- **Game Mode:** launch from Steam; Forge opens in landscape when a controller is active.

### Verify controller detection

1. Launch Forge from Steam with a pad connected.
2. Open **Home** in landscape — menu items should show an orange focus ring when navigating with the d-pad.
3. Check logs if needed: controller connect/disconnect lines appear as `Controller` in the libGDX log; initial pad enumeration may also print `Gamepad: <name>` once at startup.

### Controls (classic mode, landscape + gamepad)

| Input | Action |
|---|---|
| D-pad | Move focus / field selection |
| A | Select / confirm |
| B | Back / cancel prompt |
| L1 / R1 | Previous / next player panel (match); prev / next tab (settings, deck editor) |
| L2 / R2 | OK / cancel (prompt bar); also mapped to Enter / Escape |
| Y | Zoom card |
| Left stick down | Confirm player target (avatar) |

Touch and mouse remain fully supported alongside the controller.

### Troubleshooting

- **No gamepad response:** confirm Steam Input is enabled and a layout is applied; try Desktop Mode first.
- **Portrait layout with pad:** rotate to landscape or relaunch from Steam Game Mode.
- **Java missing on host OS:** install OpenJDK 17+ (`rpm-ostree install java-21-openjdk` on Bazzite, or distro equivalent). The fat JAR still needs a JRE on the system.

---

## Installing Forge Android in Waydroid (Alternative Method)

* You will need Waydroid installed first; this reddit post may help: https://www.reddit.com/r/SteamDeck/comments/1ay7ev8/how_to_install_waydroid_android_on_your_steam_deck/

Once you've installed Waydroid, you can follow the same steps you would on any Android device.

1. Open browser to https://github.com/Card-Forge/forge

1. Open Snapshots from the Link in the ReadMe Page

1. Download the Snapshot APK

1. Open the APK from the download notification in your Android OS.

1. Give permissions to allow the browser to run APK installers.

1. Once installed, Forge should request image storage and audio permissions.

1. Forge sends you to the Settings Page to provide permissions.

1. Run Forge from the Application Drawer (likely Forge is still running)

1. You can tap anywhere else on the screen and Forge will restart.

1. Once Forge restarts it will ask to update the Assets.

1. Congratulations you can play Forge on your SteamDeck.

### Running Forge Android in Steam GameMode

My understanding is that Waydroid running detached Android apps doesn't work at the moment, so the best way to run Forge is to start Waydroid itself from Steam Gamemode and then start the Forge App. To do this you will need to add Waydroid to Steam as a non-Steam Game, controller support should be natively supported this way, you should also enable touch screen pass-through in the Steam Controller configuration if you didn't already during install of Waydroid.

1. Add Waydroid as Non-steam game.

1. Configure/Enable controller support with touchscreen.

1. Start Waydroid from Steam GameMode.

1. Let the Waydroid Android OS boot.

1. Start Forge

## Legacy Swing Desktop (forge-gui-desktop)

This section refers to the **classic Swing desktop client** (`forge-gui-desktop`), not the libGDX `forge-gui-mobile-dev` build above.

### Installing Swing Desktop Natively (Not Recommended)

Barring a flatpack (and packing Java with Forge), the correct way to install Forge Desktop natively (installer JAR or BZ2 archive) would be to install Java OpenJDK in the OS globally. **This is against Steam and Bazzite Dev recommendations**, however is doable; 

* For Bazzite: simply use `rpm-ostree install java-21-openjdk`

> You can then install forge as you would in any Linux OS, following the [user guide](user-guide).
> Again, this is against Bazzite devs recommendations, and they will likely belittle you and tell you you can't ask for support, if you tell them you did this like a normal Linux user would.

* For SteamOS: I believe you need to unlock the OS, install java from the package repo, then lock the OS again. 

> This Wiki will not provide instructions for this, if you feel you can do this you can probably look up guides to help you.

### Installing Forge in a Container/VM (Boxes or Other)

Another option is to install Forge in a Linux Container or VM, where you can download and install Java JDK into the container, also download and install Forge into the container then run Forge. This is not recommended as it can be confusing to a novice Linux user, adds system overhead, likely UI problems, and would be difficult to perform on a handheld device.

> This Wiki will not provide instructions for this, if you feel you can do this you can probably look up guides to help you.


