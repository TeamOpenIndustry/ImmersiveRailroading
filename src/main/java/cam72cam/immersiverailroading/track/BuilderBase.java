package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.Config.ConfigBalance;
import cam72cam.immersiverailroading.Config.ConfigDamage;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.immersiverailroading.thirdparty.trackapi.ITrack;
import cam72cam.mod.world.World;

import java.util.ArrayList;
import java.util.List;

//TODO @cam72cam use Vec3i and Vec3i

@SuppressWarnings("incomplete-switch")
public abstract class BuilderBase {
	protected final World world;
	protected ArrayList<TrackBase> tracks = new ArrayList<>();
	
	public RailInfo info;

	public final Vec3i pos;
	private Vec3i parent_pos;

	public boolean overrideFlexible = true;

	public List<ItemStack> drops;

	public BuilderBase(RailInfo info, World world, Vec3i pos) {
		this.info = info;
		this.world = world;
		this.pos = pos;
		parent_pos = pos;
	}

	public abstract List<VecYPR> getRenderData();

	public boolean canBuild() {
		for(TrackBase track : tracks) {
			if (!track.canPlaceTrack()) {
				return false;
			}
		}
		return true;
	}
	
	public void build() {
		/*
		Assume we have already tested.
		There are a few edge cases which break with overlapping split builders
		if (!canBuild()) {
			return ;
		}
		*/
		for(TrackBase track : tracks) {
			if (!track.isOverTileRail()) {
				track.placeTrack(true).markDirty();
			} else {
				// Track -> Base?
				// To
				// Track -> Placed -> Base?

				TileRail rail = world.getBlockEntity(track.getPos(), TileRail.class);
				TileRailBase placed = track.placeTrack(false);
				placed.setReplaced(rail.getReplaced());
				rail.setReplaced(placed.getData());
				rail.markDirty();
			}
		}
	}
	
	public List<TrackBase> getTracksForRender() {
		return this.tracks;
	}

	public List<TrackBase> getTracksForFloating() {
		return this.tracks;
	}

	
	public void setParentPos(Vec3i pos) {
		parent_pos = this.pos.add(pos);
	}
	public Vec3i getParentPos() {
		return parent_pos;
	}
	
	public int costTies() {
		return (int) Math.ceil(this.tracks.size()/3 * ConfigBalance.TieCostMultiplier);
	}
	
	public int costRails() {
		return (int) Math.ceil(this.tracks.size()*2/3 * ConfigBalance.RailCostMultiplier / 2);
	}
	
	public int costBed() {
		//TODO more accurate
		return (int) Math.ceil(this.tracks.size() * 0.1 * ConfigBalance.BedCostMultiplier);
	}

	public int costFill() {
		int fillCount = 0;
		for (TrackBase track : tracks) {
			if (BlockUtil.canBeReplaced(world, track.getPos().down(), false)) {
				fillCount += 1;
			}
		}
		return (int) Math.ceil(!this.info.settings.railBedFill.isEmpty() ? fillCount : 0);
	}

	public void setDrops(List<ItemStack> drops) {
		this.drops = drops;
	}

	public void clearArea() { // Clear 6-block tall right-of-way ignoring snow UNLESS the snow directly intersects with the location of a future track tile.
		for (TrackBase track : tracks) {
			for (int i = 0; i < 6 * info.settings.gauge.scale(); i++) {
				Vec3i main = track.getPos().up(i);
				if (!ITrack.isRail(world, main) && (i == 0 || !world.isSnow(main))) {
					world.setToAir(main);
				}
				if (info.settings.gauge.isModel() && ConfigDamage.enableSideBlockClearing && info.settings.type != TrackItems.SLOPE && !info.settings.type.isTable()) {
					for (Facing facing : Facing.HORIZONTALS) {
						Vec3i pos = main.offset(facing);
						if (!ITrack.isRail(world, pos)) {
							world.setToAir(pos);
						}
					}
				}
			}
			if (BlockUtil.canBeReplaced(world, track.getPos().down(), false)) {
				world.setToAir(track.getPos().down());
			}
		}
	}
}
