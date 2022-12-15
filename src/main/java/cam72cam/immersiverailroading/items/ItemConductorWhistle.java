package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.net.SoundPacket;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.item.CreativeTab;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.CustomItem;
import cam72cam.mod.item.Recipes;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.world.World;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ItemConductorWhistle extends CustomItem {
	private static HashMap<UUID, Integer> cooldown = new HashMap<>();

	public ItemConductorWhistle() {
		super(ImmersiveRailroading.MODID, "item_conductor_whistle");

		Fuzzy gold = Fuzzy.GOLD_INGOT;
		Recipes.shapedRecipe(this, 2,
				gold, gold, gold, gold, gold, gold);
	}

	@Override
	public int getStackSize() {
		return 1;
	}

	@Override
	public List<CreativeTab> getCreativeTabs() {
		return Collections.singletonList(ItemTabs.MAIN_TAB);
	}

	@Override
	public void onClickAir(Player player, World world, Player.Hand hand) {
		if (world.isServer && player.hasPermission(Permissions.CONDUCTOR)) {
			if (cooldown.containsKey(player.getUUID())) {
				int newtime = cooldown.get(player.getUUID());
				if (newtime < player.getTickCount() || newtime > player.getTickCount() + 40) {
					cooldown.remove(player.getUUID());
				} else {
					return;
				}
			}
			
			cooldown.put(player.getUUID(), player.getTickCount() + 40);
			
			SoundPacket packet = new SoundPacket(
					ImmersiveRailroading.MODID + ":sounds/conductor_whistle.ogg",
					player.getPosition(), Vec3d.ZERO,
					0.7f, (float) (Math.random() / 4 + 0.75), 
					(int) (Config.ConfigBalance.villagerConductorDistance * 1.2f), 
					1
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
					List<Entity> villagers = world.getEntities(x -> x.isVillager() && bb.intersects(x.getBounds()), Entity.class);
					for (Entity villager : villagers) {
						EntityCoupleableRollingStock closest = null;
						for (EntityCoupleableRollingStock car : closestToPlayer.getTrain()) {
							if (car.canFitPassenger(villager) && car.getDefinition().acceptsPassengers()) {
								if (closest == null || closest.getPosition().distanceTo(villager.getPosition()) > car.getPosition().distanceTo(villager.getPosition())) {
									closest = car;
								}
							}
						}
						if (closest != null) {
							closest.addPassenger(villager);
						}
					}
				} else {
					for (EntityCoupleableRollingStock car : closestToPlayer.getTrain()) {
						if (car.getPosition().distanceTo(player.getPosition()) < Config.ConfigBalance.villagerConductorDistance) {
							for (Entity passenger : car.getPassengers()) {
								if (passenger.isVillager()) {
									car.removePassenger(passenger);
								}
							}
						}
					}
				}
			}
		}
	}
}
