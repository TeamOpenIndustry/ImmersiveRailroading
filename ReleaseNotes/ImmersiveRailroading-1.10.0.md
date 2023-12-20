# Immersive Railroading 1.10.0 Release Candidate

After over two years in the making, Immersive Railroading is ready for it's next release!  This release has a truly
staggering number of new features which the community has been helping hone through a myriad of test builds.

## Features

### Controls, Gauges, and Doors

Many pieces of rolling stock can now be directly interacted with: https://www.youtube.com/watch?v=CNVSjASu15c. These
interactions are defined by the modelers as part of their stock configuration or as group names in their models.
Additionally
the scroll wheel can be used to actuate controls https://www.youtube.com/watch?v=qh3phRIMGg4.

Controls are items that can be dragged by players that usually control an aspect of the stock's function. Throttles,
Brakes, Firebox doors, and many more are potential controls. A full list can be found
in [ModelComponentType.java](https://github.com/TeamOpenIndustry/ImmersiveRailroading/blob/master/src/main/java/cam72cam/immersiverailroading/library/ModelComponentType.java#L89-L103)
.
Sounds can be tied to control movement via a block in the
stock's [sound configuration](https://github.com/TeamOpenIndustry/ImmersiveRailroading/blob/master/src/main/resources/assets/immersiverailroading/rolling_stock/default/base.caml#L87-L92)
.
An example can be found as part of
the [default steam configuration](https://github.com/TeamOpenIndustry/ImmersiveRailroading/blob/master/src/main/resources/assets/immersiverailroading/rolling_stock/default/steam.caml#L35-L51)
.

Readouts (Gauges) are quite similar, but are controlled by some property of the stock. Boiler pressure, Brake Pressure,
Speed and many more options are available. A full list can be found
in [ModelComponentType.java](https://github.com/TeamOpenIndustry/ImmersiveRailroading/blob/master/src/main/java/cam72cam/immersiverailroading/library/ModelComponentType.java#L106-L115)

An explanation of many of the controls and readouts can be found
in https://gist.github.com/cam72cam/132c47ff46a3ef942d09991af286ce52.
If adding `_FLAGS_` to your model names seems tedious, many of these same options are available in the stock's
configuration (see below).
Since that document was written, a few additions have been made:

* Add RANGE_MIN_MAX (ex GAUGE_LIQUID_1_RANGE_0.8_1.0 for sight glasses)
* Add HIDE modifier
* OFFSET_NUMBER: see diesel gauges for examples
* INVERT inverts how the TL/ROT/SCALE is applied
* NOINTERACT or NOTOUCH prevents the control from being actuated by the player
* Allow controls on many POSITIONs
    - BOGEY_FRONT / BOGEY_REAR
    - FRONT_LOCOMOTIVE / REAR_LOCOMOTIVE
    - Can be omitted
    - WIDGET_1, WIDGET_BOGEY_FRONT_1, etc...

Doors are what they say on the tin. They are effectively a Control that manages how players can enter/exit pices of
stock.
Doors usually follow the pattern `EXTERNAL_DOOR_#ID#`, `INTERNAL_DOOR_#ID#`, or `DOOR_#ID#`.

* External doors can allow players to walk into and out of rolling stock by walking through an open doorway.
* Internal doors can be used to manage players walking between cars.
* Any other doors are currently just for show, but can be interacted with the same as standard controls.

### Lighting

Custom lighting has been requested for a long time and is finally here!

There are three different types of lighting available:

* Headlights (flares) allow you to create custom light flares for your rolling stock
    - These have customizable colors and textures.
    - If optifine is present and optifine's dynamic light feature is turned on, in-world dynamic lighting is enabled.
    - A demo can be seen here: https://www.youtube.com/watch?v=Z0NfqwpMqUI
* For ambient lights that should still seem very bright add _FULLBRIGHT to a group (emissive).
* Dimmable / controllable lights can be added with Light Control Groups
    - Adding LCG_#CGNAME# to a group will use the CG value set by some other control to provide the lighting level for
      this object.

The above [control document](https://gist.github.com/cam72cam/132c47ff46a3ef942d09991af286ce52) also contains most of
the information
required for setting up custom lighting on your stock.

Note: Cars must be connected to locomotives that are turned on for lights to be enabled

### CAML

Created an alternative to JSON files called CAML: Cam's Awesome Markup Language.

* Goal: Simpler and Easier to use
* Can be used in place of any normal JSON file in IR
*
Specification: https://github.com/TeamOpenIndustry/ImmersiveRailroading/blob/ad9f91184894e77bf4a5424fe0cc53eddf4565bd/src/main/java/cam72cam/immersiverailroading/util/CAML.java#L14-L36
* Online tool for converting CAML to JSON and JSON to CAML: https://teamopenindustry.cc/caml/

### Custom User Interfaces

Modelers can now create their own user interfaces for rolling stock!  This is an incredibly flexible system which shares
many similarities with the new Control/Readout system. Additionally all stock overlay guis are now click/draggable:
https://www.youtube.com/watch?v=MI3X8eC6ZcI

Some initial documentation can be found [here](https://gist.github.com/cam72cam/40acfe4a8c34768d2dd8f020903f2067). I
also
recorded an [hour-long intensive](https://www.youtube.com/watch?v=NbzYrK9eIBg).

Additional features that have been added since the prior documentation was written:

* Added clamp = FLOOR and clamp = CEIL to GUIs (round down vs round up)
* Add global: true to propogate this control value across all connected stock

### Custom Animations

Custom animations are another incredible new tool for modelers. Animations can be exported from blender
using [animatrix.py](https://github.com/TeamOpenIndustry/ImmersiveRailroading/blob/master/animatrix.py) and then
attached
to either controls/readouts/doors or used to animate valve gears. An example video can be found
here: https://www.youtube.com/live/CWLGKaYG7k4.
Additionally, you can attach sounds to the animations for an incredibly immersive experience.

Mode detailed information can be found
in [base.caml](https://github.com/TeamOpenIndustry/ImmersiveRailroading/blob/master/src/main/resources/assets/immersiverailroading/rolling_stock/default/base.caml#L107-L127)

### Stock Sway

* Add sway to trains
    - https://www.youtube.com/watch?v=yBFonNGqiAo
    - Track json "bumpiness", defaulted to 1.0 on clacking track
    - stock "properties" -> "swayMultiplier"
* Add tilting around curves on supported stock
    - stock "properties" -> "tiltMultiplier"
    - https://www.youtube.com/watch?v=VTnZrP1o0lw

### Physics

This release also includes an entirely new Physics engine!  This was written from scratch and fixes an incredible number
of legacy bugs with the mod. Additionally, it is easy to update and expand moving forward.

Notable changes

* Added coupler slack https://www.youtube.com/watch?v=O-boGSqi_8c
* Added Cog support
    - Locomotives: "properties" -> "cog": true
    - Track: "cog": true
* Added brake pressure to balance braking
    - Red line in GUI
    - Config to disable and use old brake code
    - Brake pressure does not cross shunted stock
* Custom Brake Shoes
    - "properties" -> "brake_shoe_material"
    - STEEL/CAST_IRON/WOOD (on steel wheels)
    - Default is CAST_IRON
    - Can override Uk with "properties" -> "brake_friction_coefficient"
* Added starting resistance in physics calculations
* Add "properties" -> "rolling_resistance_coefficient"
* Better locomotive starting traction calculations
* More accurate tractive effort calculations
* Integrate block hardness into physics calculations (dirt = low resistance, obsidian = high resistance)
* Improved server -> client tick time synchronization

### Performance

* Re-add texture LevelOfDetail (LOD)
    - Dramatically reduces VRAM used
    - Multi-level so that models look progressively better as you get closer
    - Does not drastically increase texture size (~15%)
    - Configured with ConfigGraphics > StockLODDistance
* Smoother world loading:
    - Threaded track model generation
    - Threaded stock model/texture loading
* At least 4x faster track pathing performance
* Reduce number of future stock states calculated by default (performance improvement / can be increased for slower
  networks)

### Rolling Stock Configuration

The rolling stock configuration saw many expansions in this latest release. Support for CAML was added, along with
dozens of
new options for modelers to tweak!

With all of these new features, documentation was required. Examples and comments on all of the possible configuration
fields
can be found
in [the rolling stock default folder](https://github.com/TeamOpenIndustry/ImmersiveRailroading/tree/master/src/main/resources/assets/immersiverailroading/rolling_stock/default)

#### General:

* tigerbird1: extra tooltip infos "extra_tooltip_info": {}
* Allows stock to opt into "properties" -> "independent_brake": true
    - Locomotives have independent brakes turned on by default
* Custom coupler sounds: "sounds" -> "couple"
* Collision sound effect from @JediNoob124
    - "sounds" -> "collision"
* Flange noise on curves from @JediNoob124
    - "sounds" -> "flange"
    - "sounds" -> "flange_min_yaw"
* Sliding sound effect (brake overwhelming wheel friction) from @JediNoob124
    - "sounds" -> "sliding"
* More Complex Sounds:
    - Certain sounds can now be specified either as a single file or a block that defines at least one of
      start/main/stop
    - Diesel: idle, horn, bell
    - Steam: idle
    - "animations" -> "sound"
    -
    See [sound_definition.caml](https://github.com/TeamOpenIndustry/ImmersiveRailroading/blob/master/src/main/resources/assets/immersiverailroading/rolling_stock/default/sound_definition.caml)
* Allow passenger cars to carry cargo (same json fields as freight cars)

#### Diesel Locomotives:

* Add diesel notching (only 8 for now)
* Add ability to specify diesel throttle notches (properties -> throttle_notches)
* Added cab car support (properties -> cab_car : true)
* Idle vs Running sounds for diesels
    - sounds -> engine_pitch_range (0.25 default)

#### Steam Locomotives:

* Cylinder drains and control
    - Demo: https://www.youtube.com/watch?v=KvROsBbBf4o
    - "sounds": "cylinder_drain"
    - Default sounds from Admiral
* Custom smoke/steam particle textures

### Rolling Stock Model Components

As always, a full list of available model components can be found
in [ModelComponentType.java](https://github.com/TeamOpenIndustry/ImmersiveRailroading/blob/master/src/main/java/cam72cam/immersiverailroading/library/ModelComponentType.java#L106-L115)

* Support valve gear (LEFT/INNER_LEFT/CENTER/INNER_RIGHT/RIGHT) on all stock
* Render cargo items inside stock model (if area specified)
    - Create a box (or boxes) in the model named CARGO_ITEMS_#ID#
    - `_STRETCHED` will non uniformly scale the items to exactly fill the area
    - `_IRSIZE` will render IR items in the inventory at full size
* Add Seats: SEAT_#ID#
    - Works for both players and villagers
    - Sitting happens on left click

### Updated User Interfaces

The Track Configuration UI was completely overhauled.  It now shows an interactive preview of the track that will be built, along with
adding searchable and scrollable selectors for many of the different options.

The Paint Brush UI went through a similar overhaul.  A searchable and scrollable list of all the model's textures is now shown, along with
an animated preview of the stock in question. Tigerbird added Modes to paint brush: gui, random single mode, random train mode.  A 
new item model was added by NathanTheSteelMan.

Some of the existing UIs got much needed TLC, notably a search was added to the item picker GUIs and the plate roller GUI was simplified.

### Server Permissions

Added server-level player permissions to config/immersiverailroading_permissions.cfg. These also integrate with
permissions mods such as PermissionsEx and FTBTeams. The full list is defined
in [src/main/java/cam72cam/immersiverailroading/library/Permissions.java](https://github.com/TeamOpenIndustry/ImmersiveRailroading/blob/master/src/main/java/cam72cam/immersiverailroading/library/Permissions.java)

* LOCOMOTIVE_CONTROL
* BRAKE_CONTROL
* FREIGHT_INVENTORY
* COUPLING_HOOK
* PAINT_BRUSH
* STOCK_ASSEMBLY
* BOARD_LOCOMOTIVE
* BOARD_STOCK
* BOARD_WITH_LEAD
* CONDUCTOR
* AUGMENT_TRACK
* SWITCH_CONTROL
* EXCHANGE_TRACK
* BUILD_TRACK
* BREAK_TRACK
* MACHINIST

### Computer Integration

* Support engine start via CC and OC
* Add independent brake info to CC and OC info
* Add all standard OC functions to remote card
* Support for ComputerCraft polling events
* Automation now works with the new reverser code
    - computers have new functions available
    - throttle_forward/throttle_reverse are now throttle/reverser

### Track

* Add track curvosity setting for changing how harsh a custom curve is
* Add basic routing with nametags
    - https://www.youtube.com/watch?v=zH6yUg8LMgw
    - Uses the same tagging system as CC/OC
* Support multiple options for track materials:
    - ex: "item": "ore:concrete, ore:cobblestone"
* Click location to set turntable positioning
* Push/Pull mode for item/fluid augments (use piston to change mode)
* tigerbird: Add wireless reset to switch key
* Added option to disable clacks in track definition `"clack": false`

### Crafting

* Reduce wooden train component cost
* Add all steel block / ingot casts to basin
* Increase steel -> rail ratio
* Add railcraft support for crafting materials

### Config Options

* Add steam/diesel reverser (disabled by default, look in "Immersion Level" Config)
* Detailed Sound Categories in Config Settings
* New config option: ConfigSound -> scaleSoundToGauge
* #1302: global config for sound scaling to gauges
* Add config option for how often sway should be applied
* Add config for showing stock variants (slower bootup / requires restart when config changed)

### Miscellaneous

* More intense wheelslip
* tigerbird: Add new death by train message!
* Add pack to track.json and stock.json, change track tooltip
    - Can specify pack in track.json and stock.json and it will auto apply to all models added by the pack
* Allow interacting with the world while riding stock
* Support interacting with blocks/items from the train
    - To open GUIs you now need an empty hand
* Reduce chuff speed pitching (faster -> higher pitch)
* Adds keybinds for independent brake (linked to train brake keys by default )

## Bugfixes

* Java versions newer than 1.8 are now supported!
* New cache structure that will auto-clean old entries when packs/models are removed
    - This will REGENERATE YOUR CACHE on the first load!
    - Cache now automatically fixes itself when files go missing
* Fix potential item dupe issue
* Don't multiply diesel tank capacity by 10 (old bug). This will reduce your fuel capacity in all diesel locomotives.
* Work around mods breaking fuzzy (oredict/tags) items
* Better support for keepStockLoaded=false
* Fix rounding errors in non-standard gauge stock definitions
* Fix lots of non-standard gauge issues:
    - Widget translations
    - Widget labels
    - Headlights
    - Front/Rear locomotive bogies
    - Sounds scaled to specified gauge by default
    - Track "model_spacing_m" for custom length track pieces
* Fix cargo scale on smaller gauges
* Improve bogey/truck pathing
* Steam Chimney double velocity offset
* Track following (esp switches)
* Reduce "clanginess" of switches/short tracks
* Enforce turn table size limitations properly
* tigerbird: fix augment stacking after first place/remove
* tigerbird: fix stock component stacking
* Fix stock sometimes skewing across a switch (fun bug from the beginning of IR)
* Prevent chat spam when lots of track component options available
* Improve diesel key repeat cooldown
* Optifine FastRender no longer breaks icons (nasty hack)
* Disabled item component tab (not useful, caused JEI lag)
* Fixed smaller gauge stock not aligned to tracks
* Allow tuning of MB of memory reserved for stock loading (default to 1 thread per 1GB of avail memory)

## Known Issues

* Moving around stock while riding may desync server side / be somewhat buggy
* Sometimes offline mode will cause issues, UMC issue that will be patched later
* Custom animation widgets occasionally desync
* Locked switches occasionally desync
