package cam72cam.mod.render;

import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.block.BlockType;
import cam72cam.mod.block.BlockTypeEntity;
import cam72cam.mod.block.tile.TileEntity;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.ColorizerGrass;
import net.minecraft.world.biome.BiomeColorHelper;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@Mod.EventBusSubscriber(Side.CLIENT)
public class BlockRender {
    private static final List<BakedQuad> EMPTY = new ArrayList<>();
    private static final List<Consumer<ModelBakeEvent>> bakers = new ArrayList<>();
    private static final List<Runnable> colors = new ArrayList<>();
    private static final Map<Class<? extends BlockEntity>, Function<BlockEntity, StandardModel>> renderers = new HashMap<>();

    @SubscribeEvent
    public static void onModelBakeEvent(ModelBakeEvent event) {
        bakers.forEach(baker -> baker.accept(event));
    }

    public static void onPostColorSetup() {
        // TODO call from non mod context (subscribe event)
        colors.forEach(Runnable::run);

        ClientRegistry.bindTileEntitySpecialRenderer(TileEntity.class, new TileEntitySpecialRenderer<TileEntity>() {
            public void render(TileEntity te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
                BlockEntity instance = te.instance();
                if (instance == null) {
                    return;
                }
                Class<? extends BlockEntity> cls = instance.getClass();
                Function<BlockEntity, StandardModel> renderer = renderers.get(cls);
                if (renderer == null) {
                    return;
                }

                StandardModel model = renderer.apply(instance);
                if (model == null) {
                    return;
                }

                if (!model.hasCustom()) {
                    return;
                }

                GL11.glPushMatrix();
                {
                    GL11.glTranslated(x, y, z);
                    model.renderCustom();
                }
                GL11.glPopMatrix();
            }
            public boolean isGlobalRenderer(TileEntity te) {
                return true;
            }
        });
    }

    // TODO version for non TE blocks

    public static <T extends BlockEntity> void register(BlockType block, Function<T, StandardModel> model, Class<T> cls) {
        renderers.put(cls, (te) -> model.apply(cls.cast(te)));

        colors.add(() -> {
            BlockColors blockColors = Minecraft.getMinecraft().getBlockColors();
            blockColors.registerBlockColorHandler((state, worldIn, pos, tintIndex) -> worldIn != null && pos != null ? BiomeColorHelper.getGrassColorAtPos(worldIn, pos) : ColorizerGrass.getGrassColor(0.5D, 1.0D), block.internal);
        });

        bakers.add(event -> {
            event.getModelRegistry().putObject(new ModelResourceLocation(block.internal.getRegistryName(), ""), new IBakedModel() {
                @Override
                public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
                    if (block instanceof BlockTypeEntity) {
                        if (!(state instanceof IExtendedBlockState)) {
                            return EMPTY;
                        }
                        IExtendedBlockState extState = (IExtendedBlockState) state;
                        Object data = extState.getValue(BlockTypeEntity.BLOCK_DATA);
                        if (!cls.isInstance(data)) {
                            return EMPTY;
                        }
                        StandardModel out = model.apply(cls.cast(data));
                        if (out == null) {
                            return EMPTY;
                        }
                        return out.getQuads(side, rand);
                    } else {
                        // TODO
                        return EMPTY;
                    }
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
                    if (block.internal.getMaterial(null) == Material.IRON) {
                        return Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelForState(Blocks.IRON_BLOCK.getDefaultState()).getParticleTexture();
                    }
                    return Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelForState(Blocks.STONE.getDefaultState()).getParticleTexture();
                }

                @Override
                public ItemOverrideList getOverrides() {
                    return null;
                }
            });
        });
    }
}
