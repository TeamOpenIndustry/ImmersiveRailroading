package cam72cam.mod.render;

import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import net.minecraft.block.Block;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.LightUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class StandardModel {
    private List<Pair<IBlockState, IBakedModel>> models = new ArrayList<>();
    private List<Consumer<Float>> custom = new ArrayList<>();

    public StandardModel addColorBlock(Color color, Vec3d translate, Vec3d scale) {
        IBlockState state = Blocks.CONCRETE.getDefaultState();
        state = state.withProperty(BlockColored.COLOR, color.internal);
        IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelForState(state);
        models.add(Pair.of(state, new BakedScaledModel(model, scale, translate)));
        return this;
    }

    public StandardModel addSnow(int layers, Vec3d translate) {
        layers = Math.min(layers, 8);
        IBlockState state = Blocks.SNOW_LAYER.getDefaultState().withProperty(BlockSnow.LAYERS, layers);
        IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelForState(state);
        models.add(Pair.of(state, new BakedScaledModel(model, new Vec3d(1, 1,1), translate)));
        return this;
    }
    public StandardModel addItemBlock(ItemStack bed, Vec3d translate, Vec3d scale) {
        IBlockState state = itemToBlockState(bed);
        IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelForState(state);
        models.add(Pair.of(state, new BakedScaledModel(model, scale, translate)));
        return this;
    }
    public StandardModel addItem(ItemStack stack, Vec3d translate, Vec3d scale) {
        custom.add((pt) -> {
            GL11.glPushMatrix();
            {
                GL11.glScaled(scale.x, scale.y, scale.z);
                GL11.glTranslated(translate.x, translate.y, translate.z);
                Minecraft.getMinecraft().getRenderItem().renderItem(stack.internal, ItemCameraTransforms.TransformType.GROUND);
            }
            GL11.glPopMatrix();
        });
        return this;
    }
    public StandardModel addCustom(Runnable fn) {
        this.custom.add(pt -> fn.run());
        return this;
    }
    public StandardModel addCustom(Consumer<Float> fn) {
        this.custom.add(fn);
        return this;
    }

    public List<BakedQuad> getQuads(EnumFacing side, long rand) {
        List<BakedQuad> quads = new ArrayList<>();
        for (Pair<IBlockState, IBakedModel> model : models) {
            quads.addAll(model.getValue().getQuads(model.getKey(), side, rand));
        }

        return quads;
    }

    public static IBlockState itemToBlockState(cam72cam.mod.item.ItemStack stack) {
        Block block = Block.getBlockFromItem(stack.internal.getItem());
        @SuppressWarnings("deprecation")
        IBlockState gravelState = block.getStateFromMeta(stack.internal.getMetadata());
        if (block instanceof BlockLog) {
            gravelState = gravelState.withProperty(BlockLog.LOG_AXIS, BlockLog.EnumAxis.Z);
        }
        return gravelState;
    }

    public void render() {
        render(0);
    }
    public void render(float partialTicks) {
        renderCustom(partialTicks);
        renderQuads();
    }

    public void renderQuads() {
        List<BakedQuad> quads = new ArrayList<>();
        for (Pair<IBlockState, IBakedModel> model : models) {
            quads.addAll(model.getRight().getQuads(null, null, 0));
            for (EnumFacing facing : EnumFacing.values()) {
                quads.addAll(model.getRight().getQuads(null, facing, 0));
            }

        }
        if (quads.isEmpty()) {
            return;
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        BufferBuilder worldRenderer = new BufferBuilder(2048);
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        quads.forEach(quad -> LightUtil.renderQuadColor(worldRenderer, quad, -1));
        worldRenderer.finishDrawing();
        new WorldVertexBufferUploader().draw(worldRenderer);
    }

    public void renderCustom() {
        renderCustom(0);
    }

    public void renderCustom(float partialTicks) {
        custom.forEach(cons -> cons.accept(partialTicks));
    }

    public boolean hasCustom() {
        return !custom.isEmpty();
    }
}
