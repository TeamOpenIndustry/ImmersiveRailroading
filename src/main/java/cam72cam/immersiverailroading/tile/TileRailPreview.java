package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.net.PreviewRenderPacket;
import cam72cam.immersiverailroading.track.IIterableTrack;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.PlacementInfo;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.block.BlockEntityTickable;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.util.Facing;

public class TileRailPreview extends BlockEntityTickable {
	private int ticksAlive;
	private RailInfo info;

	@TagField
	private ItemStack item;
	@TagField
	private PlacementInfo placementInfo;
	@TagField
	private PlacementInfo customInfo;
	@TagField
	private boolean isAboveRails = false;

	public ItemStack getItem() {
		return this.item;
	}
	
	public void setup(ItemStack stack, PlacementInfo info) {
		this.item = stack.copy();
		this.placementInfo = info;
		this.isAboveRails = BlockUtil.isIRRail(getWorld(), getPos().down()) && getWorld().getBlockEntity(getPos().down(), TileRailBase.class).getRailHeight() < 0.5;
		this.markDirty();
	}

	public void setItem(ItemStack stack, Player player) {
		this.item = stack.copy();
		RailSettings settings = RailSettings.from(item);

		if (settings.direction != TrackDirection.NONE) {
			this.placementInfo = this.placementInfo.withDirection(settings.direction);
		}

		if (!settings.isPreview) {
			if (this.getRailRenderInfo() != null && this.getRailRenderInfo().build(player, isAboveRails() ? getPos().down() : getPos())) {
				new PreviewRenderPacket(this.getWorld(), this.getPos()).sendToAll();
				if (isAboveRails()) {
					getWorld().breakBlock(this.getPos());
				}
				return;
			}
		}
		this.markDirty();
	}

	@Override
	public void load(TagCompound nbt) {
		info = null;
	}

    public void setCustomInfo(PlacementInfo info) {
        this.customInfo = info;
        if (customInfo != null) {
            RailSettings settings = RailSettings.from(item);
            float yaw = settings.type == TrackItems.TURN ? placementInfo.yaw / 2 : placementInfo.yaw;
            if(settings.type ==TrackItems.TURN
                    || settings.type == TrackItems.STRAIGHT
                    || settings.type == TrackItems.SLOPE){
                Vec3d placeOffset = new Vec3d(
                        customInfo.placementPosition.x - placementInfo.placementPosition.x,
                        0,
                        customInfo.placementPosition.z - placementInfo.placementPosition.z
                );
                Vec3d unit = new Vec3d(0, 0, 1).rotateYaw(yaw);
                int shadowLength = (int) Math.round(VecUtil.dotMultiply(placeOffset, unit));
                int length;

                switch (settings.type) {
                    case TURN:
                        double sin = Math.sin(Math.toRadians(settings.degrees / 2));
                        length = sin != 0d
                                 ? Math.max(0, (int) ((shadowLength / 2d) / sin)) + 1
                                 : 1;
                        break;
                    case STRAIGHT:
                    case SLOPE:
                    default:
                        length = Math.max(0, shadowLength) + 1;
                        break;
                }
                settings = settings.with(b -> b.length = length);
            }

            settings.write(item);
        }
        this.markDirty();
    }
    
    public void setPlacementInfo(PlacementInfo info) {
		this.placementInfo = info;
		this.markDirty();
	}
	
	@Override
	public boolean onClick(Player player, Player.Hand hand, Facing facing, Vec3d hit) {
		if (player.isCrouching()) {
			if (getWorld().isServer) {
				this.setPlacementInfo(new PlacementInfo(this.getItem(), player.getYawHead(), hit));
			}
			return false;
		} else if (!player.getHeldItem(hand).is(IRItems.ITEM_GOLDEN_SPIKE)) {
			GuiTypes.RAIL_PREVIEW.open(player, getPos());
			return true;
		}
		return false;
	}

	@Override
	public ItemStack onPick() {
		if (item == null) {
			return ItemStack.EMPTY;
		}
		return item;
	}

	@Override
	public IBoundingBox getBoundingBox() {
		// Won't be a lot of these in world, extra allocations are fine
		return IBoundingBox.ORIGIN.expand(new Vec3d(1, 0.125, 1));
	}

	@Override
	public IBoundingBox getRenderBoundingBox() {
		return IBoundingBox.INFINITE;
	}

	public RailInfo getRailRenderInfo() {
		if (getWorld() != null && item != null && (info == null || info.settings == null)) {
			info = new RailInfo(item, placementInfo, customInfo);
		}
		return info;
	}

	@Override
	public void markDirty() {
		super.markDirty();
        info = new RailInfo(item, placementInfo, customInfo);
        if (isMulti() && getWorld().isServer) {
			new PreviewRenderPacket(this).sendToAll();
		}
	}

	public boolean isMulti() {
		if (getRailRenderInfo().getBuilder(getWorld()) instanceof IIterableTrack) {
			return ((IIterableTrack)getRailRenderInfo().getBuilder(getWorld())).getSubBuilders() != null;
		}
		return false;
	}

	@Override
	public void update() {
		if (getWorld().isServer && isMulti()) {
			getWorld().keepLoaded(getPos());

			if (this.ticksAlive % 20 == 0) {
				new PreviewRenderPacket(this).sendToAll();
			}
			this.ticksAlive ++;
		}
	}

	@Override
	public boolean tryBreak(Player entityPlayer) {
		if (entityPlayer != null && entityPlayer.isCrouching()) {
			if (this.getRailRenderInfo() != null && this.getRailRenderInfo().build(entityPlayer, isAboveRails() ? getPos().down() : getPos())) {
				new PreviewRenderPacket(this.getWorld(), this.getPos()).sendToAll();
				return isAboveRails();
			}
			return false;
		}
		new PreviewRenderPacket(this.getWorld(), this.getPos()).sendToAll();
		return true;
	}

	public boolean isAboveRails() {
		return isAboveRails;
	}
}
