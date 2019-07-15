package cam72cam.mod.render;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.item.ItemBase;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.world.World;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ItemRender {
    private static final List<BakedQuad> EMPTY = new ArrayList<>();
    private static final List<Consumer<ModelBakeEvent>> bakers = new ArrayList<>();
    private static final List<Runnable> mappers = new ArrayList<>();

    @SubscribeEvent
    public static void onModelBakeEvent(ModelBakeEvent event) {
        bakers.forEach(baker -> baker.accept(event));
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        mappers.forEach(Runnable::run);
    }

    public static void register(ItemBase item, BiFunction<ItemStack, World, StandardModel> model) {
        mappers.add(() ->
                ModelLoader.setCustomModelResourceLocation(item.internal, 0, new ModelResourceLocation(item.getRegistryName().internal, ""))
        );

        bakers.add((ModelBakeEvent event) -> event.getModelRegistry().putObject(new ModelResourceLocation(item.getRegistryName().internal, ""), new BakedItemModel(model)));
    }

    static class BakedItemModel implements IBakedModel {
        private final ItemStack stack;
        private final World world;
        private final BiFunction<ItemStack, World, StandardModel> model;

        BakedItemModel(BiFunction<ItemStack, World, StandardModel> model) {
            this.world = null;
            this.stack = null;
            this.model = model;
        }

        BakedItemModel(ItemStack stack, World world, BiFunction<ItemStack, World, StandardModel> model) {
            this.stack = stack;
            this.world = world;
            this.model = model;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
            if (stack == null || world == null) {
                return EMPTY;
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
                return new BakedItemModel(new ItemStack(stack), MinecraftClient.getPlayer().getWorld(), model);
            }
        }

        @Override
        public ItemOverrideList getOverrides() {
            return new ItemOverrideListHack();
        }
    }
}
