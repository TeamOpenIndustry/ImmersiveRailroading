package cam72cam.mod.render;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.gui.Progress;
import cam72cam.mod.item.ItemBase;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.world.World;
import com.google.common.collect.ImmutableList;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ItemLayerModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ItemRender {
    private static final List<BakedQuad> EMPTY = new ArrayList<>();
    private static final List<Consumer<ModelBakeEvent>> bakers = new ArrayList<>();
    private static final List<Runnable> mappers = new ArrayList<>();
    private static final List<Consumer<TextureStitchEvent.Pre>> textures = new ArrayList<>();
    private static final SpriteSheet iconSheet = new SpriteSheet(128);

    @SubscribeEvent
    public static void onModelBakeEvent(ModelBakeEvent event) {
        bakers.forEach(baker -> baker.accept(event));
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        mappers.forEach(Runnable::run);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onTextureStich(TextureStitchEvent.Pre event) {
        textures.forEach(texture -> texture.accept(event));
    }

    public static void register(ItemBase item, Identifier tex) {
        bakers.add(event -> event.getModelRegistry().putObject(new ModelResourceLocation(item.getRegistryName().internal, ""), new ItemLayerModel(ImmutableList.of(
               tex.internal
        )).bake(TRSRTransformation.identity(), DefaultVertexFormats.ITEM, ModelLoader.defaultTextureGetter())));

        textures.add(event -> Minecraft.getMinecraft().getTextureMapBlocks().registerSprite(tex.internal));

        mappers.add(() -> ModelLoader.setCustomModelResourceLocation(item.internal, 0,
                new ModelResourceLocation(item.getRegistryName().internal, "")));
    }

    public static void register(ItemBase item, BiFunction<ItemStack, World, StandardModel> model) {
        register(item, model, null);
    }

    public static void register(ItemBase item, BiFunction<ItemStack, World, StandardModel> model, Function<ItemStack, Pair<String, StandardModel>> cacheRender) {
        mappers.add(() ->
                ModelLoader.setCustomModelResourceLocation(item.internal, 0, new ModelResourceLocation(item.getRegistryName().internal, ""))
        );

        bakers.add((ModelBakeEvent event) -> event.getModelRegistry().putObject(new ModelResourceLocation(item.getRegistryName().internal, ""), new BakedItemModel(model, cacheRender)));

        if (cacheRender != null) {
            textures.add((event) -> {
                List<ItemStack> variants = item.getItemVariants(null);
                Progress.Bar bar = Progress.push(item.getClass().getSimpleName() + " Icon", variants.size());
                for (ItemStack stack : variants) {
                    Pair<String, StandardModel> info = cacheRender.apply(stack);
                    bar.step(info.getKey());
                    createSprite(info.getKey(), info.getValue());
                }
                Progress.pop(bar);
            });
        }
    }

    static class BakedItemModel implements IBakedModel {
        private final ItemStack stack;
        private final World world;
        private final BiFunction<ItemStack, World, StandardModel> model;
        private final Function<ItemStack, Pair<String, StandardModel>> cacheRender;
        private final boolean isGUI;

        BakedItemModel(BiFunction<ItemStack, World, StandardModel> model, Function<ItemStack, Pair<String, StandardModel>> cacheRender) {
            this.world = null;
            this.stack = null;
            this.model = model;
            this.cacheRender = cacheRender;
            isGUI = false;
        }

        BakedItemModel(ItemStack stack, World world, BiFunction<ItemStack, World, StandardModel> model, Function<ItemStack, Pair<String, StandardModel>> cacheRender, boolean isGUI) {
            this.stack = stack;
            this.world = world;
            this.model = model;
            this.cacheRender = cacheRender;
            this.isGUI = isGUI;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
            if (stack == null || world == null) {
                return EMPTY;
            }

            if (isGUI) {
                iconSheet.renderSprite(cacheRender.apply(stack).getKey());
                return EMPTY;
            }

            StandardModel std = model.apply(stack, world);
            if (std == null) {
                return EMPTY;
            }


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
            if (side == null) {
                std.renderCustom();
            }

            return std.getQuads(side, rand);
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

        class ItemOverrideListHack extends ItemOverrideList {
            ItemOverrideListHack() {
                super(new ArrayList<>());
            }

            @Override
            public IBakedModel handleItemState(IBakedModel originalModel, net.minecraft.item.ItemStack stack, @Nullable net.minecraft.world.World world, @Nullable EntityLivingBase entity) {
                return new BakedItemModel(new ItemStack(stack), MinecraftClient.getPlayer().getWorld(), model, cacheRender, isGUI);
            }
        }

        @Override
        public ItemOverrideList getOverrides() {
            return new ItemOverrideListHack();
        }

        @Override
        public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType cameraTransformType) {
            Pair<? extends IBakedModel, Matrix4f> def = ForgeHooksClient.handlePerspective(this, cameraTransformType);
            // TODO more efficient
            if (cacheRender != null && (cameraTransformType == ItemCameraTransforms.TransformType.GUI)) {
                return Pair.of(new BakedItemModel(stack, world, model, cacheRender, true), def.getRight());
            }
            // TODO Expose as part of the renderItem API
            if (cameraTransformType == ItemCameraTransforms.TransformType.FIXED) {
                Matrix4f mat = new Matrix4f();
                mat.setIdentity();
                mat.rotY((float) Math.toRadians(90));
                return Pair.of(def.getLeft(), mat);
            }
            if (cameraTransformType == ItemCameraTransforms.TransformType.HEAD) {
                Matrix4f mat = new Matrix4f();
                mat.setIdentity();
                mat.setScale(2);
                mat.setTranslation(new Vector3f(0, 1, 0));
                return Pair.of(def.getLeft(), mat);
            }
            return def;
        }
    }

    private static void createSprite(String id, StandardModel model) {
        int width = iconSheet.spriteSize;
        int height = iconSheet.spriteSize;
        Framebuffer fb = new Framebuffer(width, height, true);
        fb.setFramebufferColor(0, 0, 0, 0);
        fb.framebufferClear();
        fb.bindFramebuffer(true);

        GLBoolTracker depth = new GLBoolTracker(GL11.GL_DEPTH_TEST, true);
        GL11.glDepthFunc(GL11.GL_LESS);
        GL11.glClearDepth(1);

        model.renderCustom();

        ByteBuffer buff = ByteBuffer.allocateDirect(4 * width * height);
        GL11.glReadPixels(0, 0, width, height, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, buff);

        fb.unbindFramebuffer();
        fb.deleteFramebuffer();
        depth.restore();

        iconSheet.setSprite(id, buff);
    }
}
