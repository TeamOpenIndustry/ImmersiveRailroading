package cam72cam.immersiverailroading.proxy;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.Config.ConfigDebug;
import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.*;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.multiblock.*;
import cam72cam.immersiverailroading.net.*;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.thirdparty.CompatLoader;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.util.IRFuzzy;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Registry;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.net.Packet;
import cam72cam.mod.net.PacketDirection;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.sound.Audio;
import cam72cam.mod.sound.ISound;
import cam72cam.mod.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@EventBusSubscriber(modid = ImmersiveRailroading.MODID)
public abstract class CommonProxy {
	protected static List<Supplier<Entity>> entityClasses = new ArrayList<>();
    static {
    	entityClasses.add(LocomotiveSteam::new);
    	entityClasses.add(LocomotiveDiesel::new);
    	entityClasses.add(CarPassenger::new);
    	entityClasses.add(CarFreight::new);
    	entityClasses.add(CarTank::new);
    	entityClasses.add(Tender::new);
    	entityClasses.add(HandCar::new);
    }

	public cam72cam.mod.gui.container.Registry GUI_REGISTRY;

	public void preInit(FMLPreInitializationEvent event) throws IOException {
    	DefinitionManager.initDefinitions();
    	Config.init();

    }

    public void init(FMLInitializationEvent event) {
    	Packet.register(MRSSyncPacket::new, PacketDirection.ServerToClient);
    	Packet.register(KeyPressPacket::new, PacketDirection.ClientToServer);
    	Packet.register(MousePressPacket::new, PacketDirection.ClientToServer);
    	Packet.register(ItemRailUpdatePacket::new, PacketDirection.ClientToServer);
    	Packet.register(BuildableStockSyncPacket::new, PacketDirection.ServerToClient);
    	Packet.register(MultiblockSelectCraftPacket::new, PacketDirection.ClientToServer);
    	Packet.register(SoundPacket::new, PacketDirection.ServerToClient);
    	Packet.register(PaintSyncPacket::new, PacketDirection.ServerToClient);
        Packet.register(PreviewRenderPacket::new, PacketDirection.ServerToClient);

        this.GUI_REGISTRY = new cam72cam.mod.gui.container.Registry(ImmersiveRailroading.instance);

    	CompatLoader.load();

    	MultiblockRegistry.register(SteamHammerMultiblock.NAME, new SteamHammerMultiblock());
    	MultiblockRegistry.register(PlateRollerMultiblock.NAME, new PlateRollerMultiblock());
    	MultiblockRegistry.register(RailRollerMultiblock.NAME, new RailRollerMultiblock());
    	MultiblockRegistry.register(BoilerRollerMultiblock.NAME, new BoilerRollerMultiblock());
    	MultiblockRegistry.register(CastingMultiblock.NAME, new CastingMultiblock());

		IRFuzzy.IR_RAIL_BED.addAll(Fuzzy.BRICK_BLOCK);
		IRFuzzy.IR_RAIL_BED.addAll(Fuzzy.COBBLESTONE);
		IRFuzzy.IR_RAIL_BED.addAll(Fuzzy.CONCRETE);
		IRFuzzy.IR_RAIL_BED.addAll(Fuzzy.DIRT);
		IRFuzzy.IR_RAIL_BED.addAll(Fuzzy.GRAVEL_BLOCK);
		IRFuzzy.IR_RAIL_BED.addAll(Fuzzy.HARDENED_CLAY);
		IRFuzzy.IR_RAIL_BED.addAll(Fuzzy.LOG_WOOD);
		IRFuzzy.IR_RAIL_BED.addAll(Fuzzy.NETHER_BRICK);
		IRFuzzy.IR_RAIL_BED.addAll(Fuzzy.WOOD_PLANK);

		IRFuzzy.applyFallbacks();
    }


	public void serverStarting(FMLServerStartingEvent event) {
		event.registerServerCommand(new IRCommand());
	}

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
    	for (Supplier<Entity> type : entityClasses) {
			Registry.register(ImmersiveRailroading.MODID, type, EntityRollingStock.settings, ImmersiveRailroading.instance, ImmersiveRailroading.ENTITY_SYNC_DISTANCE);
    	}
    }

	@SubscribeEvent
	public static void onWorldTick(WorldTickEvent event) {
		if (event.phase != Phase.START) {
			return;
		}



		if (!event.world.isRemote) {
			ChunkManager.handleWorldTick(event.world);
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

	public abstract int getTicks();


	public ISound newSound(Identifier oggLocation, boolean repeats, float attenuationDistance, Gauge gauge) {
		return Audio.newSound(oggLocation, repeats, (float) (attenuationDistance * gauge.scale() * ConfigSound.soundDistanceScale), (float)Math.sqrt(Math.sqrt(gauge.scale())));
	}

	public int getRenderDistance() {
		return 8;
	}

    public abstract void addPreview(int dimension, TileRailPreview preview);
}
