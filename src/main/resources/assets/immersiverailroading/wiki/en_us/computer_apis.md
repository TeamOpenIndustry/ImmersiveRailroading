# Overview
IR is fully compatible with [OpenComputers](https://www.curseforge.com/minecraft/mc-mods/opencomputers), and mostly compatible with ComputerCraft. You can find a Getting Started Guide for using IR with OC [here](immersiverailroading:wiki/en_us/open_computers.md).

## Augments
All augments interface with OC via an Adapter block, and are exposed using the `components` API. They will also fire OpenComputers events, using the name `ir_train_overhead`; you can listen for them with:
```lua
local event_name, net_address, augment_type, stock_uuid = event.pull("ir_train_overhead")
```

## Loco Control Augment
The component name is `ir_augment_control`. To learn more about this augment, see it's [dedicated page](immersiverailroading:wiki/en_us/augment_control.md). Don't forget to set the augment to COMPUTER mode by right-clicking it with a redstone torch!
```lua
setThrottle(number): sets the throttle to a value between -1 and 1, where -1 is full-reverse and 1 is full-forward
setBrake(number): sets the brake to a value between 0 and 1
setHorn(number): fires off the locomotive's horn for the given number of ticks
getPos(): returns the augment's position
getAugmentType(): returns the augment's type
```

## Detector Augment
The component name is `ir_augment_detector`. To learn more about this augment, see it's [dedicated page](immersiverailroading:wiki/en_us/augment_detector.md).

```lua
info(): returns information about the stock/locomotive overhead
consist(): returns summary information about the full train overhead
getTag(): returns the stock/locomotive tag
setTag(string): sets the stock/locomotive tag
getPos(): returns the augment's position
getAugmentType(): returns the augment's type
```

## Cards
IR provides cards for Open Computers, however no analogue exists for ComputerCraft as of this revision.

## Radio Control Card
See it's [dedicated page](immersiverailroading:wiki/en_us/opencomputers_train_radio_control_card.md)
