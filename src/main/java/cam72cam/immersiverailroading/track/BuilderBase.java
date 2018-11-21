package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.Config.ConfigBalance;
import cam72cam.immersiverailroading.Config.ConfigDamage;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//TODO @cam72cam use BlockPos and Vec3i

@SuppressWarnings("incomplete-switch")
public abstract class BuilderBase {
	protected ArrayList<TrackBase> tracks = new ArrayList<TrackBase>();
	
	public RailInfo info;

	private BlockPos pos;
	private BlockPos parent_pos;

	public boolean overrideFlexible = false;

	public List<ItemStack> drops;

	public BuilderBase(RailInfo info, BlockPos pos) {
		this.info = info;
		this.pos = pos;
		parent_pos = pos;
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
			this.length = -1;
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
	
	public PosRot convertRelativePositions(BlockPos rel) {
		return new PosRot(pos.add(BlockUtil.rotateYaw(rel, info.placementInfo.facing.getOpposite())), info.placementInfo.facing);
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

	
	public void setParentPos(BlockPos pos) {
		parent_pos = convertRelativePositions(pos);
	}
	public BlockPos getParentPos() {
		return parent_pos;
	}
	
	public int costTies() {
		return MathHelper.ceil(this.tracks.size()/3 * ConfigBalance.TieCostMultiplier);
	}
	
	public int costRails() {
		return MathHelper.ceil(this.tracks.size()*2/3 * ConfigBalance.RailCostMultiplier);
	}
	
	public int costBed() {
		//TODO more accurate
		return MathHelper.ceil(this.tracks.size() * 0.1 * ConfigBalance.BedCostMultiplier);
	}

	public int costFill() {
		int fillCount = 0;
		for (TrackBase track : tracks) {
			if (BlockUtil.canBeReplaced(info.world, track.getPos().down(), false)) {
				fillCount += 1;
			}
		}
		return MathHelper.ceil(this.info.settings.railBedFill.getItem() != Items.AIR ? fillCount : 0);
	}

	public void setDrops(List<ItemStack> drops) {
		this.drops = drops;
	}

	public void clearArea() {
		for (TrackBase track : tracks) {
			for (int i = 0; i < 6 * info.settings.gauge.scale(); i++) {
				BlockPos main = track.getPos().up(i);
				if (!BlockUtil.isRail(info.world, main)) {
					info.world.destroyBlock(main, false);
				}
				if (info.settings.gauge.isModel() && ConfigDamage.enableSideBlockClearing && info.settings.type != TrackItems.SLOPE && info.settings.type != TrackItems.TURNTABLE) {
					for (EnumFacing facing : EnumFacing.HORIZONTALS) {
						BlockPos pos = main.offset(facing);
						if (!BlockUtil.isRail(info.world, pos)) {
							info.world.destroyBlock(pos, false);
						}
					}
				}
			}
			if (BlockUtil.canBeReplaced(info.world, track.getPos().down(), false)) {
				info.world.destroyBlock(track.getPos().down(), false);
			}
		}
	}
}
