package cam72cam.immersiverailroading.proxy;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.Config.ConfigDebug;
import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.*;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.sound.Audio;
import cam72cam.mod.sound.ISound;
import cam72cam.mod.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;

import java.util.List;

@EventBusSubscriber(modid = ImmersiveRailroading.MODID)
public abstract class CommonProxy {

	public void preInit() {
		Config.init();
    }

    public void init() {
    }

	@SubscribeEvent
	public static void onWorldTick(WorldTickEvent event) {
		if (event.phase != Phase.START) {
			return;
		}



		if (!event.world.isRemote) {
			World world = World.get(event.world);
			// We do this here as to let all the entities do their onTick first.  Otherwise some might be one onTick ahead
			// if we did this in the onUpdate method
			List<EntityCoupleableRollingStock> entities = world.getEntities(EntityCoupleableRollingStock.class);

			// Try locomotives first
			for (EntityCoupleableRollingStock stock : entities) {
				if (stock instanceof Locomotive) {
					stock = stock.findByUUID(stock.getUUID());
					stock.tickPosRemainingCheck();
				}
			}
			// Try rest
			for (EntityCoupleableRollingStock stock : entities) {
				stock = stock.findByUUID(stock.getUUID());
				stock.tickPosRemainingCheck();
			}

			try {
				Thread.sleep(ConfigDebug.lagServer);
			} catch (InterruptedException e) {
				ImmersiveRailroading.catching(e);
			}
		}
	}


	public ISound newSound(Identifier oggLocation, boolean repeats, float attenuationDistance, Gauge gauge) {
		return Audio.newSound(oggLocation, repeats, (float) (attenuationDistance * gauge.scale() * ConfigSound.soundDistanceScale), (float)Math.sqrt(Math.sqrt(gauge.scale())));
	}

	public int getRenderDistance() {
		return 8;
	}

    public abstract void addPreview(int dimension, TileRailPreview preview);
}
