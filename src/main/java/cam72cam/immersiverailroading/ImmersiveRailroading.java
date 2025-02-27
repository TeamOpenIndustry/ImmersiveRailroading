package cam72cam.immersiverailroading;

import cam72cam.immersiverailroading.entity.*;
import cam72cam.immersiverailroading.entity.physics.chrono.ServerChronoState;
import cam72cam.immersiverailroading.gui.overlay.GuiBuilder;
import cam72cam.immersiverailroading.items.ItemPaintBrush;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.library.Particles;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.model.StockModel;
import cam72cam.immersiverailroading.multiblock.*;
import cam72cam.immersiverailroading.net.*;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.render.SmokeParticle;
import cam72cam.immersiverailroading.render.block.RailBaseModel;
import cam72cam.immersiverailroading.render.item.*;
import cam72cam.immersiverailroading.render.multiblock.MBBlueprintRender;
import cam72cam.immersiverailroading.render.multiblock.TileMultiblockRender;
import cam72cam.immersiverailroading.render.rail.RailPreviewRender;
import cam72cam.immersiverailroading.thirdparty.CompatLoader;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailGag;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.util.IRFuzzy;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.ModCore;
import cam72cam.mod.ModEvent;
import cam72cam.mod.config.ConfigFile;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.EntityRegistry;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.input.Keyboard;
import cam72cam.mod.input.Keyboard.KeyCode;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.net.Packet;
import cam72cam.mod.net.PacketDirection;
import cam72cam.mod.render.*;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.sound.Audio;
import cam72cam.mod.text.Command;

import java.util.function.Function;

public class ImmersiveRailroading extends ModCore.Mod {
    public static final String MODID = "immersiverailroading";

	public static final int ENTITY_SYNC_DISTANCE = 512;
	private static ImmersiveRailroading instance;

	public ImmersiveRailroading() {
		instance = this;
	}

	@Override
	public String modID() {
		return MODID;
	}

	@Override
	public void commonEvent(ModEvent event) {
		CompatLoader.common(event);

		switch (event) {
			case CONSTRUCT:
				EntityRegistry.register(ImmersiveRailroading.instance, CarFreight::new, ImmersiveRailroading.ENTITY_SYNC_DISTANCE);
				EntityRegistry.register(ImmersiveRailroading.instance, CarPassenger::new, ImmersiveRailroading.ENTITY_SYNC_DISTANCE);
				EntityRegistry.register(ImmersiveRailroading.instance, CarTank::new, ImmersiveRailroading.ENTITY_SYNC_DISTANCE);
				EntityRegistry.register(ImmersiveRailroading.instance, HandCar::new, ImmersiveRailroading.ENTITY_SYNC_DISTANCE);
				EntityRegistry.register(ImmersiveRailroading.instance, LocomotiveDiesel::new, ImmersiveRailroading.ENTITY_SYNC_DISTANCE);
				EntityRegistry.register(ImmersiveRailroading.instance, LocomotiveSteam::new, ImmersiveRailroading.ENTITY_SYNC_DISTANCE);
				EntityRegistry.register(ImmersiveRailroading.instance, Tender::new, ImmersiveRailroading.ENTITY_SYNC_DISTANCE);

				Packet.register(BuildableStockSyncPacket::new, PacketDirection.ServerToClient);
				Packet.register(ItemRailUpdatePacket::new, PacketDirection.ClientToServer);
				Packet.register(ItemMultiblockUpdatePacket::new, PacketDirection.ClientToServer);
				Packet.register(MRSSyncPacket::new, PacketDirection.ServerToClient);
				Packet.register(MultiblockSelectCraftPacket::new, PacketDirection.ClientToServer);
                Packet.register(MultiblockSetStockPacket::new, PacketDirection.ClientToServer);
                Packet.register(MultiblockControlChangePacket::new, PacketDirection.ClientToServer);
				Packet.register(PreviewRenderPacket::new, PacketDirection.ServerToClient);
				Packet.register(SoundPacket::new, PacketDirection.ServerToClient);
				Packet.register(KeyPressPacket::new, PacketDirection.ClientToServer);
				Packet.register(ItemTrackExchangerUpdatePacket::new, PacketDirection.ClientToServer);
				Packet.register(ClientPartDragging.DragPacket::new, PacketDirection.ClientToServer);
				Packet.register(ClientPartDragging.SeatPacket::new, PacketDirection.ClientToServer);
				Packet.register(GuiBuilder.ControlChangePacket::new, PacketDirection.ClientToServer);
				Packet.register(ItemPaintBrush.PaintBrushPacket::new, PacketDirection.ClientToServer);

				ServerChronoState.register();

				IRBlocks.register();
				IRItems.register();
				GuiTypes.register();

				Command.register(new IRCommand());

				break;
			case INITIALIZE:
				Config.init();
				ConfigFile.sync(Config.class);
				ConfigFile.sync(ConfigGraphics.class);
				ConfigFile.sync(ConfigSound.class);
				ConfigFile.sync(ConfigPermissions.class);

				DefinitionManager.initDefinitions();
				break;
			case FINALIZE:
				Permissions.register();

				MultiblockRegistry.register(SteamHammerMultiblock.NAME, new SteamHammerMultiblock());
				MultiblockRegistry.register(PlateRollerMultiblock.NAME, new PlateRollerMultiblock());
				MultiblockRegistry.register(RailRollerMultiblock.NAME, new RailRollerMultiblock());
				MultiblockRegistry.register(BoilerRollerMultiblock.NAME, new BoilerRollerMultiblock());
				MultiblockRegistry.register(CastingMultiblock.NAME, new CastingMultiblock());
                for (String s : DefinitionManager.multiblocks.keySet()) {
                    MultiblockRegistry.register(DefinitionManager.multiblocks.get(s).name,
                            new CustomTransporterMultiblock(DefinitionManager.multiblocks.get(s)));
                }
                TileMultiblockRender.registerOthers();
				IRFuzzy.applyFallbacks();
				break;
		}

	}

	@Override
	public void clientEvent(ModEvent event) {
		switch (event) {
			case CONSTRUCT:
				BlockRender.register(IRBlocks.BLOCK_RAIL, RailBaseModel::getModel, TileRail.class);
				BlockRender.register(IRBlocks.BLOCK_RAIL_GAG, RailBaseModel::getModel, TileRailGag.class);
				BlockRender.register(IRBlocks.BLOCK_RAIL_PREVIEW, RailPreviewRender::render, TileRailPreview.class);
				BlockRender.register(IRBlocks.BLOCK_MULTIBLOCK, TileMultiblockRender::render, TileMultiblock.class);

				ItemRender.register(IRItems.ITEM_PLATE, new PlateItemModel());
				ItemRender.register(IRItems.ITEM_AUGMENT, new RailAugmentItemModel());
				ItemRender.register(IRItems.ITEM_RAIL, new RailItemRender());
				ItemRender.register(IRItems.ITEM_CAST_RAIL, new RailCastItemRender());
				ItemRender.register(IRItems.ITEM_TRACK_BLUEPRINT, new TrackBlueprintItemModel());
				ItemRender.register(IRItems.ITEM_ROLLING_STOCK_COMPONENT, new StockItemComponentModel());
				ItemRender.register(IRItems.ITEM_ROLLING_STOCK, new StockItemModel());
				ItemRender.register(IRItems.ITEM_LARGE_WRENCH, ObjItemRender.getModelFor(new Identifier(MODID, "models/item/wrench/wrench.obj"), new Vec3d(0.5, 0, 0.5), 2));
				ItemRender.register(IRItems.ITEM_CONDUCTOR_WHISTLE, ObjItemRender.getModelFor(new Identifier(MODID, "models/item/whistle.obj"), new Vec3d(0.5, 0.75, 0.5), 0.1f));
				ItemRender.register(IRItems.ITEM_GOLDEN_SPIKE, ObjItemRender.getModelFor(new Identifier(MODID, "models/item/goldenspike/goldenspike.obj"), new Vec3d(0.5, 0.5, 0.5), 0.1f));
				ItemRender.register(IRItems.ITEM_HOOK, ObjItemRender.getModelFor(new Identifier(MODID, "models/item/brake_stick.obj"), new Vec3d(0.5, 0, 0.5), 2));
				ItemRender.register(IRItems.ITEM_SWITCH_KEY, ObjItemRender.getModelFor(new Identifier(MODID, "models/item/switch_key/switch_key.obj"), new Vec3d(0.5, 0, 0.5), 1));
				ItemRender.register(IRItems.ITEM_PAINT_BRUSH, ObjItemRender.getModelFor(new Identifier(MODID, "models/item/paint_brush.obj"), new Vec3d(0.5, 0.25, 0.5), 3));
				ItemRender.register(IRItems.ITEM_RADIO_CONTROL_CARD, new Identifier(MODID, "items/radio_card"));
				ItemRender.register(IRItems.ITEM_MANUAL, new Identifier(MODID, "items/engineerslexicon"));
				ItemRender.register(IRItems.ITEM_MULTIBLOCK_BLUEPRINT, new Identifier(MODID, "items/engineerslexicon"));
				ItemRender.register(IRItems.ITEM_TRACK_EXCHANGER, new TrackExchangerModel());

				IEntityRender<EntityMoveableRollingStock> stockRender = new IEntityRender<EntityMoveableRollingStock>() {
					@Override
					public void render(EntityMoveableRollingStock entity, RenderState state, float partialTicks) {
						StockModel<?, ?> renderer = entity.getDefinition().getModel();
						if (renderer != null) {
							renderer.renderEntity(entity, state, partialTicks);
						}
					}

					@Override
					public void postRender(EntityMoveableRollingStock entity, RenderState state, float partialTicks) {
						StockModel<?, ?> renderer = entity.getDefinition().getModel();
						if (renderer != null) {
							renderer.postRenderEntity(entity, state, partialTicks);
						}
					}
				};
				EntityRenderer.register(LocomotiveSteam.class, stockRender);
				EntityRenderer.register(LocomotiveDiesel.class, stockRender);
				EntityRenderer.register(CarPassenger.class, stockRender);
				EntityRenderer.register(CarFreight.class, stockRender);
				EntityRenderer.register(CarTank.class, stockRender);
				EntityRenderer.register(Tender.class, stockRender);
				EntityRenderer.register(HandCar.class, stockRender);


				Function<KeyTypes, Runnable> onKeyPress = type -> () -> new KeyPressPacket(type).sendToServer();
				Keyboard.registerKey("ir_keys.increase_throttle", KeyCode.NUMPAD8, "key.categories." + ImmersiveRailroading.MODID, onKeyPress.apply(KeyTypes.THROTTLE_UP));
				Keyboard.registerKey("ir_keys.zero_throttle", KeyCode.NUMPAD5, "key.categories." + ImmersiveRailroading.MODID, onKeyPress.apply(KeyTypes.THROTTLE_ZERO));
				Keyboard.registerKey("ir_keys.decrease_throttle", KeyCode.NUMPAD2, "key.categories." + ImmersiveRailroading.MODID, onKeyPress.apply(KeyTypes.THROTTLE_DOWN));
				Keyboard.registerKey("ir_keys.increase_reverser", KeyCode.NUMPAD9, "key.categories." + ImmersiveRailroading.MODID, onKeyPress.apply(KeyTypes.REVERSER_UP));
				Keyboard.registerKey("ir_keys.zero_reverser", KeyCode.NUMPAD6, "key.categories." + ImmersiveRailroading.MODID, onKeyPress.apply(KeyTypes.REVERSER_ZERO));
				Keyboard.registerKey("ir_keys.decrease_reverser", KeyCode.NUMPAD3, "key.categories." + ImmersiveRailroading.MODID, onKeyPress.apply(KeyTypes.REVERSER_DOWN));
				Keyboard.registerKey("ir_keys.increase_brake", KeyCode.NUMPAD7, "key.categories." + ImmersiveRailroading.MODID, onKeyPress.apply(KeyTypes.TRAIN_BRAKE_UP));
				Keyboard.registerKey("ir_keys.zero_brake", KeyCode.NUMPAD4, "key.categories." + ImmersiveRailroading.MODID, onKeyPress.apply(KeyTypes.TRAIN_BRAKE_ZERO));
				Keyboard.registerKey("ir_keys.decrease_brake", KeyCode.NUMPAD1, "key.categories." + ImmersiveRailroading.MODID, onKeyPress.apply(KeyTypes.TRAIN_BRAKE_DOWN));
				Keyboard.registerKey("ir_keys.increase_independent_brake", KeyCode.NUMPAD7, "key.categories." + ImmersiveRailroading.MODID, onKeyPress.apply(KeyTypes.INDEPENDENT_BRAKE_UP));
				Keyboard.registerKey("ir_keys.zero_independent_brake", KeyCode.NUMPAD4, "key.categories." + ImmersiveRailroading.MODID, onKeyPress.apply(KeyTypes.INDEPENDENT_BRAKE_ZERO));
				Keyboard.registerKey("ir_keys.decrease_independent_brake", KeyCode.NUMPAD1, "key.categories." + ImmersiveRailroading.MODID, onKeyPress.apply(KeyTypes.INDEPENDENT_BRAKE_DOWN));
				Keyboard.registerKey("ir_keys.horn", KeyCode.NUMPADENTER, "key.categories." + ImmersiveRailroading.MODID, onKeyPress.apply(KeyTypes.HORN));
				Keyboard.registerKey("ir_keys.dead_mans_switch", KeyCode.MULTIPLY, "key.categories." + ImmersiveRailroading.MODID, onKeyPress.apply(KeyTypes.DEAD_MANS_SWITCH));
				Keyboard.registerKey("ir_keys.start_stop_engine", KeyCode.ADD, "key.categories." + ImmersiveRailroading.MODID, onKeyPress.apply(KeyTypes.START_STOP_ENGINE));
				Keyboard.registerKey("ir_keys.bell", KeyCode.SUBTRACT, "key.categories." + ImmersiveRailroading.MODID, onKeyPress.apply(KeyTypes.BELL));
				Keyboard.registerKey("ir_keys.config", KeyCode.DIVIDE, "key.categories." + ImmersiveRailroading.MODID, () -> GuiTypes.CONFIG.open(MinecraftClient.getPlayer()));

				Audio.setSoundChannels(ConfigSound.customAudioChannels);
				break;
			case SETUP:
				GlobalRender.registerItemMouseover(IRItems.ITEM_TRACK_BLUEPRINT, TrackBlueprintItemModel::renderMouseover);
				GlobalRender.registerItemMouseover(IRItems.ITEM_MULTIBLOCK_BLUEPRINT, MBBlueprintRender::renderMouseover);

				GlobalRender.registerOverlay((state, pt) -> {
					Entity riding = MinecraftClient.getPlayer().getRiding();
					if (!(riding instanceof EntityRollingStock)) {
						return;
					}
					EntityRollingStock stock = (EntityRollingStock) riding;
					if (stock.getDefinition().getOverlay() != null) {
						stock.getDefinition().getOverlay().render(state, stock);
					}
				});

				ClientEvents.MOUSE_GUI.subscribe(evt -> {
					if (!MinecraftClient.isReady()) {
						return true;
					}
					Entity riding = MinecraftClient.getPlayer().getRiding();
					if (!(riding instanceof EntityRollingStock)) {
						return true;
					}
					EntityRollingStock stock = (EntityRollingStock) riding;
					if (stock.getDefinition().getOverlay() != null) {
						return stock.getDefinition().getOverlay().click(evt, stock);
					}
					return true;
				});

				ClientEvents.TICK.subscribe(GuiBuilder::onClientTick);
				ClientEvents.TICK.subscribe(EntityRollingStockDefinition.ControlSoundsDefinition::cleanupStoppedSounds);

				Particles.SMOKE = Particle.register(SmokeParticle::new, SmokeParticle::renderAll);

				ClientPartDragging.register();
				break;
			case RELOAD:
				DefinitionManager.initDefinitions();
				break;
		}
	}

	@Override
	public void serverEvent(ModEvent event) {
	}
}
