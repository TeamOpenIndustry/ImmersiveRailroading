package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.blocks.BlockRailBase;
import cam72cam.immersiverailroading.tile.RailInstance;
import cam72cam.immersiverailroading.tile.RailBaseInstance;
import cam72cam.immersiverailroading.tile.RailGagInstance;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.TagCompound;
import net.minecraft.block.Block;

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
            // Block below is replaceable and we will replace it with something
            (BlockUtil.canBeReplaced(builder.info.world, pos.down(), false) && !builder.info.settings.railBedFill.isEmpty()) ||
            // Block below is an IR Rail
            BlockUtil.isIRRail(builder.info.world, pos.down());
	}

	public boolean isOverTileRail() {
		return builder.info.world.getTileEntity(getPos(), RailInstance.class) != null && this instanceof TrackGag;
	}

	@SuppressWarnings("deprecation")
	public boolean canPlaceTrack() {
		Vec3i pos = getPos();

		return isDownSolid() && (BlockUtil.canBeReplaced(builder.info.world, pos, flexible || builder.overrideFlexible) || isOverTileRail());
	}

	public RailBaseInstance placeTrack(boolean actuallyPlace) {
		Vec3i pos = getPos();

		if (!actuallyPlace) {
			RailGagInstance tr = new RailGagInstance();
			tr.setPos(pos);
			tr.setWorld(builder.info.world);
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
		
		Block removed = builder.info.world.getBlockInternal(pos);
		RailBaseInstance te = null;
		if (removed != null) {
			if (builder.info.world.isBlock(pos, IRBlocks.BLOCK_RAIL_GAG)) {
				te = builder.info.world.getTileEntity(pos, RailBaseInstance.class);
				if (te != null) {
					replaced = new TagCompound();
					te.save(replaced);
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

		RailBaseInstance tr = builder.info.world.getTileEntity(pos, RailBaseInstance.class);
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
