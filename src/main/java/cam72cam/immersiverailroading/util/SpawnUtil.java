package cam72cam.immersiverailroading.util;

import java.util.List;

import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.physics.SimulationState;
import cam72cam.immersiverailroading.items.ItemRollingStock;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.Config.ConfigDebug;
import cam72cam.immersiverailroading.entity.EntityBuildableRollingStock;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock.CouplerType;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.mod.entity.Player;
import cam72cam.immersiverailroading.thirdparty.trackapi.ITrack;
import cam72cam.mod.world.World;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;

public class SpawnUtil {
	public static ClickResult placeStock(Player player, Player.Hand hand, World worldIn, Vec3i pos, EntityRollingStockDefinition def, List<ItemComponentType> list) {
		ItemRollingStock.Data data = new ItemRollingStock.Data(player.getHeldItem(hand));

		ITrack initte = ITrack.get(worldIn, new Vec3d(pos).add(0, 0.7, 0), true);
		if (initte == null) {
			return ClickResult.REJECTED;
		}
		double trackGauge = initte.getTrackGauge();
		Gauge gauge = Gauge.from(trackGauge);
		
		
		if (!player.isCreative() && gauge != data.gauge) {
			player.sendMessage(ChatText.STOCK_WRONG_GAUGE.getMessage());
			return ClickResult.REJECTED;
		}
		
		double offset = def.getCouplerPosition(CouplerType.BACK, gauge) - ConfigDebug.couplerRange;
		float yaw = player.getYawHead();

		if (worldIn.isServer) {
			EntityRollingStock stock = def.spawn(worldIn, new Vec3d(pos), yaw, gauge, data.texture);

			if (stock instanceof EntityCoupleableRollingStock) {
				EntityCoupleableRollingStock ecrs = (EntityCoupleableRollingStock) stock;
				outer:
				for (int i = 0; i <= 90; i+= 5) {
					for (int j = -1; j <= 1; j += 2) {
						ecrs.setRotationYaw(yaw + i * j);
						SimulationState state = new SimulationState(ecrs).next(offset);
						if (state.position.distanceTo(ecrs.getPosition()) > offset/2) {
							ecrs.setPosition(state.position);
							ecrs.setRotationYaw(state.yaw);
							ecrs.setRotationPitch(state.pitch);
							ecrs.setFrontYaw(state.yawFront);
							ecrs.setRearYaw(state.yawRear);
							break outer;
						}
					}
				}
			}

			if (stock instanceof EntityBuildableRollingStock) {
				((EntityBuildableRollingStock)stock).setComponents(list);
			}


			worldIn.spawnEntity(stock);
		}
		if (!player.isCreative()) {
			ItemStack stack = player.getHeldItem(hand);
			stack.setCount(stack.getCount()-1);
			player.setHeldItem(hand, stack);
		}
		return ClickResult.ACCEPTED;
	}
}
