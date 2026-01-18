package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.net.PreviewRenderPacket;
import cam72cam.immersiverailroading.track.IIterableTrack;
import cam72cam.immersiverailroading.util.*;
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
	private boolean isCustomDirty = false;
	@TagField
	private boolean isAboveRails = false;

	public ItemStack getItem() {
		return this.item;
	}
	
	public void setup(ItemStack stack, PlacementInfo info) {
		this.item = stack.copy();//multiSwitchInfo corrected here
		this.placementInfo = info;

		MultiSwitchInfo multiSwitchInfo = MultiSwitchInfo.writePlacement(MultiSwitchInfo.from(item),placementInfo);
		multiSwitchInfo.write(item);

		this.isAboveRails = BlockUtil.isIRRail(getWorld(), getPos().down()) && getWorld().getBlockEntity(getPos().down(), TileRailBase.class).getRailHeight() < 0.5;
		this.markDirty();
	}

	public void setItem(ItemStack stack, Player player) {
		this.item = stack.copy();//multiSwitchInfo corrected here

		MultiSwitchInfo multiSwitchInfo = MultiSwitchInfo.writePlacement(MultiSwitchInfo.from(item),placementInfo);
		multiSwitchInfo.write(item);

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
			RailSettings settings;

			boolean replaceType = false;
			Integer selectedOrder = MultiSwitchInfo.getSelectedFrom(item);
			if(selectedOrder==0){
				settings = RailSettings.from(item);
				if(settings.type == TrackItems.MULTISWITCH) {
					MultiSwitchInfo multiSwitchInfo = MultiSwitchInfo.from(item);
					settings = settings.with(mutable -> mutable.type = multiSwitchInfo.realShapeType);
					replaceType = true;
				}
			}else {
				MultiSwitchInfo multiSwitchInfo = MultiSwitchInfo.from(item);
				if(multiSwitchInfo!=null&&multiSwitchInfo.wayList!=null&&selectedOrder<=multiSwitchInfo.wayList.size()){
					settings = multiSwitchInfo.wayList.get(selectedOrder-1).settings;
				}else {
					ImmersiveRailroading.warn("invalid multiSwitchInfo:"+multiSwitchInfo+",or selectedOrder:"+selectedOrder);
					return;
				}
			}

			if(settings.type ==TrackItems.TURN
				|| settings.type == TrackItems.STRAIGHT
				|| settings.type == TrackItems.SLOPE){
				Vec3d placeOffset = new Vec3d(
						customInfo.placementPosition.x - placementInfo.placementPosition.x,
						0,
						customInfo.placementPosition.z - placementInfo.placementPosition.z
				);
				float yaw = settings.type == TrackItems.TURN
							? placementInfo.yaw + ((settings.direction == TrackDirection.LEFT ? -1 : 1) * (settings.degrees / 2)) //Calculate arc direction for turn
							: placementInfo.yaw; //Simply use its yaw
				Vec3d unit = new Vec3d(0, 0, 1).rotateYaw(yaw);
				//TODO Replace me with UMC method once #170 is merged
                int shadowLength = (int) Math.round(placeOffset.x * unit.x
													+ placeOffset.y * unit.y
													+ placeOffset.z * unit.z);
				int length;

				switch (settings.type) {
					case TURN:
						//Transform it back to radius
						double sin = Math.sin(Math.toRadians(settings.degrees / 2));
						length = sin != 0d
								 ? Math.max(1, (int) ((shadowLength / 2d) / sin)) + 1
								 : 2;
						break;
					case STRAIGHT:
					case SLOPE:
					default:
						length = Math.max(0, shadowLength) + 1;
						break;
				}
				settings = settings.with(b -> b.length = length);
			}

			if(selectedOrder==0){
				if(replaceType) {
					settings = settings.with(mutable -> mutable.type = TrackItems.MULTISWITCH);
					MultiSwitchInfo multiSwitchInfo = MultiSwitchInfo.from(item);
					multiSwitchInfo = multiSwitchInfo.with(mutable -> mutable.defaultCustom = customInfo);//use defaultCustom when it is MultiSwitchInfo!
					multiSwitchInfo.write(item);
					isCustomDirty = true;
				}
				settings.write(item);
			}else {
				MultiSwitchInfo multiSwitchInfo = MultiSwitchInfo.from(item);
				if(multiSwitchInfo!=null&&multiSwitchInfo.wayList!=null&&selectedOrder<=multiSwitchInfo.wayList.size()){
					SingleWayInfo singleWayInfo = multiSwitchInfo.wayList.get(selectedOrder-1);
					RailSettings finalSettings = settings;
					singleWayInfo = singleWayInfo.with(mutable -> mutable.settings = finalSettings);
					multiSwitchInfo.wayList.set(selectedOrder-1,singleWayInfo);
					multiSwitchInfo.write(item);
					isCustomDirty = true;
				}
			}
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
		} else if (getWorld().isClient && !player.getHeldItem(hand).is(IRItems.ITEM_GOLDEN_SPIKE)) {
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

	public RailInfo getRailRenderInfo() {//not only for render, but also for build
		if (getWorld() != null && item != null && (info == null || info.settings == null)) {
			PlacementInfo custom;
			if(RailSettings.from(item).type==TrackItems.MULTISWITCH) {
				PlacementInfo defaultCustom = MultiSwitchInfo.from(item).defaultCustom;
				custom = defaultCustom == null ? null : defaultCustom.withFloorYoffset(RailSettings.from(item).customOffset);
			}else {
				custom = customInfo;//non-MultiSwitch types
			}

			info = new RailInfo(item, placementInfo, custom, MultiSwitchInfo.from(item));

			//write wayList placement & custom offset into info
			writePosOffset();
		}
		return info;//build will go here
	}

	//TODO:move custom of normal types in multiSwitchInfo/settings?
	private void writePosOffset() {//write wayList placement & custom offset into info
		MultiSwitchInfo multiSwitchInfo = MultiSwitchInfo.from(item);
		if(multiSwitchInfo!=null && multiSwitchInfo.wayList!=null) {
			for(int i=0; i<multiSwitchInfo.wayList.size(); i++) {
				SingleWayInfo singleWayInfo = multiSwitchInfo.wayList.get(i);
				SingleWayInfo finalSingleWayInfo = singleWayInfo;

				singleWayInfo = singleWayInfo.with(mutable -> {
					mutable.placementInfo = mutable.placementInfo.withFloorYoffset(RailSettings.from(item).placementOffset);
					if(mutable.customInfo != null)mutable.customInfo = mutable.customInfo.withFloorYoffset(finalSingleWayInfo.settings.customOffset);
				});
				multiSwitchInfo.wayList.set(i,singleWayInfo);
			}
		}
		info = info.with(mutable -> mutable.multiSwitchInfo = multiSwitchInfo);
	}

	@Override
	public void markDirty() {
		super.markDirty();

		//selectedOrder==0:non-multiSwitch or straightBuilder(multiSwitch) of multiSwitch,selectedOrder>0:turnBuilder(multiSwitch)
		int selectedOrder = MultiSwitchInfo.getSelectedFrom(item);

		//update offset placement offset from item <= packet <= Gui
		placementInfo = placementInfo.withFloorYoffset(RailSettings.from(item).placementOffset);

        info = new RailInfo(item, placementInfo, customInfo, MultiSwitchInfo.from(item));

		//update custom if it is multiSwitch
		if(selectedOrder>0 && isCustomDirty) {
			MultiSwitchInfo multiSwitchInfo = MultiSwitchInfo.writeCustom(MultiSwitchInfo.from(item),customInfo,selectedOrder);
            info = info.with(mutable -> {
				mutable.multiSwitchInfo = multiSwitchInfo;
			});
			multiSwitchInfo.write(item);//both item and info are updated
		}
		if(info.settings.type==TrackItems.MULTISWITCH) {
			MultiSwitchInfo multiSwitchInfo = MultiSwitchInfo.from(item);

			PlacementInfo custom = multiSwitchInfo.defaultCustom;
			if(custom!=null)custom = custom.withFloorYoffset(RailSettings.from(item).customOffset);
			PlacementInfo finalCustom = custom;

			info = info.with(mutable -> {
				mutable.customInfo = finalCustom;//only need to overwrite info.customInfo, no update in item
			});
		}
		isCustomDirty = false;

		//update custom offset from item <= packet <= Gui if selectedOrder>0
		if(selectedOrder==0) {//if selectedOrder == 0
			customInfo = customInfo == null ? null : customInfo.withFloorYoffset(RailSettings.from(item).customOffset);
			info = info.with(mutable -> mutable.customInfo = customInfo);
		}

		//write wayList placement & custom offset into info
		writePosOffset();

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
