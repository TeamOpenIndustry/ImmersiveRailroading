package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.data.TrackBlock;
import cam72cam.immersiverailroading.data.TrackInfo;
import cam72cam.immersiverailroading.data.WorldData;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.SingleCache;

public class TrackBase {
	public BuilderBase builder;

	protected Vec3i rel;
	private float bedHeight;
	private float railHeight;

	private boolean flexible = false;

	public boolean solidNotRequired;

	public TrackBase(BuilderBase builder, Vec3i rel) {
		this.builder = builder;
		this.rel = rel;
	}

	private final SingleCache<Vec3i, Vec3i> downCache = new SingleCache<>(Vec3i::down);
	public boolean isDownSolid(boolean countFill) {
		Vec3i pos = downCache.get(getPos());
		return
            // Config to bypass solid block requirement
            !Config.ConfigDamage.requireSolidBlocks ||
            // Turn table override
            solidNotRequired ||
            // Valid block beneath
            builder.world.isTopSolid(pos) ||
            // BlockType below is replaceable and we will replace it with something
            countFill && (BlockUtil.canBeReplaced(builder.world, pos, false) && !builder.info.settings.railBedFill.isEmpty()) ||
            // BlockType below is an IR Rail
            BlockUtil.isIRRail(builder.world, pos);
	}

	public boolean canPlaceTrack() {
		Vec3i pos = getPos();

		return isDownSolid(true) && BlockUtil.canBeReplaced(builder.world, pos, flexible || builder.overrideFlexible);
	}

	public void placeTrack(WorldData data, TrackInfo trackInfo) {
		Vec3i pos = getPos();

		if (!builder.info.settings.railBedFill.isEmpty() && BlockUtil.canBeReplaced(builder.world, pos.down(), false)) {
			builder.world.setBlock(pos.down(), builder.info.settings.railBedFill);
		}


		TrackBlock current = data.getTrackBlock(getPos());
		data.setTrackBlock(getPos(), new TrackBlock(trackInfo, getRailHeight(), getBedHeight(), builder.world.getSnowLevel(pos), current));

		//builder.world.setBlock(pos, block);
	}

	private final SingleCache<Vec3i, Vec3i> posCache = new SingleCache<>(pos -> pos.add(rel));
	public Vec3i getPos() {
		return posCache.get(builder.pos);
	}

	public void setHeight(float height) {
		setBedHeight(height);
		setRailHeight(height);
	}
	public void setBedHeight(float height) {
		this.bedHeight = height;
	}
	public float getBedHeight() {
		return bedHeight;
	}
	public void setRailHeight(float height) {
		this.railHeight = height;
	}
	public float getRailHeight() {
		return railHeight;
	}

	public void setFlexible() {
		this.flexible  = true;
	}
}
