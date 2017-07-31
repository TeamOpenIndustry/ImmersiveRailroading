package cam72cam.immersiverailroading.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.vecmath.Matrix4f;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.CarFreight;
import cam72cam.immersiverailroading.entity.CarTank;
import cam72cam.immersiverailroading.entity.EntityRidableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.registry.DefinitionManager;
import cam72cam.immersiverailroading.entity.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.gui.FreightContainer;
import cam72cam.immersiverailroading.gui.FreightContainerGui;
import cam72cam.immersiverailroading.gui.TankContainer;
import cam72cam.immersiverailroading.gui.TankContainerGui;
import cam72cam.immersiverailroading.items.ItemRollingStock;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.net.KeyPressPacket;
import cam72cam.immersiverailroading.render.obj.OBJModel;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailTESR;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.relauncher.Side;
import util.Matrix4;

@EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy {
	private static Map<KeyTypes, KeyBinding> keys = new HashMap<KeyTypes, KeyBinding>();

	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int entityID, int nop1, int nop2) {
		return null;
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int entityID, int nop1, int nop2) {
		switch (GuiTypes.values()[ID]) {
		case FREIGHT:
			return new FreightContainerGui((CarFreight) world.getEntityByID(entityID),
					new FreightContainer(player.inventory, (CarFreight) world.getEntityByID(entityID)));
		case TANK:
			return new TankContainerGui((CarTank) world.getEntityByID(entityID),
					new TankContainer(player.inventory, (CarTank) world.getEntityByID(entityID)));
		case TENDER:
			return null;
		default:
			return null;
		}
	}
	
	

	@Override
	public void init(FMLInitializationEvent event) {
		super.init(event);
		
		keys.put(KeyTypes.THROTTLE_UP, new KeyBinding("Increase Throttle", Keyboard.KEY_NUMPAD8, "key.categories." + ImmersiveRailroading.MODID));
		keys.put(KeyTypes.THROTTLE_DOWN, new KeyBinding("Decrease Throttle", Keyboard.KEY_NUMPAD5, "key.categories." + ImmersiveRailroading.MODID));
		keys.put(KeyTypes.THROTTLE_ZERO, new KeyBinding("Zero Throttle", Keyboard.KEY_ADD, "key.categories." + ImmersiveRailroading.MODID));
		keys.put(KeyTypes.HORN, new KeyBinding("Sound Horn", Keyboard.KEY_NUMPADENTER, "key.categories." + ImmersiveRailroading.MODID));
		keys.put(KeyTypes.PLAYER_FORWARD, Minecraft.getMinecraft().gameSettings.keyBindForward);
		keys.put(KeyTypes.PLAYER_BACKWARD, Minecraft.getMinecraft().gameSettings.keyBindBack);
		keys.put(KeyTypes.PLAYER_LEFT, Minecraft.getMinecraft().gameSettings.keyBindLeft);
		keys.put(KeyTypes.PLAYER_RIGHT, Minecraft.getMinecraft().gameSettings.keyBindRight);
		
	}
	
	public World getWorld(int dimension)  {
		return FMLClientHandler.instance().getWorldClient();
	}

	public static final IRenderFactory<EntityRollingStock> RENDER_INSTANCE = new IRenderFactory<EntityRollingStock>() {
		@Override
		public Render<? super EntityRollingStock> createRenderFor(RenderManager manager) {
			return new Render<EntityRollingStock>(manager) {
				@Override
				public boolean shouldRender(EntityRollingStock livingEntity, ICamera camera, double camX, double camY, double camZ) {
					return true;
				}

				@Override
				public void doRender(EntityRollingStock stock, double x, double y, double z, float entityYaw, float partialTicks) {
					EntityRollingStockDefinition def = stock.getDefinition();

					OBJModel model = def.getModel();
					Matrix4 defaultTransform = def.getDefaultTransformation();

					GlStateManager.pushAttrib();
					GlStateManager.pushMatrix();

					// Bind block textures to current context
					if (model.texLoc != null) {
						Minecraft.getMinecraft().getTextureManager().bindTexture(model.texLoc);
					}

					GL11.glDisable(GL11.GL_LIGHTING);

					// Move to specified position
					GlStateManager.translate(x, y + 0.2, z);

					GlStateManager.scale(2, 2, 2);

					GlStateManager.rotate(180 - entityYaw, 0, 1, 0);
					GlStateManager.rotate(stock.rotationPitch, 1, 0, 0);
					FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
					Matrix4 transform = defaultTransform.copy().rotate(Math.toRadians(180), 0, 1, 0);
					matrix.put((float) transform.m00);
					matrix.put((float) transform.m01);
					matrix.put((float) transform.m02);
					matrix.put((float) transform.m03);
					matrix.put((float) transform.m10);
					matrix.put((float) transform.m11);
					matrix.put((float) transform.m12);
					matrix.put((float) transform.m13);
					matrix.put((float) transform.m20);
					matrix.put((float) transform.m21);
					matrix.put((float) transform.m22);
					matrix.put((float) transform.m23);
					matrix.put((float) transform.m30);
					matrix.put((float) transform.m31);
					matrix.put((float) transform.m32);
					matrix.put((float) transform.m33);
					matrix.flip();

					GlStateManager.multMatrix(matrix);

					model.draw();

					GL11.glEnable(GL11.GL_LIGHTING);

					GlStateManager.popMatrix();
					GlStateManager.popAttrib();

					// Render.renderOffsetAABB(stock.getCollisionBoundingBox(),
					// x, y, z);
				}

				@Override
				protected ResourceLocation getEntityTexture(EntityRollingStock entity) {
					return null;
				}
			};
		}
	};

	@SubscribeEvent
	public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
		for (Class<? extends EntityRollingStock> type : entityClasses) {
			RenderingRegistry.registerEntityRenderingHandler(type, RENDER_INSTANCE);
		}
	}

	@SubscribeEvent
	public static void registerModels(ModelRegistryEvent event) {
		OBJLoader.INSTANCE.addDomain(ImmersiveRailroading.MODID.toLowerCase());

		ClientRegistry.bindTileEntitySpecialRenderer(TileRail.class, new TileRailTESR());
		for (TrackItems item : TrackItems.values()) {
			ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(ImmersiveRailroading.BLOCK_RAIL), item.getMeta(),
					new ModelResourceLocation(ImmersiveRailroading.BLOCK_RAIL.getRegistryName(), "inventory"));
		}

		ModelLoader.setCustomMeshDefinition(ImmersiveRailroading.ITEM_ROLLING_STOCK, new ItemMeshDefinition() {
			@Override
			public ModelResourceLocation getModelLocation(ItemStack stack) {
				// TODO NBT or Damage
				return new ModelResourceLocation(ImmersiveRailroading.ITEM_ROLLING_STOCK.getRegistryName(), ItemRollingStock.defFromStack(stack));
			}
		});
	}

	@SubscribeEvent
	public static void onModelBakeEvent(ModelBakeEvent event) {
		for (String defID : DefinitionManager.getDefinitionNames()) {
			ModelResourceLocation loc = new ModelResourceLocation(ImmersiveRailroading.ITEM_ROLLING_STOCK.getRegistryName(), defID);
			IBakedModel model = getInventoryModel(DefinitionManager.getDefinition(defID));
			event.getModelRegistry().putObject(loc, model);
		}
	}

	@SubscribeEvent
	public static void onKeyInput(InputEvent.KeyInputEvent event) {
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		for (KeyTypes key : KeyTypes.values()) {
			KeyBinding binding = keys.get(key);
			if (binding.isKeyDown()) {
				EntityRidableRollingStock riding = (EntityRidableRollingStock) player.getRidingEntity();
				if (riding != null) {
					ImmersiveRailroading.net.sendToServer(new KeyPressPacket(key, riding.getEntityWorld().provider.getDimension(), player.getEntityId(), riding.getEntityId()));
				}
			}
		}
	}

	private static IBakedModel getInventoryModel(EntityRollingStockDefinition def) {
		OBJModel model = def.getModel();
		Matrix4 defaultTransform = def.getDefaultTransformation();
		return new IBakedModel() {
			@Override
			public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
				/*
				 * I am an evil wizard!
				 * 
				 * So it turns out that I can stick a draw call in here to
				 * render my own stuff. This subverts forge's entire baked model
				 * system with a single line of code and injects my own OpenGL
				 * payload. Fuck you modeling restrictions.
				 * 
				 * This is probably really fragile if someone calls getQuads
				 * before actually setting up the correct GL context.
				 */
				if (model.texLoc != null) {
					Minecraft.getMinecraft().getTextureManager().bindTexture(model.texLoc);
				} else {
					Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_MISSING_TEXTURE);
				}
				GL11.glDisable(GL11.GL_LIGHTING);
				model.draw();
				GL11.glEnable(GL11.GL_LIGHTING);
				Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
				return new ArrayList<BakedQuad>();
			}

			@Override
			public boolean isAmbientOcclusion() {
				return true;
			}

			@Override
			public boolean isGui3d() {
				return true;
			}

			@Override
			public boolean isBuiltInRenderer() {
				return false;
			}

			@Override
			public TextureAtlasSprite getParticleTexture() {
				return null;
			}

			@Override
			public ItemOverrideList getOverrides() {
				return ItemOverrideList.NONE;
			}

			@Override
			public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType cameraTransformType) {
				Pair<? extends IBakedModel, Matrix4f> defaultVal = ForgeHooksClient.handlePerspective(this, cameraTransformType);
				switch (cameraTransformType) {
				case THIRD_PERSON_LEFT_HAND:
				case THIRD_PERSON_RIGHT_HAND:
					return Pair.of(defaultVal.getLeft(),
							new Matrix4().scale(0.4, 0.4, 0.4).rotate(Math.toRadians(60), 1, 0, 0).multiply(defaultTransform).toMatrix4f());
				case FIRST_PERSON_LEFT_HAND:
				case FIRST_PERSON_RIGHT_HAND:
					return Pair.of(defaultVal.getLeft(),
							new Matrix4().scale(0.4, 0.4, 0.4).rotate(Math.toRadians(10), 1, 0, 0).multiply(defaultTransform).toMatrix4f());
				case GROUND:
					return Pair.of(defaultVal.getLeft(), defaultTransform.copy().scale(2, 2, 2).toMatrix4f());
				case FIXED:
					// Item Frame
					return Pair.of(defaultVal.getLeft(), defaultTransform.copy().scale(4, 4, 4).toMatrix4f());
				case GUI:
					return Pair.of(defaultVal.getLeft(), new Matrix4().translate(0, -0.1, 0).scale(0.3, 0.3, 0.3).rotate(Math.toRadians(200), 0, 1, 0)
							.rotate(Math.toRadians(-15), 1, 0, 0).multiply(defaultTransform.copy()).toMatrix4f());
				case HEAD:
					return Pair.of(defaultVal.getLeft(),
							new Matrix4().scale(2, 2, 2).translate(0, 0, 0.5).leftMultiply(defaultTransform).toMatrix4f());
				case NONE:
					return defaultVal;
				}
				return defaultVal;
			}
		};
	}

	public InputStream getResourceStream(ResourceLocation modelLoc) throws IOException {
		return Minecraft.getMinecraft().getResourceManager().getResource(modelLoc).getInputStream();
	}
}
