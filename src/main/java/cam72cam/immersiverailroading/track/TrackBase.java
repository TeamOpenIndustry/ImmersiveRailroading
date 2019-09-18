package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.blocks.BlockRailBase;
import cam72cam.immersiverailroading.tile.Rail;
import cam72cam.immersiverailroading.tile.RailBase;
import cam72cam.immersiverailroading.tile.RailGag;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.TagCompound;

public abstract class TrackBase {
	public BuilderBase builder;

	protected Vec3i rel;
	private float bedHeight;
	private float railHeight;

	protected BlockRailBase block;

	private boolean flexible = false;

	private Vec3i parent;

	public boolean solidNotRequired;

	public TrackBase(BuilderBase builder, Vec3i rel, BlockRailBase block) {
		this.builder = builder;
		this.rel = rel;
		this.block = block;
	}

	public boolean isDownSolid() {
		Vec3i pos = getPos();
		return
            // Config to bypass solid block requirement
            !Config.ConfigDamage.requireSolidBlocks ||
            // Turn table override
            solidNotRequired ||
            // Valid block beneath
            builder.info.world.isTopSolid(pos.down()) ||
            // BlockType below is replaceable and we will replace it with something
            (BlockUtil.canBeReplaced(builder.info.world, pos.down(), false) && !builder.info.settings.railBedFill.isEmpty()) ||
            // BlockType below is an IR Rail
            BlockUtil.isIRRail(builder.info.world, pos.down());
	}

	public boolean isOverTileRail() {
		return builder.info.world.getBlockEntity(getPos(), Rail.class) != null && this instanceof TrackGag;
	}

	@SuppressWarnings("deprecation")
	public boolean canPlaceTrack() {
		Vec3i pos = getPos();

		return isDownSolid() && (BlockUtil.canBeReplaced(builder.info.world, pos, flexible || builder.overrideFlexible) || isOverTileRail());
	}

	public RailBase placeTrack(boolean actuallyPlace) {
		Vec3i pos = getPos();

		if (!actuallyPlace) {
			TileEntity te = (TileEntity) IRBlocks.BLOCK_RAIL_GAG.internal.createTileEntity(null, null);
			te.setPos(pos);
			te.setWorld(builder.info.world);
			RailGag tr = (RailGag) te.instance();
			if (parent != null) {
				tr.setParent(parent);
			} else {
				tr.setParent(builder.getParentPos());
			}
			tr.setRailHeight(getRailHeight());
			tr.setBedHeight(getBedHeight());
			return tr;
		}

		if (!builder.info.settings.railBedFill.isEmpty() && BlockUtil.canBeReplaced(builder.info.world, pos.down(), false)) {
			builder.info.world.setBlock(pos.down(), builder.info.settings.railBedFill);
		}


		TagCompound replaced = null;
		
		RailBase te = null;
		if (!builder.info.world.isAir(pos)) {
			if (builder.info.world.isBlock(pos, IRBlocks.BLOCK_RAIL_GAG)) {
				te = builder.info.world.getBlockEntity(pos, RailBase.class);
				if (te != null) {
					replaced = te.getData();
				}
			} else {
				builder.info.world.breakBlock(pos);
			}
		}
		
        if (te != null) {
            te.setWillBeReplaced(true);
        }
        builder.info.world.setBlock(pos, block);
        if (te != null) {
            te.setWillBeReplaced(false);
        }

		RailBase tr = builder.info.world.getBlockEntity(pos, RailBase.class);
		tr.setReplaced(replaced);
		if (parent != null) {
			tr.setParent(parent);
		} else {
			tr.setParent(builder.getParentPos());
		}
		tr.setRailHeight(getRailHeight());
		tr.setBedHeight(getBedHeight());
		return tr;
	}

	public Vec3i getPos() {
		return builder.convertRelativePositions(rel);
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

	public boolean isFlexible() {
		return this.flexible;
	}

	public void overrideParent(Vec3i blockPos) {
		this.parent = blockPos;
	}
}
