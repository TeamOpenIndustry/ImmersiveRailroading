package cam72cam.immersiverailroading.util;

import java.util.List;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.items.nbt.ItemTextureVariant;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.physics.MovementSimulator;
import cam72cam.immersiverailroading.physics.TickPos;
import cam72cam.immersiverailroading.Config.ConfigDebug;
import cam72cam.immersiverailroading.entity.EntityBuildableRollingStock;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock.CouplerType;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.mod.entity.Player;
import cam72cam.mod.World;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Hand;
import net.minecraft.util.EnumFacing;
import trackapi.lib.ITrack;
import trackapi.lib.Util;

public class SpawnUtil {
	public static ClickResult placeStock(Player player, Hand hand, World worldIn, Vec3i pos, EntityRollingStockDefinition def, List<ItemComponentType> list) {
		ITrack initte = Util.getTileEntity(worldIn.internal, new Vec3d(pos).add(0, 0.7, 0).internal, true);
		if (initte == null) {
			return ClickResult.REJECTED;
		}
		double trackGauge = initte.getTrackGauge();
		Gauge gauge = Gauge.from(trackGauge);
		
		
		if (!player.isCreative() && gauge != ItemGauge.get(player.getHeldItem(hand))) {
			player.sendMessage(ChatText.STOCK_WRONG_GAUGE.getMessage());
			return ClickResult.REJECTED;
		}
		
		double offset = def.getCouplerPosition(CouplerType.BACK, gauge) - ConfigDebug.couplerRange;
		float yaw = player.getYawHead();
		TickPos tp = new MovementSimulator(worldIn.internal, new TickPos(0, Speed.ZERO, new Vec3d(pos).add(0, 0.7, 0).add(0.5, 0, 0.5).internal, yaw, yaw, yaw, 0, false), def.getBogeyFront(gauge), def.getBogeyRear(gauge), gauge.value()).nextPosition(offset);
		
		if (!tp.isOffTrack) {
			if (worldIn.isServer) {
				String texture = ItemTextureVariant.get(player.getHeldItem(hand));
				EntityRollingStock stock = def.spawn(worldIn.internal, tp.position, EnumFacing.fromAngle(player.getYawHead()), gauge, texture);
				
				if (stock instanceof EntityBuildableRollingStock) {
					((EntityBuildableRollingStock)stock).setComponents(list);
				}
				
				if (stock instanceof EntityMoveableRollingStock) {
					// snap to track
					EntityMoveableRollingStock mrs = (EntityMoveableRollingStock)stock;
					tp.speed = Speed.ZERO;
					mrs.initPositions(tp);
				}
			}
			if (!player.isCreative()) {
				ItemStack stack = player.getHeldItem(hand);
				stack.setCount(stack.getCount()-1);
				player.setHeldItem(hand, stack);
			}
			return ClickResult.ACCEPTED;
		}
		return ClickResult.REJECTED;
	}
}
