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
    private List<Pair<IBlockState, IBakedModel>> models = new ArrayList<>();
    private List<Runnable> custom = new ArrayList<>();

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
    public StandardModel addItem(ItemStack bed, Vec3d translate, Vec3d scale) {
        IBlockState state = itemToBlockState(bed);
        IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelForState(state);
        models.add(Pair.of(state, new BakedScaledModel(model, scale, translate)));
        return this;
    }
    public StandardModel addCustom(Runnable fn) {
        this.custom.add(fn);
        return this;
    }

    public List<BakedQuad> getQuads(EnumFacing side, long rand) {
        List<BakedQuad> quads = new ArrayList<>();
        for (Pair<IBlockState, IBakedModel> model : models) {
            quads.addAll(model.getValue().getQuads(model.getKey(), side, rand));
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

        // Model can only be rendered once.  If mods go through the RenderItem.renderModel as they are supposed to this should work just fine
        if (side == null) {
            custom.forEach(Runnable::run);
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
