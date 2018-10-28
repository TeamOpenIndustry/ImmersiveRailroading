package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.ItemTrackBlueprint;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.library.TrackPositionType;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.util.BufferUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemRailUpdatePacket implements IMessage {
	private int slot;
	private int length;
	private int quarters;
	private TrackItems type;
	private double gauge;
	private TrackPositionType posType;
	public TrackDirection direction;
	private ItemStack bedStack;
	private ItemStack railBedFill;
	private boolean isPreview;
	private boolean isGradeCrossing;
	private BlockPos tilePreviewPos;
	
	public ItemRailUpdatePacket() {
		// For Reflection
	}
	
	@SideOnly(Side.CLIENT)
	public ItemRailUpdatePacket(int slot, int length, int quarters, TrackItems type, double gauge, TrackPositionType posType, TrackDirection direction, ItemStack bedStack, ItemStack railBedFill, boolean isPreview, boolean isGradeCrossing) {
		this.slot = slot;
		this.length = length;
		this.quarters = quarters;
		this.type = type;
		this.posType = posType;
		this.bedStack = bedStack;
		this.railBedFill = railBedFill;
		this.isPreview = isPreview;
		this.isGradeCrossing = isGradeCrossing;
		this.gauge = gauge;
		this.direction = direction;
	}

	public ItemRailUpdatePacket(BlockPos tilePreviewPos, int length, int quarters, TrackItems type, double gauge, TrackPositionType posType, TrackDirection direction, ItemStack bedStack, ItemStack railBedFill, boolean isPreview, boolean isGradeCrossing) {
		this.tilePreviewPos = tilePreviewPos;
		this.length = length;
		this.quarters = quarters;
		this.type = type;
		this.posType = posType;
		this.bedStack = bedStack;
		this.railBedFill = railBedFill;
		this.isPreview = isPreview;
		this.isGradeCrossing = isGradeCrossing;
		this.gauge = gauge;
		this.direction = direction;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.length = buf.readInt();
		if (buf.readBoolean()) {
			this.slot = buf.readInt();
		} else {
			this.tilePreviewPos = new BlockPos(BufferUtil.readVec3i(buf));
		}
		this.quarters = buf.readInt();
		this.type = TrackItems.values()[buf.readInt()];
		this.gauge = buf.readDouble();
		this.posType = TrackPositionType.values()[buf.readInt()];
		this.direction = TrackDirection.values()[buf.readInt()];
		this.bedStack = ByteBufUtils.readItemStack(buf);
		this.railBedFill = ByteBufUtils.readItemStack(buf);
		this.isPreview = buf.readBoolean();
		this.isGradeCrossing = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(length);
		buf.writeBoolean(tilePreviewPos == null);
		if (tilePreviewPos == null) {
			buf.writeInt(slot);
		} else {
			BufferUtil.writeVec3i(buf, tilePreviewPos);
		}
		buf.writeInt(quarters);
		buf.writeInt(type.ordinal());
		buf.writeDouble(gauge);
		buf.writeInt(posType.ordinal());
		buf.writeInt(direction.ordinal());
		ByteBufUtils.writeItemStack(buf, bedStack);
		ByteBufUtils.writeItemStack(buf, railBedFill);
		buf.writeBoolean(isPreview);
		buf.writeBoolean(isGradeCrossing);
	}
	
	public static class Handler implements IMessageHandler<ItemRailUpdatePacket, IMessage> {
		@Override
		public IMessage onMessage(ItemRailUpdatePacket message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> handle(message, ctx));
			return null;
		}

		private void handle(ItemRailUpdatePacket message, MessageContext ctx) {
			ItemStack stack;
			TileRailPreview te = null;
			if (message.tilePreviewPos == null) {
				stack = ctx.getServerHandler().player.inventory.getStackInSlot(message.slot);
			} else {
				te = TileRailPreview.get(ctx.getServerHandler().player.world, message.tilePreviewPos);
				if (te == null) {
					ImmersiveRailroading.warn("Got invalid item rail update packet at %s", message.tilePreviewPos);
					return;
				}
				stack = te.getItem();
			}
			ItemTrackBlueprint.setType(stack, message.type);
			ItemGauge.set(stack, Gauge.from(message.gauge));
			ItemTrackBlueprint.setLength(stack, message.length);
			ItemTrackBlueprint.setQuarters(stack, message.quarters);
			ItemTrackBlueprint.setPosType(stack, message.posType);
			ItemTrackBlueprint.setDirection(stack, message.direction);
			ItemTrackBlueprint.setBed(stack, message.bedStack);
			ItemTrackBlueprint.setBedFill(stack, message.railBedFill);
			ItemTrackBlueprint.setPreview(stack, message.isPreview);
			ItemTrackBlueprint.setGradeCrossing(stack, message.isGradeCrossing);
			if (message.tilePreviewPos == null) {
				ctx.getServerHandler().player.inventory.setInventorySlotContents(message.slot, stack);
			} else {
				te.setItem(stack);
			}
		}
	}
}
