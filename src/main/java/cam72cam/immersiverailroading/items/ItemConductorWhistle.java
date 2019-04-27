package cam72cam.immersiverailroading.items;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.net.SoundPacket;
import cam72cam.mod.World;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.ItemBase;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.network.NetworkRegistry;

public class ItemConductorWhistle extends ItemBase {
	private static HashMap<UUID, Integer> cooldown = new HashMap<>();
	
	public ItemConductorWhistle() {
		super(ImmersiveRailroading.MODID, "item_conductor_whistle", 1, ItemTabs.MAIN_TAB);
	}
	
	@Override
	public ClickResult onClickBlock(Player player, World world, Vec3i pos, Hand hand, Facing facing, Vec3d hit) {
		if (world.isServer) {
			if (cooldown.containsKey(player.getUUID())) {
				int newtime = cooldown.get(player.getUUID());
				if (newtime < ImmersiveRailroading.proxy.getTicks()) {
					cooldown.remove(player.getUUID());
				} else {
					return ClickResult.PASS;
				}
			}
			
			cooldown.put(player.getUUID(), ImmersiveRailroading.proxy.getTicks() + 40);
			
			SoundPacket packet = new SoundPacket(
					ImmersiveRailroading.MODID + ":sounds/conductor_whistle.ogg",
					player.getPosition(), Vec3d.ZERO,
					0.7f, (float) (Math.random() / 4 + 0.75), 
					(int) (Config.ConfigBalance.villagerConductorDistance * 1.2f), 
					Gauge.from(Gauge.STANDARD)
			);
			ImmersiveRailroading.net.sendToAllAround(packet, new NetworkRegistry.TargetPoint(player.internal.dimension, player.internal.posX, player.internal.posY, player.internal.posZ, Config.ConfigBalance.villagerConductorDistance * 1.2f));
			
			AxisAlignedBB bb = player.internal.getEntityBoundingBox().grow(Config.ConfigBalance.villagerConductorDistance, 4, Config.ConfigBalance.villagerConductorDistance);
			List<EntityCoupleableRollingStock> carsNearby = world.internal.getEntitiesWithinAABB(EntityCoupleableRollingStock.class, bb);
			EntityCoupleableRollingStock closestToPlayer = null;
			for (EntityCoupleableRollingStock car : carsNearby) {
				if (closestToPlayer == null) {
					closestToPlayer = car;
					continue;
				}
				if (closestToPlayer.getPositionVector().distanceTo(player.getPosition().internal) > car.getPositionVector().distanceTo(player.getPosition().internal)) {
					closestToPlayer = car;
				}
			}
			
			if (closestToPlayer != null) {
				if (!player.isCrouching()) {
					List<EntityVillager> villagers = world.internal.getEntitiesWithinAABB(EntityVillager.class, bb);
					for (EntityVillager villager : villagers) {
						EntityCoupleableRollingStock closest = null;
						for (EntityCoupleableRollingStock car : closestToPlayer.getTrain()) {
							if (car.canFitPassenger(new Entity(villager)) && car.getDefinition().acceptsPassengers()) {
								if (closest == null || closest.getPositionVector().distanceTo(villager.getPositionVector()) > car.getPositionVector().distanceTo(villager.getPositionVector())) {
									closest = car;
								}
							}
						}
						if (closest != null) {
							closest.addPassenger(new Entity(villager));
						}
					}
				} else {
					for (EntityCoupleableRollingStock car : closestToPlayer.getTrain()) {
						if (car.getPositionVector().distanceTo(player.getPosition().internal) < Config.ConfigBalance.villagerConductorDistance) {
							while (car.getPassengerCount() != 0) {
                                car.dismountRidingEntity();
							}
						}
					}
				}
			}
		}
		
		return ClickResult.PASS;
	}
}
