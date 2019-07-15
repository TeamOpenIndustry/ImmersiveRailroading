package cam72cam.mod.render;

import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import net.minecraft.block.Block;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class StandardModel {
    private List<Pair<IBlockState, IBakedModel>> models;
    public StandardModel() {
        this.models = new ArrayList<>();
    }

    public void addColorBlock(Color color, Vec3d translate, Vec3d scale) {
         // TODO rest of scale/translate
        IBlockState state = Blocks.CONCRETE.getDefaultState();
        state = state.withProperty(BlockColored.COLOR, color.internal);
        IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelForState(state);
        models.add(Pair.of(state, new BakedScaledModel(model, (float) scale.y)));
    }

    public void addSnow(int layers, Vec3d translate) {
         // TODO rest of scale/translate
        layers = Math.min(layers, 8);
        IBlockState state = Blocks.SNOW_LAYER.getDefaultState().withProperty(BlockSnow.LAYERS, layers);
        IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelForState(state);
        models.add(Pair.of(state, model));
    }
    public void addItem(ItemStack bed, Vec3d translate, Vec3d scale) {
        // TODO rest of scale/translate
        IBlockState state = itemToBlockState(bed);
        IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelForState(state);
        models.add(Pair.of(state, new BakedScaledModel(model, (float)scale.y)));
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
}
