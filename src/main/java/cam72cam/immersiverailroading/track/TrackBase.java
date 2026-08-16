package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.blocks.BlockRailBase;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.tile.TileRailGag;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.math.Plane;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.SingleCache;

public abstract class TrackBase {
	public BuilderBase builder;
	//In overlays we have to use the re-defined origin, overwrite here
	//TODO Find a better workaround
	protected Vec3i correctedOverlayBuilder;

	protected Vec3i rel;
	private float bedHeight;
	private float railHeight;
	private Plane bedFace;
	//Override default value
	private boolean scaleModel = true;

	protected BlockRailBase block;

	private boolean flexible = false;

	private Vec3i parent;

	public boolean solidNotRequired;

	public TrackBase(BuilderBase builder, Vec3i rel, BlockRailBase block) {
		this.builder = builder;
		this.rel = rel;
		this.block = block;
		this.correctedOverlayBuilder = builder.pos;
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

	public boolean isOverTileRail() {
		return builder.world.getBlockEntity(getPos(), TileRail.class) != null && this instanceof TrackGag;
	}

	public boolean canPlaceTrack() {
		Vec3i pos = getPos();

		return isDownSolid(true) && (BlockUtil.canBeReplaced(builder.world, pos, flexible || builder.overrideFlexible) || isOverTileRail());
	}

	public TileRailBase placeTrack(boolean actuallyPlace) {
		Vec3i pos = getPos();

		if (!actuallyPlace) {
			TileRailGag tr = (TileRailGag) IRBlocks.BLOCK_RAIL_GAG.createBlockEntity(builder.world, pos);
			if (parent != null) {
				tr.setParent(parent);
			} else {
				tr.setParent(builder.getParentPos());
			}
			tr.setRailHeight(getRailHeight());
			tr.setBedHeight(getBedHeight());
			tr.setSnowLayers((int) Math.floor(getBedHeight() * 8f));
			tr.setBedFace(getBedFace());
			tr.setScaleModel(isScaleModel());
			return tr;
		}

		Vec3i bedFillPos;
		if(getBedFace() != null) { // TODO: they may be broken by bed block
			Facing facing = Facing.fromNormal(getBedFace().normal);
			Vec3i axis = new Vec3i(facing.getXMultiplier(), facing.getYMultiplier(), facing.getZMultiplier());
			bedFillPos = pos.add(axis);
		} else {
			bedFillPos = pos.down();
		}
		if (!builder.info.settings.railBedFill.isEmpty() && BlockUtil.canBeReplaced(builder.world, bedFillPos, false)) {
			builder.world.setBlock(bedFillPos, builder.info.settings.railBedFill);
		}

		TagCompound replaced = null;
		int hasSnow = 0;
		TileRailBase te = null;
		if (!builder.world.isAir(pos)) {
			if (builder.world.isBlock(pos, IRBlocks.BLOCK_RAIL_GAG)) {
				te = builder.world.getBlockEntity(pos, TileRailBase.class);
				if (te != null) {
					replaced = te.getData();
				}
			} else {
				hasSnow = builder.world.getSnowLevel(pos);
				builder.world.breakBlock(pos);
			}
		}
		
        if (te != null) {
            te.setWillBeReplaced(true);
        }
        builder.world.setBlock(pos, block);
        if (te != null) {
            te.setWillBeReplaced(false);
        }

		TileRailBase tr = builder.world.getBlockEntity(pos, TileRailBase.class);
		tr.setReplaced(replaced);
		if (parent != null) {
			tr.setParent(parent);
		} else {
			tr.setParent(builder.getParentPos());
		}
		tr.setRailHeight(getRailHeight());
		tr.setBedHeight(getBedHeight());
		tr.setSnowLayers(tr.getMinSnowLayers());
		tr.setBedFace(getBedFace());
		tr.setScaleModel(isScaleModel());
		for (int i = 0; i < hasSnow; i++) {
			tr.handleSnowTick();
		}
		return tr;
	}

	private final SingleCache<Vec3i, Vec3i> posCache = new SingleCache<>(pos -> pos.add(rel));
	public Vec3i getPos() {
		return posCache.get(correctedOverlayBuilder);
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

	public void setBedFace(Plane bedFace) {
		this.bedFace = bedFace;
	}
	public Plane getBedFace	() {
		return bedFace;
	}

	public void setScaleModel(boolean scaleModel) {
		this.scaleModel = scaleModel;
	}
	public boolean isScaleModel() {
		return scaleModel;
	}

	public void setFlexible() {
		this.flexible  = true;
	}

	public boolean isFlexible() {
		return this.flexible;
	}

	public void overrideBuilderPos(Vec3i pos) {
		this.correctedOverlayBuilder = pos;
	}

	public void overrideParent(Vec3i blockPos) {
		this.parent = blockPos;
	}
}
