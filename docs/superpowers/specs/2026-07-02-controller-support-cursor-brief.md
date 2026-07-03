# Request for a Cursor AI Agent: Full Controller Support for Forge (libGDX client)

**Date:** 2026-07-02
**Repo:** `Card-Forge/forge` (this checkout: `/home/calin/Documents/github/forge`)
**Deliverable of THIS document:** a research-grounded work request. You (the Cursor agent) own the detailed design, spec, and implementation. The research below is done for you — build on it, verify it, and do not re-derive it from scratch.

---

## 1. Goal

Make the entire **libGDX client** of Forge playable with a game controller on a **Steam Deck OLED** — menus, deck building, match setup, and in-duel play — with **no touch or mouse required**. Target pads: a **Stadia controller (generic Bluetooth)** and a **Nintendo Switch Pro controller**.

Adventure mode is already controller-navigable and is the reference implementation to learn from — not the thing to fix.

## 2. Target & environment (read this first — it eliminates a whole class of wrong turns)

- **Module to change:** `forge-gui-mobile` (the shared libGDX UI). The desktop launcher is `forge-gui-mobile-dev` (LWJGL3 + `gdx-controllers-desktop` / Jamepad-SDL backend); its entry point is `forge-gui-mobile-dev/src/forge/app/Main.java`. This is what runs on the Steam Deck.
- **NOT `forge-gui-desktop`.** That is the Swing client. It has no controller infrastructure and is explicitly out of scope.
- **Launch model on the Deck: Steam Input ON.** Forge is added as a non-Steam game and launched through Steam. Steam Input normalizes *both* pads into a single virtual gamepad (Xbox-style layout). Consequences you can rely on:
  - The app sees **one consistent controller mapping**. You do **not** need per-pad `gamecontrollerdb.txt` entries, and you do **not** need to special-case the Switch Pro A/B/X/Y swap or the Stadia-over-Bluetooth SDL axis quirks — Steam handles those before the app sees input.
  - On-screen button glyphs can therefore be **standard/generic** (Xbox-style A/B/X/Y), not per-pad. Steam overlays its own glyphs anyway.
  - Design defensively: if Steam Input is off and a raw pad is seen via SDL, the app should still function (libGDX `ControllerMapping` gives logical buttons), just without the guarantees above.

## 3. How input works today (verified in-code)

Controller input is translated to **libGDX key codes** and dispatched through the existing keyboard path. There is **no central focus manager**; each widget self-manages a `selectedChild`/`selectedIndex`, and `keyDown()` moves that selection.

> **⚠️ Prerequisite (Workstream 0): the controller pipeline is OFF outside Adventure mode.** This was verified after the first draft and corrects this brief's original framing. `enableControllerListener()` is only reached from the adventure-open path (`Forge.java:377`, right after `isMobileAdventureMode = true`), and `hasGamepad()` returns `false` unless `isMobileAdventureMode` (`Forge.java:282–288`). Since `translateButtons()`/`translateAxis()` early-return on `!hasGamepad()`, **no pad events reach classic mode at all** — Home, Lobby, Settings, *and even classic MatchScreen*. Nothing screen-level is testable on the Deck acceptance path until this is fixed. So the **first** unit of work is: register the listener on classic/desktop startup (e.g. `Forge.create()` / `afterDbLoaded()`) and extend `hasGamepad()` to return true in classic mode when a pad is connected (keep the portrait gate if desired). Do NOT touch Adventure's separate listener path while doing this.

**Translation & routing — `forge-gui-mobile/src/forge/Forge.java`:**
- `enableControllerListener()` — lines ~1507–1678. Sets up a `ControllerAdapter`.
- `translateButtons()` — lines ~1579–1672. Maps controller buttons → `Keys.BUTTON_A/B/X/Y`, `Keys.DPAD_*`, `Keys.BUTTON_L1/R1`, etc.
- `translateAxis()` — lines ~1552–1577. L2/R2 triggers (axis 4/5) → `Keys.ENTER`/`Keys.ESCAPE`; left-stick Y → `Keys.PAGE_DOWN`.
- Dispatch target: top overlay (`FOverlay.getTopOverlay()`) else `currentScreen`, via `container.keyDown(int)`.
- **Routing asymmetry (smaller than the first draft claimed):** when `currentScreen instanceof MatchScreen`, the full button set is forwarded (lines ~1590–1629). The `else` branch for other screens (lines ~1630–1671) **does** forward d-pad, Back, and face buttons A/B/X/Y + L1 — face buttons are NOT dropped. What is MatchScreen-only is **L2/R2 triggers → Enter/Escape** and **left-stick Y → PAGE_DOWN** (commented out for other screens). So the routing cleanup is mainly triggers/analog, not face buttons.
- Flags: `hasGamepad` / `lastInputWasController` (declared ~line 74–75). `hasGamepad()` (~line 282–288) returns `false` entirely unless `isMobileAdventureMode`; only within adventure does it apply the `&& isLandscapeMode()` gate. See the Workstream 0 callout above — this is the real blocker, not the landscape gate.

**Base UI classes:**
- `forge/toolbox/FDisplayObject.java` — `keyDown()/keyUp()` default to `false` (no-op).
- `forge/toolbox/FContainer.java` — `keyDown()` (~lines 151–164) **broadcasts** to all enabled children until one returns `true`. No focus tracking.
- `forge/screens/FScreen.java` — `keyDown()` (~lines 402–416) handles ESCAPE/BACK (`Forge.back()`) then delegates to children.
- `forge/Forge.java` `MainInputProcessor` (~lines 1195–1270) — top overlay first, else current screen; no focus tracking at this level.

**Current coverage (verified):**

| Area | State | Key files (with line hints) |
|---|---|---|
| Adventure mode | ✅ Full (reference) | `forge/adventure/util/KeyBinding.java`, `forge/adventure/scene/Scene.java` |
| In-match `MatchScreen` | ✅ code exists, ❌ inert in classic | `keyDown` logic is present (`screens/match/MatchScreen.java` ~523–741) but never runs outside Adventure because `hasGamepad()` is `false` there. Becomes live once Workstream 0 lands. |
| Field/zone card selection | ✅ Full | `screens/match/views/VField.java` ~263–330 (`selectedChild`, `setNextSelected`); `views/VDisplayArea.java` ~11–66 |
| Item/card lists | ✅ Full | `itemmanager/ItemManager.java` ~1231–1286 (`selectedIndex`, d-pad, context menus) |
| Dropdown menus | ✅ Full (via tab) | `menu/FDropDown.java` ~33/260–271, `menu/FMenuTab.java` |
| Deck editor | 🟡 Partial | `deck/FDeckEditor.java` ~1025–1069 (R1 tab cycle; embedded list nav works). **Full build-by-pad is required** (add/remove/save/rename) — see Workstream 5. |
| Option dialogs | 🟡 Partial | `toolbox/FOptionPane.java` ~329–352 **already handles `BUTTON_A` (→ default option) and `BUTTON_B` (→ cancel)** as well as Enter/Escape. The real gap is **no d-pad focus movement *between* the option buttons** — A always fires the default. |
| Home / main menu | ❌ None | `screens/home/HomeScreen.java`, `screens/home/NewGameMenu.java` |
| Match setup / lobby | ❌ None | `screens/constructed/LobbyScreen.java`, `constructed/PlayerPanel.java` |
| Settings | ❌ None | `screens/settings/SettingsScreen.java`, `settings/SettingsPage.java` |
| Base button `FButton` | ❌ No `BUTTON_A` | `toolbox/FButton.java:355–361` triggers on ENTER/SPACE only. Gamepad press-animation hook already exists in `trigger()`; the key binding does not. |
| Compound widgets | ❌ None | `FComboBox`, `FCheckBox`, `FScrollPane`, `TabPageScreen`, `SettingsPage` have **no** `keyDown`. Lobby/Settings need *new* semantics (open dropdown on A, cycle values on d-pad, toggle on A), not just focus. |

**In-match interaction gaps (verified touch-only):**
- Choosing **which opponent to attack** (player/avatar targeting). `screens/match/views/VAvatar.java` ~102–105; `forge-gui/src/main/java/forge/gamemodes/match/input/InputAttack.java` (`onPlayerSelected` exists but has no pad path to reach it). Note: `VAvatar.keyDown(PAGE_DOWN)` selects the *currently focused panel's* player — it does not navigate *between* opponents. Attack targeting needs d-pad to move focus across avatars, then A to confirm.
- **Switching the current attacker** during block declaration. `.../input/InputBlock.java`.
- **Cycling candidates** for multi-target spells. `.../input/InputSelectTargets.java` ~165.
- **Tapping mana sources** to pay costs (`InputPayMana` expects tapping individual cards; mobile needs a selectable mana-source list or a field-nav extension during that phase). Confirm-payment via OK already works.

Everything else in-match already works by pad (once Workstream 0 makes the pipeline live): d-pad card navigation, `BUTTON_A` = select/tap, `BUTTON_Y` = zoom, `BUTTON_L1` = switch player panel, ENTER/L2 = OK, ESCAPE/R2 = Cancel, plus Ctrl-shortcut equivalents (end turn, alpha strike, undo, auto-yield). **Caveat to verify:** `MatchScreen.keyDown` (~line 525) deliberately drops ENTER/ESCAPE when a gamepad is active and the menu bar is showing — confirm this doesn't block dialog OK/Cancel during pad play.

## 4. Approach (decided with the requester)

**Build one shared focus/navigation abstraction, then adopt it across the touch-only screens.** Do not keep re-implementing per-screen `selectedChild` logic.

The abstraction should encapsulate:
- an ordered set of focusable children (or a way to derive it from layout),
- a current selection with **geometry-aware d-pad movement** (up/down/left/right resolves to the nearest focusable in that direction). This is the **hardest design bet** given Forge's absolute positioning, scroll panes, and overlapping panels — the design pass must specify: (a) **no-neighbor fallback** (wrap to opposite edge vs stay put — recommend stay put on first pass), (b) **scroll-pane clipping** (a focusable scrolled out of view must still be reachable and trigger `scrollIntoView`), and (c) **dynamic layouts** (the lobby player list changes at runtime — re-derive focusables on layout change; see WS4).
- `confirm` (A) and `cancel`/`back` (B/Escape) semantics,
- a **consistent visual focus outline** drawn for the selected element.

**Popup focus scope (decide up front):** Forge has many popups beyond `NewGameMenu` — context menus, deck-chooser overlays, `FOptionPane`. Decide whether the focus layer provides a **generic popup/overlay focus model** (recommended — `FOverlay.getTopOverlay()` already gives a natural focus-scope boundary) or handles named popups one at a time. A generic model avoids re-solving focus-trapping per popup.

Then refactor the existing ad-hoc implementations (`VField`, `VDisplayArea`, `FDropDown`, `ItemManager`) toward it **opportunistically and without regression** — matching behavior first, sharing code second. Match/Adventure/ItemManager behavior must not regress.

**Two parallel controller systems — keep them separate.** Adventure uses `Scene.SceneControllerListener` routing to scene methods; classic uses `Forge.enableControllerListener()` → key-code translation. Do **not** merge them. Share only the `KeyBinding`/glyph *utilities*; leave Adventure's working path untouched.

**Integration notes for the focus manager:**
- Integrate with `FContainer` without breaking its broadcast model — likely one designated "focus owner" child gets first crack at keys before the broadcast loop.
- Call `scrollIntoView()` on focus change (already done in `VDisplayArea`; Home's `ButtonScroller`, lobby's `playersScroll`, and settings' `FGroupList` all need this).
- Reuse the `setHovered(true)` convention for visual state, plus a distinct focus ring.
- Lean toward a `Focusable` interface + optional mixin rather than changing the `FDisplayObject` base, to minimize blast radius.

## 5. Workstreams & recommended sequencing

Each numbered item is a reviewable increment. **0 is a hard prerequisite** — nothing screen-level is testable on the Deck until it lands.

0. **Enable classic-mode controller plumbing (prerequisite).** Register `enableControllerListener()` on classic/desktop startup — prefer **`afterDbLoaded()` (`Forge.java:394`)** (or the first classic screen transition after it) over `create()` so the UI exists before pads are registered, matching Adventure's ordering. Mirror Adventure's Android guard: `if (!GuiBase.isAndroid() || !getDeviceAdapter().getGamepads().isEmpty())`. Extend `hasGamepad()` (`Forge.java:282–288`) to return true in classic mode when a pad is connected. **Decision: keep the portrait gate** — `hasGamepad()` stays `false` in portrait even with a pad connected (portrait gamepad support is a non-goal; the Deck runs landscape). Verify pad events now reach classic screens and that inert MatchScreen pad code comes alive. **Watch two things:** (a) **naming** — there is a `static boolean hasGamepad` *field* (`Forge.java:74`, connection state) *and* a `hasGamepad()` *method* (feature gate); do not conflate them — consider renaming for clarity (e.g. `isPadConnected` vs `isControllerInputActive`) or documenting the split inline. (b) **dual-listener regression** — after this lands, entering Adventure means both `Forge`'s `ControllerAdapter` and `Scene.SceneControllerListener` are registered. Add an explicit smoke test: enter Adventure after classic startup and confirm no double-input / duplicate key dispatch. Do not disturb Adventure's path.
1. **Shared focus layer + `FButton` A.** Implement the focusable-container abstraction + visual focus ring in `forge/toolbox` (see integration notes in §4). Add `BUTTON_A` handling to `FButton.keyDown()` (or map A→trigger in the focus layer) — the gamepad press-animation hook in `trigger()` already exists. Also give `LaunchScreen` "Start Match" a `BUTTON_A` path.
2. **Trigger/analog routing cleanup** in `Forge.java`: enable the currently-MatchScreen-only mappings (L2/R2 → Enter/Escape, left-stick Y → navigation) for other screens as appropriate. (Face buttons already route — this is the *small* part.) While here, fix the now-stale `//adventure only` comments at `Forge.java:1554` and `:1581` — after WS0 those early-returns gate on `hasGamepad()`, which is no longer adventure-only.
3. **`HomeScreen` + `NewGameMenu`.** Note `NewGameMenu` is an `FPopupMenu`, not an `FScreen` — it needs a popup-menu focus model (d-pad between items, A to select), not screen focus. The concrete acceptance path is **Home → `NewGameMenu` → `ConstructedScreen`** (`screens/constructed/ConstructedScreen.java`, which `extends LobbyScreen extends LaunchScreen`) — so the "Constructed" menu entry must be reachable and must open the lobby by pad. Name each intermediate screen in the test matrix.
4. **`LobbyScreen` (incl. `ConstructedScreen`) + `PlayerPanel` + compound widgets.** `ConstructedScreen` *is* a `LobbyScreen`, so this workstream covers the acceptance lobby. Needs **new interaction semantics** for `FComboBox` (open on A, cycle on d-pad), `FCheckBox` (toggle on A), and `FScrollPane` focus/scroll — not just wiring existing handlers. Also covers the **deck-chooser flow** (`FDeckChooser`) reached from here, which the acceptance test requires — it likely delegates list nav to `ItemManager` (partial ✅) but its open/close chrome may not be navigable. `LobbyScreen implements ILobbyView`: network-driven state changes may need to refresh/restore focus — verify focus survives a lobby update.
5. **Deck editor — full build-by-pad (`FDeckEditor`).** *In scope per the requester: the acceptance test requires building/editing a deck entirely by controller, not just picking one.* Today only the tab cycle (R1) and embedded `ItemManager` list nav work (`deck/FDeckEditor.java` ~1025–1069, 🟡 partial). Needs by-pad: **add/remove cards** (A on a card in the catalog moves it to the deck and vice-versa), **move focus between the catalog pane and the deck pane** (e.g. L1/R1 or d-pad across the split), **quantity adjust**, **save**, and **rename/search text input** (reuse Adventure's `TextInput` on-screen path — decide alongside Settings text input in #6). This is the largest single screen; budget it accordingly. Reuses the shared focus layer (#1) and compound-widget semantics (#4).
6. **`SettingsScreen` + `SettingsPage` + `TabPageScreen`.** Reuses the compound-widget work from #4. Decide text-input handling (settings search, deck rename): skip for v1 or reuse Adventure's `TextInput` pattern — coordinate this decision with the deck-editor text input in #5.
7. **Dialog coverage:** `FOptionPane` already fires A→default and B→cancel; the only gap is **d-pad focus movement *between* the option buttons** so A confirms the *focused* button rather than always the default.
8. **In-match gap closure:** opponent selection for attacks (d-pad *between* avatars, then A — see §3 note on `VAvatar`), attacker switching during block, multi-target candidate cycling, and mana-source selection/tapping — all reachable via d-pad + A. Touch equivalents live in the `forge-gui` `input/*` classes; the mobile-side selection UI is in `screens/match/*`. Thin mobile-side adapters calling existing `MatchController`/`selectPlayer` APIs are the right pattern (as `VAvatar` already does). **Also fix here (tracked, not a footnote):** `MatchScreen.keyDown` (~line 525) drops ENTER/ESCAPE when a gamepad is active and the menu bar is showing. Repro: open the in-match menu bar by pad, trigger a dialog (e.g. concede confirm), confirm OK/Cancel is not swallowed. Adjust the guard so it only suppresses the menu-toggle case, not genuine dialog input.
9. **Glyphs & feedback:** standard on-screen button prompts beyond Adventure, a visible focus outline everywhere the layer is used, controller affordances (e.g. "Ⓐ Select"). The existing glyph code hardcodes an `"XBox_"` prefix in `KeyBinding.getLabelText()` — generalize as needed. Standard glyphs are fine (Steam Input normalizes pads). Plus the Steam Deck launch doc: add Forge as a non-Steam game, pick the recommended Steam Input layout (Gamepad template), verify pad detection. **Log-line note:** `Forge.java:1677` (`System.out.println("Gamepad: <name>")`) is a *one-time* print at listener setup, not the connect handler; live connect/disconnect events log via `Gdx.app.log("Controller", …)` (`Forge.java:1512`/`1520`). **Docs alignment:** `docs/Steam-Deck-and-Bazzite-Install.md` recommends the **Waydroid Android APK** path and calls native desktop "Not Recommended." The requester's actual Deck setup is confirmed **native desktop** (`forge-gui-mobile-dev` fat JAR via a launch script, no Waydroid installed), which is what this brief targets. Add a controller-support section to that doc for the native-desktop path so it doesn't contradict this work.
10. **Opportunistic refactor** of `VField`/`ItemManager` toward the shared layer — last, since they're battle-tested; don't block menu work on it.

**Regression testing:** no automated UI tests exist for the mobile client. Maintain a **manual test matrix** per screen, and re-run the Adventure world + battle smoke test after every increment (Workstream 0 and shared utilities are the main regression risk).

**Starter manual test matrix** (extend as screens land; ✓ = works by pad, — = n/a, ? = to verify):

| Screen | D-pad nav | A (confirm) | B (back) | L1/R1 (tab) | L2/R2 (OK/Cancel) | Focus ring visible |
|---|---|---|---|---|---|---|
| HomeScreen | | | | — | — | |
| NewGameMenu (popup) | | | | — | — | |
| ConstructedScreen / Lobby | | | | | — | |
| FDeckChooser | | | | | — | |
| FDeckEditor — list nav | | | | | — | |
| FDeckEditor — add/remove card | | | — | | — | |
| FDeckEditor — save/rename | | | | | — | |
| SettingsScreen | | | | | — | |
| FOptionPane dialog | | | | — | | |
| MatchScreen (in-duel) | | | | | | |
| — attack opponent select | | | — | — | — | |
| — declare blockers | | | — | — | — | |
| — multi-target spell | | | — | — | — | |
| — pay mana | | | — | — | | |
| Adventure (regression) | | | | | | |

## 6. Acceptance test (must pass on the Steam Deck, with both pads)

Boot → navigate the main menu → **build a deck in the deck editor** (open the editor, add and remove cards, then save it — not merely pick a preconstructed one) → select that deck, configure and start a duel → play a full turn: **attack a chosen opponent**, **declare blockers**, **cast a multi-target spell** (selecting targets), **pay mana** — then **concede/exit** — **entirely by controller**, with no touch/mouse. Verify **no regression** to touch input or Adventure-mode controller nav.

## 7. Non-goals

- The Swing client (`forge-gui-desktop`).
- Per-pad SDL mapping files / `gamecontrollerdb.txt` and Switch A-B swap handling (Steam Input covers these).
- Portrait-mode gamepad support.
- Rewriting the input pipeline or the game-side `PlayerControllerHuman`/`input/*` contracts.
- Online/multiplayer-specific flows beyond what the acceptance test touches.

## 8. Constraints & conventions for the implementing agent

- Keep changes within `forge-gui-mobile` (and `forge-gui-mobile-dev` only if the launcher needs a tweak). The game-side `forge-gui` `input/*` classes define the interaction contract — read them, but prefer to satisfy them from the mobile UI rather than change them.
- Follow existing patterns (`FDisplayObject`/`FContainer`/`FScreen`, `selectedChild`/`setNextSelected`). Match surrounding code style, naming, and comment density.
- **Do a design/spec pass before coding**, and where practical develop test-first. Build with the project's Maven profile (e.g. `mvn -U -B -P windows-linux install`; run the client via `forge-gui-mobile-dev`).
- Preserve touch input and Adventure-mode behavior at every step. Land the work in reviewable increments (shared layer → routing → one screen at a time → in-match gaps → glyphs).

## 9. Quick-start pointers (so you don't hunt)

- Controller entry point: `forge-gui-mobile/src/forge/Forge.java` → `enableControllerListener`, `translateButtons`, `translateAxis`.
- Focus/keyboard base: `forge/toolbox/FDisplayObject.java`, `FContainer.java`, `screens/FScreen.java`.
- Working reference nav: `screens/match/views/VField.java`, `itemmanager/ItemManager.java`, `menu/FDropDown.java` + `menu/FMenuTab.java`.
- Adventure reference: `forge/adventure/util/KeyBinding.java`.
- Touch-only targets: `screens/home/HomeScreen.java`, `screens/constructed/LobbyScreen.java`, `screens/settings/SettingsScreen.java`.
- In-match interaction contracts: `forge-gui/src/main/java/forge/gamemodes/match/input/{InputAttack,InputBlock,InputSelectTargets,InputPayMana}.java`.
