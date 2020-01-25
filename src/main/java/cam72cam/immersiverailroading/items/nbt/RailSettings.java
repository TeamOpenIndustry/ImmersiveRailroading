package cam72cam.immersiverailroading.items.nbt;

import cam72cam.immersiverailroading.library.*;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.util.TagCompound;

public class RailSettings {
    public final Gauge gauge;
    public final TrackItems type;
    public final int length;
    public final int quarters;
    public final TrackPositionType posType;
    public final TrackSmoothing smoothing;
    public final TrackDirection direction;
    public final ItemStack railBed;
    public final ItemStack railBedFill;
    public final boolean isPreview;
    public final boolean isGradeCrossing;
    public final String track;

    public RailSettings(Gauge gauge, String track, TrackItems type, int length, int quarters, TrackPositionType posType, TrackSmoothing smoothing, TrackDirection direction, ItemStack railBed, ItemStack railBedFill, boolean isPreview, boolean isGradeCrossing) {
        this.gauge = gauge;
        this.track = track;
        this.type = type;
        this.length = length;
        this.quarters = quarters;
        this.posType = posType;
        this.smoothing = smoothing;
        this.direction = direction;
        this.railBed = railBed;
        this.railBedFill = railBedFill;
        this.isPreview = isPreview;
        this.isGradeCrossing = isGradeCrossing;
    }

    public RailSettings(TagCompound nbt) {
        if (nbt.hasKey("gauge")) {
            gauge = Gauge.from(nbt.getDouble("gauge"));
            track = nbt.getString("track");
            type = TrackItems.values()[nbt.getInteger("type")];
            length = nbt.getInteger("length");
            quarters = nbt.hasKey("quarters") ? nbt.getInteger("quarters") : 4;
            posType = TrackPositionType.values()[nbt.getInteger("pos_type")];
            smoothing = nbt.hasKey("smoothing") ?
                    TrackSmoothing.values()[nbt.getInteger("smoothing")] :
                    type == TrackItems.SLOPE ? TrackSmoothing.NEITHER : TrackSmoothing.BOTH;
            direction = TrackDirection.values()[nbt.getInteger("direction")];
            railBed = new ItemStack(nbt.get("bedItem"));
            railBedFill = new ItemStack(nbt.get("bedFill"));
            isPreview = nbt.getBoolean("isPreview");
            isGradeCrossing = nbt.getBoolean("isGradeCrossing");
        } else {
            gauge = Gauge.from(Gauge.STANDARD);
            type = TrackItems.STRAIGHT;
            track = "default";
            length = 10;
            quarters = 4;
            posType = TrackPositionType.FIXED;
            smoothing = TrackSmoothing.BOTH;
            direction = TrackDirection.NONE;
            railBed = ItemStack.EMPTY;
            railBedFill = ItemStack.EMPTY;
            isPreview = false;
            isGradeCrossing = false;
        }
    }

    public TagCompound toNBT() {
        TagCompound nbt = new TagCompound();
        nbt.setDouble("gauge", gauge.value());
        nbt.setString("track", track);
        nbt.setInteger("type", type.ordinal());
        nbt.setInteger("length", length);
        nbt.setInteger("quarters", quarters);
        nbt.setInteger("pos_type", posType.ordinal());
        nbt.setInteger("smoothing", smoothing.ordinal());
        nbt.setInteger("direction", direction.ordinal());
        nbt.set("bedItem", railBed.toTag());
        nbt.set("bedFill", railBedFill.toTag());
        nbt.setBoolean("isPreview", isPreview);
        nbt.setBoolean("isGradeCrossing", isGradeCrossing);
        return nbt;
    }

    public RailSettings withLength(int length) {
       return new RailSettings(
				gauge,
				track,
				type,
				length,
				quarters,
				posType,
                smoothing,
                direction,
				railBed,
				railBedFill,
				isPreview,
				isGradeCrossing
		) ;
    }

    public RailSettings withType(TrackItems type) {
        return new RailSettings(
                gauge,
                track,
                type,
                length,
                quarters,
                posType,
                smoothing,
                direction,
                railBed,
                railBedFill,
                isPreview,
                isGradeCrossing
        );
    }
    
    public RailSettings withTrack(String track) {
        return new RailSettings(
                gauge,
                track,
                type,
                length,
                quarters,
                posType,
                smoothing,
                direction,
                railBed,
                railBedFill,
                isPreview,
                isGradeCrossing
        );
    }
}
