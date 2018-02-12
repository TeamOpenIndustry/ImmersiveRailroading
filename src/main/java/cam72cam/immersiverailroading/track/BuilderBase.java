package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

//TODO @cam72cam use BlockPos and Vec3i

@SuppressWarnings("incomplete-switch")
public abstract class BuilderBase {
	protected ArrayList<TrackBase> tracks = new ArrayList<TrackBase>();
	
	public World world;
	int x;
	int y;
	int z;
	public EnumFacing rotation;
	
	private int[] translation;

	public RailInfo info;

	private BlockPos parent_pos;

	public boolean overrideFlexible = false;

	public List<ItemStack> drops;

	public Gauge gauge;
	
	public BuilderBase(RailInfo info, BlockPos pos) {
		this.info = info;
		rotation = info.facing;
		world = info.world;
		gauge = info.gauge;
		parent_pos = pos;
		this.x = pos.getX();
		this.y = pos.getY();
		this.z = pos.getZ();
	}

	public class VecYawPitch extends Vec3d {
		private float yaw;
		private float pitch;
		private float length;
		private List<String> groups;
		
		public VecYawPitch(double xIn, double yIn, double zIn, float yaw, String... groups) {
			super(xIn, yIn, zIn);
			this.yaw = yaw;
			this.pitch = 0;
			this.length = 1;
			this.groups = Arrays.asList(groups);
		}
		public VecYawPitch(double xIn, double yIn, double zIn, float yaw, float pitch, String... groups) {
			this(xIn, yIn, zIn, yaw, groups);
			this.pitch = pitch;
		}
		public VecYawPitch(double xIn, double yIn, double zIn, float yaw, float pitch, float length, String... groups) {
			this(xIn, yIn, zIn, yaw, pitch, groups);
			this.length = length;
		}
		public float getYaw() {
			return this.yaw;
		}
		public float getPitch() {
			return this.pitch;
		}
		public float getLength() {
			return this.length;
		}
		public List<String> getGroups() {
			return this.groups;
		}
	}
	
	public abstract List<VecYawPitch> getRenderData();
	
	public void setRelativeTranslate(int x, int y, int z) {
		translation = new int[]{x, y, z};
	}	
	
	public class PosRot extends BlockPos{
		private EnumFacing rot;
		
		public EnumFacing getRotation() {
			return rot;
		}

		public PosRot(BlockPos pos, EnumFacing rot) {
			super(pos);
			this.rot = rot;
		}
	}
	
	public PosRot convertRelativeCenterPositions(int rel_x, int rel_y, int rel_z, EnumFacing rel_rotation) {
		if (rel_x >= 1) {
			switch(rotation) {
			case SOUTH:
				rel_x += 0;
				rel_z += 0;
				break;
			case WEST:
				rel_x += 0;
				rel_z -= 1;
				break;
			case NORTH:
				rel_x -= 1;
				rel_z -= 1;
				break;
			case EAST:
				rel_x -= 1;
				rel_z -= 0;
				break;
			}
		} else {
			switch(rotation) {
			case EAST:
				rel_x += 0;
				rel_z += 0;
				break;
			case NORTH:
				rel_x += 0;
				rel_z -= 1;
				break;
			case WEST:
				rel_x += 1;
				rel_z -= 1;
				break;
			case SOUTH:
				rel_x += 1;
				rel_z += 0;
				break;
			}
		}
		return convertRelativePositions(rel_x, rel_y, rel_z, rel_rotation);
	}

	public PosRot convertRelativePositions(int rel_x, int rel_y, int rel_z, EnumFacing rel_rotation) {
		if (translation != null) {
			rel_x += translation[0];
			rel_y += translation[1];
			rel_z += translation[2];
		}
		
		EnumFacing newrot = EnumFacing.fromAngle(rel_rotation.getHorizontalAngle() + rotation.getHorizontalAngle());
		
		switch (rotation) {
		case SOUTH:
			// 270*
			return new PosRot(new BlockPos(x + rel_x, y + rel_y, z + rel_z), newrot);
		case WEST:
			// 180*
			return new PosRot(new BlockPos(x - rel_z, y + rel_y, z + rel_x), newrot);
		case NORTH:
			//  90*
			return new PosRot(new BlockPos(x - rel_x, y + rel_y, z - rel_z), newrot);
		case EAST:
			//   0*
			return new PosRot(new BlockPos(x + rel_z, y + rel_y, z - rel_x), newrot);
		}
		return null;
	}
	
	public int getX() {
		return x;
	}
	public int getY() {
		return y;
	}
	public int getZ() {
		return z;
	}

	public boolean canBuild() {
		for(TrackBase track : tracks) {
			if (!track.canPlaceTrack()) {
				return false;
			}
		}
		return true;
	}
	
	public void build() {
		if (!canBuild()) {
			return ;
		}
		for(TrackBase track : tracks) {
			track.placeTrack().markDirty();;
		}
	}
	
	public List<TrackBase> getTracksForRender() {
		return this.tracks;
	}

	public BlockPos getPos() {
		return new BlockPos(x, y, z);
	}

	
	public void setParentPos(BlockPos pos) {
		parent_pos = convertRelativePositions(pos.getX(), pos.getY(), pos.getZ(), this.rotation);
	}
	public BlockPos getParentPos() {
		return parent_pos;
	}
	
	public int costTies() {
		return MathHelper.ceil(this.tracks.size()/3 * Config.TieCostMultiplier);
	}
	
	public int costRails() {
		return MathHelper.ceil(this.tracks.size()*2/3 * Config.RailCostMultiplier);
	}
	
	public int costBed() {
		//TODO more accurate
		return MathHelper.ceil(this.tracks.size() * 0.1 * Config.BedCostMultiplier);
	}

	public int costFill() {
		int fillCount = 0;
		for (TrackBase track : tracks) {
			if (BlockUtil.canBeReplaced(world, track.getPos().down(), false)) {
				fillCount += 1;
			}
		}
		return MathHelper.ceil(this.info.railBedFill.getItem() != Items.AIR ? fillCount : 0);
	}

	public void setDrops(List<ItemStack> drops) {
		this.drops = drops;
	}

	public void clearArea() {
		for (TrackBase track : tracks) {
			for (int i = 0; i < 6 * gauge.scale(); i++) {
				BlockPos main = track.getPos().up(i);
				if (!BlockUtil.isRail(world, main)) {
					world.destroyBlock(main, false);
				}
				if (gauge != Gauge.MODEL && Config.enableSideBlockClearing) {
					for (EnumFacing facing : EnumFacing.HORIZONTALS) {
						BlockPos pos = main.offset(facing);
						if (!BlockUtil.isRail(world, pos)) {
							world.destroyBlock(pos, false);
						}
					}
				}
			}
			if (BlockUtil.canBeReplaced(world, track.getPos().down(), false)) {
				world.destroyBlock(track.getPos().down(), false);
			}
		}
	}
}
