package cam72cam.immersiverailroading.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@EventBusSubscriber
public class VanillaEntityAugmentor {
	@SubscribeEvent
	public void onEntityJoinWorld(LivingSpawnEvent event) {
		Entity entity = event.getEntity();
		//(entity instanceof EntityChicken || entity instanceof EntityPig || entity instanceof EntityCow || entity instanceof EntityMooshroom || entity instanceof EntityRabbit || entity instanceof EntitySheep || entity instanceof EntityOcelot || entity instanceof EntityWolf || entity instanceof EntityLlama || entity instanceof EntityHorse || entity instanceof EntityDonkey || entity instanceof EntityMule || entity instanceof EntityVillager || entity instanceof EntityZombie || entity instanceof EntitySkeleton || entity instanceof EntitySpider || entity instanceof EntityWitherSkeleton || entity instanceof EntityHusk || entity instanceof EntityZombieVillager || entity instanceof EntityCreeper || entity instanceof EntitySlime || entity instanceof EntityEnderman || entity instanceof EntityWitch)
		if (entity instanceof EntityLiving) {
			//((EntityCreature) entity).tasks.addTask(0, new EntityAIAvoidTrain((EntityCreature)entity, EntityRollingStock.class, 100, 0.8, 0.8));
			((EntityLiving)entity).tasks.addTask(0, new EntityAIAvoidEntity((EntityCreature) entity, EntityPig.class, 100, 1, 1));
		}
	}
}
