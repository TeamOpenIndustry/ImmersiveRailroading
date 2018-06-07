package cam72cam.immersiverailroading.items;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.net.SoundPacket;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;

public class ItemConductorWhistle extends Item {
	public static final String NAME = "item_conductor_whistle";
	private static HashMap<UUID, Integer> cooldown = new HashMap<UUID, Integer>();
	
	public ItemConductorWhistle() {
		super();
		setUnlocalizedName(ImmersiveRailroading.MODID + ":" + NAME);
		setRegistryName(new ResourceLocation(ImmersiveRailroading.MODID, NAME));
        this.setCreativeTab(ItemTabs.MAIN_TAB);
	}
	
	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		if (!world.isRemote) {
			if (cooldown.containsKey(player.getPersistentID())) {
				int newtime = cooldown.get(player.getPersistentID());
				if (newtime < ImmersiveRailroading.proxy.getTicks()) {
					cooldown.remove(player.getPersistentID());
				} else {
					return super.onItemRightClick(world, player, hand);
				}
			}
			
			cooldown.put(player.getPersistentID(), ImmersiveRailroading.proxy.getTicks() + 40);
			
			SoundPacket packet = new SoundPacket(
					new ResourceLocation(ImmersiveRailroading.MODID, "sounds/conductor_whistle.ogg").toString(),
					player.getPositionVector(), Vec3d.ZERO,
					0.7f, (float) (Math.random() / 4 + 0.75), 
					(int) (Config.ConfigBalance.villagerConductorDistance * 1.2f), 
					Gauge.from(Gauge.STANDARD)
			);
			ImmersiveRailroading.net.sendToAllAround(packet, new TargetPoint(player.dimension, player.posX, player.posY, player.posZ, Config.ConfigBalance.villagerConductorDistance * 1.2f));
			
			AxisAlignedBB bb = player.getEntityBoundingBox().grow(Config.ConfigBalance.villagerConductorDistance, 4, Config.ConfigBalance.villagerConductorDistance);
			List<EntityCoupleableRollingStock> carsNearby = world.getEntitiesWithinAABB(EntityCoupleableRollingStock.class, bb);
			EntityCoupleableRollingStock closestToPlayer = null;
			for (EntityCoupleableRollingStock car : carsNearby) {
				if (closestToPlayer == null) {
					closestToPlayer = car;
					continue;
				}
				if (closestToPlayer.getPositionVector().distanceTo(player.getPositionVector()) > car.getPositionVector().distanceTo(player.getPositionVector())) {
					closestToPlayer = car;
				}
			}
			
			if (closestToPlayer != null) {
				if (!player.isSneaking()) {
					List<EntityVillager> villagers = world.getEntitiesWithinAABB(EntityVillager.class, bb);
					for (EntityVillager villager : villagers) {
						EntityCoupleableRollingStock closest = null;
						for (EntityCoupleableRollingStock car : closestToPlayer.getTrain()) {
							if (car.canFitPassenger(villager) && car.getDefinition().acceptsPassengers()) {
								if (closest == null || closest.getPositionVector().distanceTo(villager.getPositionVector()) > car.getPositionVector().distanceTo(villager.getPositionVector())) {
									closest = car;
								}
							}
						}
						if (closest != null) {
							closest.addStaticPassenger(villager, villager.getPositionVector());
						}
					}
				} else {
					for (EntityCoupleableRollingStock car : closestToPlayer.getTrain()) {
						if (car.getPositionVector().distanceTo(player.getPositionVector()) < Config.ConfigBalance.villagerConductorDistance) {
							while (car.removeStaticPasssenger(player.getPositionVector(), true) != null) {
								//Unmounts all riding ents
							}
						}
					}
				}
			}
		}
		
		return super.onItemRightClick(world, player, hand);
	}
}
