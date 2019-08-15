package cam72cam.mod.world;

import cam72cam.mod.util.TagCompound;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;

public class BlockInfo {
    final IBlockState internal;

    BlockInfo(IBlockState state) {
        this.internal = state;
    }

    public BlockInfo(TagCompound info) {
        internal = NBTUtil.readBlockState(info.internal);
    }

    public TagCompound toNBT() {
        return new TagCompound(NBTUtil.writeBlockState(new NBTTagCompound(), internal));
    }
}
