# Features
- Add `FLOOR` part to define walkable spaces (@poizzy)
  - This is a replacement of legacy `passenger` block in json, and if `FLOOR` is defined, json would be ignored.
- Add track snapping to end points (@Deepseasaltyfish)
- Add Remote Controller for locomotives that are radio-compatible (@Jeronimo97)
  - In-depth description could be found [here](https://github.com/TeamOpenIndustry/ImmersiveRailroading/pull/1687)
- Add extra settings to tweak tracks (@Deepseasaltyfish). For example, 
  - Adjusting roll and offset curve in a new GUI and control offset of end points
  - Make cut appearance on rail bed blocks
  - Manual could be found [here](https://gist.github.com/Deepseasaltyfish/b40993303cb7a948b0046fbf427a085c)
- Add new curve-based sound definition format (@poizzy)
  - Manual could be found [here](https://gist.github.com/poizzy/d645a9767ae9e75855615a62e51a532c)
- Add `Transition` (or to be more specific, Cubic Parabola) as a new track type, which behaves like pre-defined custom curves (@Deepseasaltyfish)
  - Manual could be found [here](https://gist.github.com/Deepseasaltyfish/364776e72f8a92d5e9baf7e1ee55a5c2)
# Fixes and Improvements
- Closed `INTERNAL`/`CONNECTING` doors will actually block moving now (@poizzy)
  - Not quite well on doors that are not axis-aligned
- Make snowy track's bounding box height better match its appearance
- Replace old `Turn` with a more accurate algorithm to simulate circles (@Deepseasaltyfish)
  - Placed `Turn`s won't be affected, and you can manually re-enable the legacy ones in config `balance/EnableLegacyTurn`
- Refine Track Exchanger GUI to a Blueprint-like one (@Deepseasaltyfish)
- Make Track Exchanger able to set bed block via clicking on the secondary hand, similar to Track Blueprint (@Deepseasaltyfish)
- Fix wrong item absorbing implementation on `MALLET_WALSCHAERTS` stocks (@Deepseasaltyfish)
- Fix fluid-related augments not able to interact with statically built pipe graphs, like IE
# Changes
- Use new format of defining units in default pack
- Change detector augment redstone power output to weak power to avoid bugs
  - You can manually set it back to strong power in config `debug/detectorOutputStrongCharging`
- Move `deepSnow` from config `debug` to `balance`