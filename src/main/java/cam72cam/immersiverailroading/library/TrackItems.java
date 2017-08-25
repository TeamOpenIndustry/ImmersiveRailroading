package cam72cam.immersiverailroading.library;

import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.track.BuilderBase;
import cam72cam.immersiverailroading.track.BuilderCrossing;
import cam72cam.immersiverailroading.track.BuilderSlope;
import cam72cam.immersiverailroading.track.BuilderStraight;
import cam72cam.immersiverailroading.track.BuilderTurn;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public enum TrackItems implements IStringSerializable {
	STRAIGHT,
	CROSSING,
	SLOPE,
	TURN,
	SWITCH,
	PARALEL_SWITCH,
	;
	
	public int getMeta() {
		return this.ordinal();
	}
	
	public static TrackItems fromMeta(int meta) {
		return TrackItems.values()[meta];
	}
	@Override
	public String getName() {
		return this.name().toLowerCase(); 
	}
	
	@Override
	public String toString() {
	    return getName();
	}

	public boolean isTurn() {
		return this == TURN;
	}
	
	public BuilderBase getBuilder(World world, BlockPos pos, EnumFacing facing, int length, int quarter, int quarters, TrackDirection direction, float horizOff) {
		switch (this) {
		case STRAIGHT:
			return new BuilderStraight(world, pos.getX(), pos.getY(), pos.getZ(), facing, length, quarter, horizOff);
		case CROSSING:
			return new BuilderCrossing(world, pos.getX(), pos.getY(), pos.getZ(), facing, quarter, horizOff);
		case SLOPE:
			return new BuilderSlope(world, pos.getX(), pos.getY(), pos.getZ(), facing, length, quarter, horizOff);
		case TURN:
			return new BuilderTurn(world, pos.getX(), pos.getY(), pos.getZ(), facing, length, quarter, quarters, direction, horizOff);
		default:
			return null;
		}
	}
	public BuilderBase getBuilder(TileRail te) {
		return getBuilder(te, te.getPos());
	}

	public BuilderBase getBuilder(TileRail te, BlockPos blockPos) {
		return te.getType().getBuilder(te.getWorld(), blockPos, te.getFacing().getOpposite(), te.getLength(), te.getRotationQuarter(), te.getTurnQuarters(), te.getDirection(), te.getHorizOff());
	}
}