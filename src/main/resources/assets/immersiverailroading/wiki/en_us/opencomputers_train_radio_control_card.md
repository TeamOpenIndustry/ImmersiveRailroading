The Train Radio-control Card provides greater control over locomotive automation. By replacing the static track [augments](immersiverailroading:wiki/en_us/augment_control.md) with a "wireless" card, trains need not be in specific spots to be controlled, allowing full command of rolling stock in a large area. This Card is only compatible with OpenComputers as of this page revision.

To use the Radio-control Card, it must first be linked with locomotive. Right-clicking a locomotive will link the card to it. Shift-Right-clicking unlinks the card.
Note that only configured locomotives can be used with the Card. Specifically, the configuration .json for the locomotive must define the following in the "properties" section.

`"radio_equipped":true`

Alternatively, this requirement can be disabled by modifying the IR balance configuration file such that 

`RadioEquipmentRequired = false`

Once the Card is linked (which will also be stated on the item tooltip), the Card needs to be placed into a Tier 0 or higher slot in any OpenComputers computer.
The Card can then be polled by the computer with the component name "ir_remote_control". If you are not sure how this is done, consult the OpenComputers Wiki.

The Card can now control the train in a similar fashion to the Locomotive Control Augment. The supported functions are

* setThrottle(number): Sets the throttle to a value between -1 and 1 (reverse/forward)
* setBrake(number): Sets the brake to a value between 0 and 1
* setHorn(number): fires off the locomotive's horn for the given number of ticks
* bell(): rings the locomotive bell
* getPos(): returns the locomotive's world position
* getLinkUUID(): returns the UUID of the linked loco


An important thing to take care of is that the Card has a limited range and power cost. By default, the range is 500m, and the RF cost per meter is 0. 
These can be modified in the balance config under RadioRange and RadioCostPerMeter.