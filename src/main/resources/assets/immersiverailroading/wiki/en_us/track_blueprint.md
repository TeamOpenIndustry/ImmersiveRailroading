## Overview

This tool is used to lay down rails for rolling stock to travel on. It is crafted with a piece of paper and six Steel Ingots in an H shape, like so:

![craft](immersiverailroading:wiki/images/track1.png)

With the blueprint in hand, right-click in midair to change its settings. Right-click a block to place the railbed, which requires Track Segments from the [Track Roller](immersiverailroading:wiki/en_us/track_roller.md) and Treated Wood Planks (Normal planks if IE is not installed).

## In game UI

![UI](immersiverailroading:wiki/images/track2.png)

## Settings

## Type:

This setting determines what shape your track will take. Current options are Straight, Slope, Turn, Switch, Crossing, Turntable, and Custom Curve.

NOTE: The Crossing and Turn types are obsolete as of version 1.5.0. Tracks can now cross other tracks of any gauge and at any angle. The new Custom Curve type, which is far less finicky, more flexible, and much easier to use, has rendered Turn completely useless. Crossing and Turn remain in the mod purely for backwards compatibility. See [Custom Curves](immersiverailroading:wiki/en_us/custom_curves.md) for more info on them.

## Length:

This setting determines how many blocks long the piece of track is.

## Turn Degrees:

This setting shows up only for Turn and Switch types.  It determines how large/long the turn's angle should be.

## Rail Bed:

This setting determines what visual rail bed should be placed between the rail ties.  This option is useful for debugging when you want to see where the track blocks will actually be placed. Entries are pulled from the `railBed` ore dictionary and can be added to. If you right-click on track in the world with the Track Blueprint in your offhand, the blueprint will mimic the rail bed of the track you clicked on.

![Brick Rail Bed](immersiverailroading:wiki/images/track3.png)

## Rail Bed Fill:

This setting is useful for building bridges.  It auto-places blocks from your inventory under the rail bed as it builds the tracks. It uses the same `railBed` ore dictionary. If you shift-right-click on track in the world with the Track Blueprint in your offhand, the blueprint will mimic the rail bed fill of the track you clicked on.

![Bed Fill](immersiverailroading:wiki/images/track4.png)

## Position:

This setting determines how the track should be locked to the rail bed.

* Fixed: Locks the track directly to the rail bed, no flexibility at all
* Pixels: Allows free placement of the track (rounded to the nearest 16th of a block)
* Pixels Locked: Allows free placement of the track forward and backward, but locks side to side motion
* Smooth: Allows free placement of the track
* Smooth Locked: Allows free placement of the track forward and backward, but locks side to side motion

## Gauge:

This setting determines what gauge the track should be built at

## Grade Crossing:

Extends the railbed out to the sides, to have the appearance of a level crossing; such as those used at intersections of roads and railroads tracks in real life.

## Place Blueprint:

You can place down a blueprint which renders the track in-world.  This allows you to plan, move and shape the track as you go.

Right clicking on a placed blueprint will allow you to change the settings without having to replace it.

Shift \+ right clicking on a placed blueprint will allow you to shift it's position if it is in any mode except Fixed.

Breaking the block will remove the blueprint.  Shift \+ breaking the block will attempt to place the blueprint.
