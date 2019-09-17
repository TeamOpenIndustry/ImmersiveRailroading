package cam72cam.mod.world;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Blocks that implement this interface can decide whether to allow collision,
 * depending on certain conditions.
 */
public interface IConditionalCollision {

    /**
     * Return whether or not a block at the given position with the given state can collide
     * with the given damage source.
     *
     * @param world World the block is in.
     * @param pos Position of the block.
     * @param state Block state of the block.
     * @param damageSource Damage source that would be used to collide with the block.
     * @return Whether or not to calculate actual collision.
     */
    boolean canCollide(World world, BlockPos pos, IBlockState state, DamageSource damageSource);

}
