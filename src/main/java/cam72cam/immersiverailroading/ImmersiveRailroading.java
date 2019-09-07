package cam72cam.immersiverailroading;

import cam72cam.immersiverailroading.Config.ConfigDebug;
import cam72cam.immersiverailroading.entity.*;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.multiblock.*;
import cam72cam.immersiverailroading.proxy.CommonProxy;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.render.StockRenderCache;
import cam72cam.immersiverailroading.render.block.RailBaseModel;
import cam72cam.immersiverailroading.render.entity.RenderOverride;
import cam72cam.immersiverailroading.render.item.*;
import cam72cam.immersiverailroading.render.multiblock.TileMultiblockRender;
import cam72cam.immersiverailroading.render.rail.RailPreviewRender;
import cam72cam.immersiverailroading.thirdparty.CompatLoader;
import cam72cam.immersiverailroading.tile.Rail;
import cam72cam.immersiverailroading.tile.RailGag;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.util.GLBoolTracker;
import cam72cam.immersiverailroading.util.IRFuzzy;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.EntityRegistry;
import cam72cam.mod.gui.GuiRegistry;
import cam72cam.mod.render.*;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.sound.Audio;
import cam72cam.mod.sound.ISound;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import org.lwjgl.opengl.GL11;
import paulscode.sound.SoundSystemConfig;

import java.io.IOException;

public class ImmersiveRailroading extends ModCore.Mod {
    public static final String MODID = "immersiverailroading";
	public static final String NAME = "ImmersiveRailroading";
    public static final String VERSION = "1.5.0";

	public static final int ENTITY_SYNC_DISTANCE = 512;
	public static ImmersiveRailroading instance;

	public static GuiRegistry GUI_REGISTRY = new GuiRegistry(ImmersiveRailroading.class);

    static {
    	ModCore.register(ImmersiveRailroading::new);

		EntityRegistry.register(ImmersiveRailroading.class, CarFreight::new, EntityRollingStock.settings, ImmersiveRailroading.ENTITY_SYNC_DISTANCE);
		EntityRegistry.register(ImmersiveRailroading.class, CarPassenger::new, EntityRollingStock.settings, ImmersiveRailroading.ENTITY_SYNC_DISTANCE);
		EntityRegistry.register(ImmersiveRailroading.class, CarTank::new, EntityRollingStock.settings, ImmersiveRailroading.ENTITY_SYNC_DISTANCE);
		EntityRegistry.register(ImmersiveRailroading.class, HandCar::new, EntityRollingStock.settings, ImmersiveRailroading.ENTITY_SYNC_DISTANCE);
		EntityRegistry.register(ImmersiveRailroading.class, LocomotiveDiesel::new, EntityRollingStock.settings, ImmersiveRailroading.ENTITY_SYNC_DISTANCE);
		EntityRegistry.register(ImmersiveRailroading.class, LocomotiveSteam::new, EntityRollingStock.settings, ImmersiveRailroading.ENTITY_SYNC_DISTANCE);
		EntityRegistry.register(ImmersiveRailroading.class, Tender::new, EntityRollingStock.settings, ImmersiveRailroading.ENTITY_SYNC_DISTANCE);

		MultiblockRegistry.register(SteamHammerMultiblock.NAME, new SteamHammerMultiblock());
		MultiblockRegistry.register(PlateRollerMultiblock.NAME, new PlateRollerMultiblock());
		MultiblockRegistry.register(RailRollerMultiblock.NAME, new RailRollerMultiblock());
		MultiblockRegistry.register(BoilerRollerMultiblock.NAME, new BoilerRollerMultiblock());
		MultiblockRegistry.register(CastingMultiblock.NAME, new CastingMultiblock());
	}

	public ImmersiveRailroading() {
    	instance = this;

		try {
			DefinitionManager.initDefinitions();
		} catch (IOException e) {
			throw new RuntimeException("Unable to load IR definitions", e);
		}

		Config.init();

		IRBlocks.register();
		IRItems.register();
	}

	@Override
	public String modID() {
		return MODID;
	}

	@Override
	public void setup()
	{
		proxy.init();

		IRFuzzy.applyFallbacks();
		CompatLoader.load();
	}

	@Override
	protected void initClient() {
		if (ConfigSound.overrideSoundChannels) {
			SoundSystemConfig.setNumberNormalChannels(Math.max(SoundSystemConfig.getNumberNormalChannels(), 300));
		}
		BlockRender.register(IRBlocks.BLOCK_RAIL, RailBaseModel::getModel, Rail.class);
		BlockRender.register(IRBlocks.BLOCK_RAIL_GAG, RailBaseModel::getModel, RailGag.class);
		BlockRender.register(IRBlocks.BLOCK_RAIL_PREVIEW, RailPreviewRender::render, TileRailPreview.class);
		BlockRender.register(IRBlocks.BLOCK_MULTIBLOCK, TileMultiblockRender::render, TileMultiblock.class);

		ItemRender.register(IRItems.ITEM_PLATE, PlateItemModel::getModel);
		ItemRender.register(IRItems.ITEM_AUGMENT, RailAugmentItemModel::getModel);
		ItemRender.register(IRItems.ITEM_RAIL, RailItemRender::getModel);
		ItemRender.register(IRItems.ITEM_CAST_RAIL, RailCastItemRender::getModel);
		ItemRender.register(IRItems.ITEM_TRACK_BLUEPRINT, TrackBlueprintItemModel::getModel);
		ItemRender.register(IRItems.ITEM_ROLLING_STOCK_COMPONENT, StockItemComponentModel::getModel);
		ItemRender.register(IRItems.ITEM_ROLLING_STOCK, StockItemModel::getModel, StockItemModel::getIcon);


		IEntityRender<EntityRollingStock> stockRender = (entity, partialTicks) -> {
			GLBoolTracker light = new GLBoolTracker(GL11.GL_LIGHTING, true);
			GLBoolTracker cull = new GLBoolTracker(GL11.GL_CULL_FACE, false);

			String def = entity.getDefinitionID();

			StockRenderCache.getRender(def).draw(entity, partialTicks);

			cull.restore();
			light.restore();
		};
		EntityRenderer.register(LocomotiveSteam.class, stockRender);
		EntityRenderer.register(LocomotiveDiesel.class, stockRender);
		EntityRenderer.register(CarPassenger.class, stockRender);
		EntityRenderer.register(CarFreight.class, stockRender);
		EntityRenderer.register(CarTank.class, stockRender);
		EntityRenderer.register(Tender.class, stockRender);
		EntityRenderer.register(HandCar.class, stockRender);


		GlobalRender.registerRender(partialTicks -> {
			RenderOverride.renderTiles(partialTicks);
			RenderOverride.renderParticles(partialTicks);
		});

		if (Loader.isModLoaded("igwmod")) {
			FMLInterModComms.sendMessage("igwmod", "cam72cam.immersiverailroading.thirdparty.IGWMod", "init");
		}
	}

	@Override
	protected void initServer() {
		for (EntityRollingStockDefinition def : DefinitionManager.getDefinitions()) {
			def.clearModel();
		}
	}

	public static ISound newSound(Identifier oggLocation, boolean repeats, float attenuationDistance, Gauge gauge) {
		return Audio.newSound(oggLocation, repeats, (float) (attenuationDistance * gauge.scale() * ConfigSound.soundDistanceScale), (float)Math.sqrt(Math.sqrt(gauge.scale())));
	}

	@SidedProxy(clientSide="cam72cam.immersiverailroading.proxy.ClientProxy", serverSide="cam72cam.immersiverailroading.proxy.ServerProxy")
	public static CommonProxy proxy;

    public static void debug(String msg, Object...params) {
    	if (instance.logger == null) {
    		System.out.println("DEBUG: " + String.format(msg, params));
    		return;
    	}
    	
    	if (ConfigDebug.debugLog) {
    		instance.logger.info(String.format(msg, params));
    	}
    }
    public static void info(String msg, Object...params) {
    	if (instance.logger == null) {
    		System.out.println("INFO: " + String.format(msg, params));
    		return;
    	}
    	
    	instance.logger.info(String.format(msg, params));
    }
    public static void warn(String msg, Object...params) {
    	if (instance.logger == null) {
    		System.out.println("WARN: " + String.format(msg, params));
    		return;
    	}
    	
    	instance.logger.warn(String.format(msg, params));
    }
    public static void error(String msg, Object...params) {
    	if (instance.logger == null) {
    		System.out.println("ERROR: " + String.format(msg, params));
    		return;
    	}
    	
    	instance.logger.error(String.format(msg, params));
    }
	public static void catching(Throwable ex) {
    	if (instance.logger == null) {
    		ex.printStackTrace();
    		return;
    	}
    	
		instance.logger.catching(ex);
	}
}
