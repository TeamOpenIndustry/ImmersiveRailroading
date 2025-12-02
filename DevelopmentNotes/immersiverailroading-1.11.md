## New Features

### **Configuration**
- **Goldenfield192**: 
  - Added `stockDropInCreativeMode` in `Config/Debug` to facilitate world building.
  - Added `excludeStandaloneWagons` in `Config/Debug` to reduce chunk loading costs (works with `keepStockLoaded`).
  - Added `TrainsIgnoreBlocks` in `Config/Damage` to prevent destruction to specific block types (use registry names shown in the F3 panel).
  - Added `TrackRenderDistance` in `Config/Graphics` to customize the maximum track render distance for values above 256.
  - Added `DisableLightTextureRender` in `Config/Graphics` to disable flare rendering (Dynamic light is preserved if OptiFine is installed).
  - Added `powerUnit` and `forceUnit` in `Config/Graphics` to customize units used in GUI and item tooltips.

### **Track**
- **cam72cam**: Golden spikes can now modify the height of `slope` tracks instead of being fixed at 1 block upwards.
- **Goldenfield192**: 
  - Updated golden spike behavior for horizontal direction—now uses arc length for `turn` and shadow length for `straight` and `slope`.
  - Introduced the transfer table as a new track type.
  - Rewrote curve generation to prevent overlapping track segments.
  - Added a new method for defining tracks (documented in [GitHub Gist](https://gist.github.com/Goldenfield192/70bc96cce31cd1a784868fbe302073b5)).

### **Stock**
- **poizzy & cam72cam**: Stock parts can now be made transparent by adding `ALPHA`.
- **FnordyCave**: Added `snow_layers` (integer property) under `properties` in stock definitions to define reserved snow layers on tracks after cleaning.
- **SebastianD334**:
    - Added `power_w`, `power_kw`, `power_ps` (Metric horsepower), and `power_hp` (Imperial horsepower) as alternatives for locomotive power definitions (legacy `horsepower` is equivalent to `power_hp` and is **no longer recommended and may not be supported by newer versions**).
    - Added `tractive_effort_kn` and `tractive_effort_n` as alternatives for locomotive tractive effort definitions.
    - Added `max_pressure_bar`, `max_pressure_kpa`, and `max_pressure_psi` as alternatives for steam locomotive pressure definitions (legacy `max_psi` is equivalent to `max_pressure_psi` and is **no longer recommended and may not be supported by newer versions**).
- **Goldenfield192**:
    - Added `range_min` and `range_max` to the animatrix definition block to align with `RANGE_[min]_[max]` in control definitions.
    - Added `revertDirection` to light definitions to specify if the light's direction is reversed (e.g., a front-facing light at the stock's tail).
    - Added support for defining light working directions using `FORWARD` or `REVERSE`.
    - Added `fuel_override` under `properties` to customize diesel locomotive fuels (documented in [GitHub PR #1577](https://github.com/TeamOpenIndustry/ImmersiveRailroading/pull/1577)).

### **Translations**
- **Goldenfield192**:
  - Stock labels can now be translated using the key `part.immersiverailroading:control.[name]` for [defaults](https://github.com/TeamOpenIndustry/ImmersiveRailroading/blob/master/src/main/java/cam72cam/immersiverailroading/library/ModelComponentType.java#L141) and `label.immersiverailroading:[stock_type].[stock_definition_name].[label]` or `label.immersiverailroading:[label]` for other `LABEL`s.
    - Example: For a diesel locomotive defined as `test` with a control `WIDGET_1_LABEL_test`, IR will first search for `label.immersiverailroading:diesel.test.test`, then `label.immersiverailroading:test` if the first one not found.
    - The first method is recommended for better compatibility.
  - Added numerous new translation entries for GUI elements.

### **Commands**
- **cam72cam**: Removed `/immersiverailroading reload` and added `/immersiverailroading cargoFill [radius: Integer]` to fill all stocks in the specified radius with the item held in the player's main hand (clears all stocks if the hand is empty). 
  - Default radius is 2.

### **GUI**
- **SebastianD334 & Goldenfield192**: Added support for defining decimal digits in GUI definitions using `state.[name].X`.
    - Example: `stat.speed.3` will display speed with 3 decimal digits.

## Fixes and Refactoring

- **DarkRaider**:
    - Updated `clearArea` exception logic to avoid manual cleaning when placing tracks in snow.
    - Adjusted grade crossing math to ensure symmetrical heights on both sides of the track.
- **Goldenfield192**:
    - Fixed draggable widgets on unfinished stocks.
    - Corrected particle emitter offsets, especially during high-speed movement.
    - Resolved element overlapping in the track blueprint GUI.
    - Fixed incorrect boiler machine model offsets.
    - Addressed diesel throttle glitches when setting non-notched values via dragging or augmentation.
    - Remapped non-existent materials in `firefly` and `iron_duke` to prevent unnecessary logs.
    - Ensured stocks are removed when their pack is unloaded.
    - Expanded paintbrush GUI button width to accommodate new translations.