package cam72cam.mod.render;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.item.ItemBase;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.world.World;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
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
import java.awt.image.BufferedImage;
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

    public static void register(ItemBase item, BiFunction<ItemStack, World, StandardModel> model) {
        _register(item, model, null);
    }


    public static void register(ItemBase item, BiFunction<ItemStack, World, StandardModel> model, Function<ItemStack, Pair<String, StandardModel>> cacheRender) {
        try {
            Class.forName("net.optifine.shaders.ShadersRender");
            cacheRender = null;
        } catch (ClassNotFoundException e) {
            // NOP
        }
        _register(item, model, cacheRender);
    }

    private static void _register(ItemBase item, BiFunction<ItemStack, World, StandardModel> model, Function<ItemStack, Pair<String, StandardModel>> cacheRender) {
        mappers.add(() ->
                ModelLoader.setCustomModelResourceLocation(item.internal, 0, new ModelResourceLocation(item.getRegistryName().internal, ""))
        );

        bakers.add((ModelBakeEvent event) -> event.getModelRegistry().putObject(new ModelResourceLocation(item.getRegistryName().internal, ""), new BakedItemModel(model, cacheRender)));

        if (cacheRender != null) {
            textures.add((event) -> {
                for (ItemStack stack : item.getItemVariants(null)) {
                    event.getMap().setTextureEntry(new StockIcon(cacheRender.apply(stack)));
                }
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
                TextureMap map = Minecraft.getMinecraft().getTextureMapBlocks();
                TextureAtlasSprite sprite = map.getAtlasSprite(cacheRender.apply(stack).getKey());
                if (!sprite.equals(map.getMissingSprite())) {
                    Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
                    // TODO figure out how to make this bakedquads...

                    GL11.glPushMatrix();
                    GL11.glRotated(180, 1, 0, 0);
                    GL11.glTranslated(0, -1, 0);
                    GL11.glBegin(GL11.GL_QUADS);
                    GL11.glColor4f(1, 1, 1, 1);
                    GL11.glTexCoord2f(sprite.getMinU(), sprite.getMinV());
                    GL11.glVertex3f(0, 0, 0);
                    GL11.glTexCoord2f(sprite.getMinU(), sprite.getMaxV());
                    GL11.glVertex3f(0, 1, 0);
                    GL11.glTexCoord2f(sprite.getMaxU(), sprite.getMaxV());
                    GL11.glVertex3f(1, 1, 0);
                    GL11.glTexCoord2f(sprite.getMaxU(), sprite.getMinV());
                    GL11.glVertex3f(1, 0, 0);
                    GL11.glEnd();
                    GL11.glPopMatrix();
                    return EMPTY;
                }
            }

            StandardModel std = model.apply(stack, world);
            if (std == null) {
                return EMPTY;
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

    public static final class StockIcon extends TextureAtlasSprite
    {
        private final StandardModel model;

        public StockIcon(Pair<String, StandardModel> pair) {
            super(pair.getKey());
            this.model = pair.getValue();
            this.width = this.height = 64;
        }

        @Override
        public boolean hasCustomLoader(IResourceManager manager, ResourceLocation location)
        {
            return true;
        }

        @Override
        public boolean load(IResourceManager manager, ResourceLocation location, Function<ResourceLocation, TextureAtlasSprite> textureGetter)
        {
            Framebuffer fb = new Framebuffer(width, height, true);
            fb.setFramebufferColor(0, 0, 0, 0);
            fb.framebufferClear();
            fb.bindFramebuffer(true);

            BufferedImage image = new BufferedImage(this.getIconWidth(), this.getIconHeight(), BufferedImage.TYPE_INT_ARGB);

            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LESS);
            GL11.glClearDepth(1);

            model.getQuads(null, 0);

            ByteBuffer buff = ByteBuffer.allocateDirect(4 * width * height);
            GL11.glReadPixels(0, 0, width, height, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, buff);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int i = 0;
                    i += buff.get() << 0;
                    i += buff.get() << 8;
                    i += buff.get() << 16;
                    i += buff.get() << 24;
                    image.setRGB(x, y, i);
                }
            }

            fb.unbindFramebuffer();
            fb.deleteFramebuffer();

            /*
            File loc = new File("/home/gilligan/test/" + super.getIconName().replace('/', '.') + ".png");
            try {
                ImageIO.write(image, "png", loc);
            } catch (IOException e) {
                e.printStackTrace();
            }
            */


            int[] pixels = new int[image.getWidth() * image.getHeight()];
            image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
            this.clearFramesTextureData();
            int[][] fd = new int[Minecraft.getMinecraft().gameSettings.mipmapLevels + 1][];
            fd[0] = pixels;
            this.framesTextureData.add(fd);
            return false;
        }
    }
}
