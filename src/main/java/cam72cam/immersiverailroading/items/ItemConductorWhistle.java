package cam72cam.immersiverailroading.items;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.net.SoundPacket;
import cam72cam.mod.entity.boundingbox.BoundingBox;
import cam72cam.mod.world.World;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.ItemBase;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import net.minecraft.entity.passive.EntityVillager;

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
			packet.sendToAllAround(world, player.getPosition(), Config.ConfigBalance.villagerConductorDistance * 1.2f);

			IBoundingBox bb = player.getBounds().grow(new Vec3d(Config.ConfigBalance.villagerConductorDistance, 4, Config.ConfigBalance.villagerConductorDistance));
			List<EntityCoupleableRollingStock> carsNearby = world.getEntities((EntityCoupleableRollingStock stock) -> bb.intersects(stock.getBounds()), EntityCoupleableRollingStock.class);
			EntityCoupleableRollingStock closestToPlayer = null;
			for (EntityCoupleableRollingStock car : carsNearby) {
				if (closestToPlayer == null) {
					closestToPlayer = car;
					continue;
				}
				if (closestToPlayer.getPosition().distanceTo(player.getPosition()) > car.getPosition().distanceTo(player.getPosition())) {
					closestToPlayer = car;
				}
			}
			
			if (closestToPlayer != null) {
				if (!player.isCrouching()) {
					List<EntityVillager> villagers = world.internal.getEntitiesWithinAABB(EntityVillager.class, new BoundingBox(bb));
					for (EntityVillager villager : villagers) {
						EntityCoupleableRollingStock closest = null;
						for (EntityCoupleableRollingStock car : closestToPlayer.getTrain()) {
							if (car.canFitPassenger(new Entity(villager)) && car.getDefinition().acceptsPassengers()) {
								if (closest == null || closest.getPosition().internal.distanceTo(villager.getPositionVector()) > car.getPosition().internal.distanceTo(villager.getPositionVector())) {
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
						if (car.getPosition().distanceTo(player.getPosition()) < Config.ConfigBalance.villagerConductorDistance) {
							while (car.getPassengerCount() != 0) {
                                car.removePassenger((ModdedEntity.StaticPassenger s) -> s.isVillager);
							}
						}
					}
				}
			}
		}
		
		return ClickResult.PASS;
	}
}
