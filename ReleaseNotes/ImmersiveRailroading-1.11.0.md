# Immersive Railroading 1.11.0 Release Candidate

After almost three years of waiting, Immersive Railroading 1.11.0 is finally here.

This update doesn't have the raw feature count of 1.10.0 — the extra time went into work that sets us up for much bigger things down the road.

Massive thanks to everyone who contributed code, tested builds, and kept the faith. You all are what keeps this project alive.

## Features

### New Minecraft versions

IR is now compatible with Minecraft 1.17.1-1.21.1, 1.21.4-26.1 support is working in progress.

### Tracks

#### Transfer Table

Transfer table is now a standard feature along with turntable, with 2 new settings added in track blueprint GUI: 
- Transfer Table Entries: Specify the entry count of the transfer table.
- Distance Between Entries: Specify the distance between the centers of two entries in blocks.

#### New Track Definition Format

With the ability to declare track pieces' order brought by the Advanced Track Model Definition, modelers can now make their tracks' appearance more complex and realistic.

For instance, modelers can make the grassy track truly randomized, or make third-rail support spacing easier to manage.

The full definition can be found at https://gist.github.com/Goldenfield192/70bc96cce31cd1a784868fbe302073b5

#### `TABLE` Part

It's now possible to define unstretched parts on turntables and transfer tables with the `TABLE` pattern. The part declared as `TABLE` would be rendered identically on the moving part of tables, enabling detailed appearance without manual calculations.
- For example, a 30m `TABLE` would be rendered at its true 30‑meter length on a 40m turntable, unlike `RAIL_LEFT`/`RAIL_RIGHT`, which are stretched to fit.

### Rolling Stock Configuration

Examples and comments on all the possible configuration fields can be found in [the rolling stock default folder](https://github.com/TeamOpenIndustry/ImmersiveRailroading/tree/master/src/main/resources/assets/immersiverailroading/rolling_stock/default), and here's an overview of new configs:

- Added `power_w`, `power_kw`, `power_ps` (Metric horsepower), and `power_hp` (Imperial horsepower) as alternatives for locomotive power definitions.
  - Legacy `horsepower` is equivalent to `power_hp`, and is **no longer recommended and may not be supported by newer versions**.
- Added `tractive_effort_kn` and `tractive_effort_n` as alternatives for locomotive tractive effort definitions.
- Added `max_pressure_bar`, `max_pressure_kpa`, and `max_pressure_psi` as alternatives for steam locomotive pressure definitions. 
  - Legacy `max_psi` is equivalent to `max_pressure_psi` and is **no longer recommended and may not be supported by newer versions**.
- Added an optional field `revertDirection` to light definitions to specify if the light's direction is reversed.
  - For instance, a front-facing light at the stock's tail would require `revertDirection` to be true to work properly.
- Added support for defining light working directions in model using `FORWARD` or `REVERSE`.
  - For instance, a `FORWARD` light will only work when the stock is moving forward or stopped, and a `REVERSE` light will only work when the stock is moving backward.
- Added an optional field `tags` as stocks' base information, allowing more in‑depth searching.
  - Mainly works with new augment filter.
- Added an optional field `snow_layers` (integer, ranging from 0 to 8) under `properties` in stock definitions to define reserved snow layers on tracks after cleaning.
- Added an optional field `fuel_override` under `properties` for diesel locomotives to customize the fuels the locomotive can use.
  - Full description could be found at [PR#1577](https://github.com/TeamOpenIndustry/ImmersiveRailroading/pull/1577).
- Added `range_min` and `range_max` to the animatrix definition block to allow modelers create sliced animation.
  - This has a similar effect to `RANGE_[min]_[max]` in control definitions.
- (Experimental) Stock model parts can be marked as `ALPHA` to enable transparency.
  - This requires transparent texture and compatible shader to work.

### Interactive Augment

Augments now have their own dedicated GUI! Players can simply change the settings inside instead of memorizing the correct item to hold when setting.

The filter system has also been reworked, giving players a more programmatic way to define conditions.

Full description can be found at [PR#1578](https://github.com/TeamOpenIndustry/ImmersiveRailroading/pull/1578)

### Stock Label Translation

Stock interactive parts' `LABEL`s can be translated with the translation keys:

- For built-in controls' labels, like `THROTTLE`, use `part.immersiverailroading:control.[lowercase name]`.
  - In the example, it is `part.immersiverailroading:control.throttle`.
- For modeler-defined labels, use long format `label.immersiverailroading:[stock_type].[stock_definition_name].[label]` for fewer conflicts or short format `label.immersiverailroading:[label]` for better reusability.
  - For example, as for a diesel locomotive defined as `test_stock` with a control `WIDGET_1_LABEL_test^label`, IR will first search for `label.immersiverailroading:diesel.test_stock.test^label`, then `label.immersiverailroading:test^label` if the long one is not found, then fallback to the literal text.


### Config entries

- Added `stockDropInCreativeMode` in `Config/Debug` to facilitate world building.
- Added `excludeStandaloneWagons` in `Config/Debug` to prevent a single wagon from keeping the chunk loaded 
  - Works when `keepStockLoaded` is enabled.
- Added `TrainsIgnoreBlocks` in `Config/Damage` to prevent destruction of specific block types (use registry names shown in the F3 panel).
- Added `TrackRenderDistance` in `Config/Graphics` to customize the maximum track render distance for values above 256.
- Added `DisableLightTextureRender` in `Config/Graphics` to disable flare rendering 
  - Dynamic light is preserved if OptiFine or LambDynamicLights is installed.
- Added `powerUnit` and `forceUnit` in `Config/Graphics` to customize units used in GUI and item tooltips.
- Added `scrollMode` in `Config/Graphics` to control widgets' scroll behavior more in-depth 
  - Full description could be found at [PR#1626](https://github.com/TeamOpenIndustry/ImmersiveRailroading/pull/1626).
- Added `allowCargoLoadDroppedItem` in `Config/Immersion Level` to allow `CARGO_ITEM`s and `CARGO_FILL`s to absorb nearby dropped items

## Changes

### Golden Spike

In the vertical direction, golden spikes can now modify the height of a Slope track.

In the horizontal direction of Turn, Straight and Slope, it will now use projected length to update the track, aiming at more intuitive building.

### Command

- Removed `/immersiverailroading reload` since IR removed the in-game reloading.
- Added `/immersiverailroading cargoFill [radius: Integer]` to:
  - Fill all stocks in the specified radius with the item held in the player's main hand, or
  - Clears all stocks if the hand is empty.
  - The default radius is 2.

### Custom GUI Data Digits

With the Custom User Interfaces, modelers could refer stock data in text component with `state.[name]`, like `state.speed`. Modelers can now specify the digits for decimal they want by appending `.X` (integer, ranging from 0 to 5).

For example, `state.speed` gives a 2-digit decimal (default for speed), `state.speed.5` gives a 5-digit decimal, `state.speed.0` results in an integer.

## Bugfixes and Improvements
  - Rewrote track segments spanning logic to get rid of weird overlapping in the middle.
  - Updated `clearArea` exception logic to avoid manual cleaning when placing tracks in snow.
  - Adjusted grade crossing math to ensure symmetrical heights on both sides of the track.
  - Prevented augments push or pull item/fluid from other neighbor augments.
  - Made front/rear bogie clack sound not conflicting.
  - Corrected smoke particle emitter offsets, especially notable during high-speed movement.
  - Fixed that widgets on unfinished stocks can be dragged.
  - Fixed element overlapping in the track blueprint GUI.
  - Fixed incorrect Boiler Roller model offsets.
  - Fixed Boiler Roller's animation not playing.
  - Fixed diesel throttle glitches when setting non-notched values via dragging or augments.
  - Fixed stock being persisted even with packs unloaded.
  - Fixed red "obstacle" block being rendered at world origin instead of track's position.
  - Fixed bogies of non-standard track being offset weirdly on curves.
  - Expanded paintbrush GUI button width to fit new translations.
  - Fixed Z-fighting on newly placed Turntables and Transfer tables.
    - Existing ones will need to be re-placed.
  - Remapped non-existent materials in `firefly` and `iron_duke` to prevent unnecessary logs.
