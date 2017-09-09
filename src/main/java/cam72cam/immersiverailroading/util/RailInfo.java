package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.items.ItemRail;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.track.BuilderBase;
import cam72cam.immersiverailroading.track.BuilderCrossing;
import cam72cam.immersiverailroading.track.BuilderSlope;
import cam72cam.immersiverailroading.track.BuilderStraight;
import cam72cam.immersiverailroading.track.BuilderSwitch;
import cam72cam.immersiverailroading.track.BuilderTurn;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class RailInfo {
	public BlockPos position;
	public World world;
	public EnumFacing facing;
	public TrackItems type;
	public TrackDirection direction;
	public int length;
	public int quarter;
	public int quarters;
	public Vec3d placementPosition;

	// Used for tile rendering only
	public boolean snowRenderFlagDirty = false;
	public SwitchState switchState = SwitchState.NONE;
	
	
	public RailInfo(BlockPos position, World world, EnumFacing facing, TrackItems type, TrackDirection direction, int length, int quarter, int quarters, Vec3d placementPosition) {
		this.position = position;
		this.world = world;
		this.facing = facing;
		this.type = type;
		this.direction = direction;
		this.length = length;
		this.quarter = quarter;
		this.quarters = quarters;
		this.placementPosition = placementPosition;
	}
	
	public RailInfo(ItemStack stack, EntityPlayer player, BlockPos pos, float hitX, float hitY, float hitZ) {
		position = pos;
		world = player.getEntityWorld();
		length = ItemRail.getLength(stack);
		quarters = ItemRail.getQuarters(stack);
		
		float yawHead = player.getRotationYawHead() % 360 + 360;
		direction = (yawHead % 90 < 45) ? TrackDirection.RIGHT : TrackDirection.LEFT;
		//quarter = MathHelper.floor((yawHead % 90f) /(90)*4);
		float yawPartial = (yawHead+3600) % 90f;
		if (direction == TrackDirection.LEFT) {
			yawPartial = 90-yawPartial;
		}
		if (yawPartial < 15) {
			quarter = 0;
		} else if (yawPartial < 30) {
			quarter = 1;
		} else {
			quarter = 2;
		}
		
		facing = player.getHorizontalFacing();
		type = TrackItems.fromMeta(stack.getMetadata());

		
		hitX = ((int)(hitX * 10)) / 10f;
		hitZ = ((int)(hitZ * 10)) / 10f;
		
		//hitX = 0.5f;
		//hitZ = 0.5f;
		
		placementPosition = new Vec3d(pos).addVector(hitX, 0, hitZ);
		
		if (BlockUtil.canBeReplaced(world, pos.down(), true)) {
			pos = pos.down();
		}
	}
	
	public RailInfo clone() {
		RailInfo c = new RailInfo(position, world, facing, type, direction, length, quarter, quarters, placementPosition);
		return c;
	}
	
	public BuilderBase getBuilder(BlockPos pos) {
		switch (type) {
		case STRAIGHT:
			return new BuilderStraight(this, pos);
		case CROSSING:
			return new BuilderCrossing(this, pos);
		case SLOPE:
			return new BuilderSlope(this, pos);
		case TURN:
			return new BuilderTurn(this, pos);
		case SWITCH:
			return new BuilderSwitch(this, pos);
		default:
			return null;
		}
	}
	
	private BuilderBase builder;
	public BuilderBase getBuilder() {
		if (builder == null) {
			builder = getBuilder(new BlockPos(0,0,0));
		}
		return builder;
	}
}
