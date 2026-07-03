# Controller Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the libGDX Forge client fully playable with a gamepad on Steam Deck (classic mode path), meeting the acceptance test in the design spec.

**Architecture:** Extend the existing `Forge` controller→`Keys` pipeline to classic mode; add a `FocusNavigator` abstraction for menu screens; close in-match UI gaps with thin mobile adapters. Adventure mode stays on its separate listener.

**Tech Stack:** Java, libGDX, gdx-controllers-desktop (Jamepad-SDL), Maven (`mvn -U -B -P windows-linux install`), launcher `forge-gui-mobile-dev`.

## Global Constraints

- Changes in `forge-gui-mobile` only (plus `forge-gui-mobile-dev` if launcher tweak needed).
- Steam Input ON at launch; generic Xbox-style glyphs; portrait pad out of scope.
- Preserve touch input and Adventure controller behavior at every step.
- Do not merge Adventure `SceneControllerListener` with classic `Forge` listener.
- Single feature branch; land reviewable increments in sequence.
- v1 menu path: Home → NewGameMenu → Constructed → Lobby → Deck editor → Match only.

**Design spec:** [../specs/2026-07-02-controller-support-design.md](../specs/2026-07-02-controller-support-design.md)

---

## Workstream 0: Classic controller plumbing

**Files:** `forge-gui-mobile/src/forge/Forge.java`

- [x] Rename private field `hasGamepad` → `controllerInputActive` with inline comment (connection/modality flag).
- [x] Update `hasGamepad()` to return `controllerInputActive && isLandscapeMode()` for **both** classic and adventure (remove adventure-only gate).
- [x] Call `enableControllerListener()` from `afterDbLoaded()` with Android guard matching adventure.
- [x] Make `enableControllerListener()` idempotent (`Controllers.addListener` once).
- [x] Sync `controllerInputActive` from `Controllers.getCurrent()` on first registration.
- [x] Fix stale `//adventure only` comments in `translateButtons` / `translateAxis`.

**Test:** Build and run `forge-gui-mobile-dev`. Connect pad in classic Home; verify d-pad / A / B log or reach `MatchScreen` handlers. Enter Adventure after classic startup — no double input.

---

## Workstream 1: Focus layer + FButton

**Files:** `forge/toolbox/focus/Focusable.java`, `FocusNavigator.java`, `FocusGroup.java`; `FButton.java`; `LaunchScreen.java`

- [x] Implement `Focusable`, `FocusNavigator`, `FocusGroup` per design spec §3.
- [x] Add `BUTTON_A` to `FButton.keyDown()`.
- [x] Wire `LaunchScreen` "Start Match" for pad confirm.
- [ ] Unit-test geometry neighbor selection where practical (deferred — no test harness in `forge-gui-mobile`).

**Test:** Focus ring visible on a test screen; button activates with A.

---

## Workstream 2: Trigger/analog routing

**Files:** `forge-gui-mobile/src/forge/Forge.java`

- [x] Enable L2/R2 → Enter/Escape and left-stick Y → PAGE_DOWN for non-`MatchScreen` containers (mirror match behavior).

**Test:** L2/R2 work on Home/Lobby once those screens have handlers.

---

## Workstream 3: Home + NewGameMenu

**Files:** `HomeScreen.java`, `NewGameMenu.java` (popup focus model)

- [x] `FocusGroup` on `HomeScreen` button scroller.
- [x] Popup focus for `NewGameMenu` (`FPopupMenu` / `FDropDownMenu` pattern).
- [x] Verify path opens `ConstructedScreen` (Home **A** on New Game → preferred screen; sidebar menu on Constructed).

**Test matrix rows:** HomeScreen, NewGameMenu.

---

## Workstream 4: Lobby + compound widgets

**Files:** `LobbyScreen.java`, `ConstructedScreen.java`, `PlayerPanel.java`, `FComboBox.java`, `FCheckBox.java`, `FScrollPane.java`, `FDeckChooser.java`

- [x] Focus + compound semantics per design §3.6.
- [x] `refreshFocusables()` on lobby network updates.
- [x] Deck chooser open/close by pad.

**Test matrix rows:** ConstructedScreen/Lobby, FDeckChooser.

---

## Workstream 5: Deck editor

**Files:** `deck/FDeckEditor.java`, `ClassicTextInput` helper (new)

- [x] Catalog ↔ deck pane focus (L1/R1 tab cycle; d-pad between header and card list).
- [x] A on card add/remove one copy; X opens context menu for quantity/options.
- [x] Save via L2/Enter; rename via header name tap/A (`FOptionPane` input dialog).
- [ ] `ClassicTextInput` OSK wrapper (deferred — dialog path works for rename).

**Test matrix rows:** FDeckEditor rows.

---

## Workstream 6: Settings

**Files:** `SettingsScreen.java`, `SettingsPage.java`, `TabPageScreen.java`

- [ ] Reuse WS4 widgets + tab focus.
- [ ] Text input via same `ClassicTextInput` helper.

**Test matrix row:** SettingsScreen.

---

## Workstream 7: FOptionPane

**Files:** `FOptionPane.java`

- [ ] D-pad between option buttons; A confirms focused button (not always default).

**Test matrix row:** FOptionPane dialog.

---

## Workstream 8: In-match gaps

**Files:** `VAvatar.java`, `MatchScreen.java`, relevant `views/*`

- [ ] Attack target d-pad between avatars.
- [ ] Block attacker switch, multi-target cycle, mana source selection.
- [ ] Fix menu-bar ENTER/ESCAPE guard for dialogs.

**Test matrix rows:** Match in-duel rows.

---

## Workstream 9: Glyphs + docs

**Files:** `KeyBinding.java`, focus ring polish, `docs/Steam-Deck-and-Bazzite-Install.md`

- [ ] On-screen prompts; generalize glyph prefix.
- [ ] Native desktop + Steam Input section in Deck install doc.

---

## Workstream 10: Opportunistic refactor

**Files:** `VField.java`, `ItemManager.java`

- [ ] Refactor toward `FocusNavigator` only if behavior matches; no regression.

---

## Manual test matrix

| Screen | D-pad | A | B | L1/R1 | L2/R2 | Focus ring |
|---|---|---|---|---|---|---|
| HomeScreen | | | | — | — | |
| NewGameMenu | | | | — | — | |
| Constructed / Lobby | | | | | — | |
| FDeckChooser | | | | | — | |
| FDeckEditor — list | | | | | — | |
| FDeckEditor — add/remove | | | — | | — | |
| FDeckEditor — save/rename | | | | | — | |
| SettingsScreen | | | | | — | |
| FOptionPane | | | | — | | |
| MatchScreen | | | | | | |
| — attack opponent | | | — | — | — | |
| — declare blockers | | | — | — | — | |
| — multi-target | | | — | — | — | |
| — pay mana | | | — | — | | |
| Adventure (regression) | | | | | | |

---

## Build & run

```bash
mvn -U -B -P windows-linux install -pl forge-gui-mobile-dev -am
# Run via forge-gui-mobile-dev launch script or Main.java
```
