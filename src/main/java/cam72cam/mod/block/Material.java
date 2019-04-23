package cam72cam.mod.block;

import net.minecraft.block.SoundType;

public enum Material {
    METAL(net.minecraft.block.material.Material.IRON, SoundType.METAL),
    WOOL(net.minecraft.block.material.Material.CARPET, SoundType.CLOTH),
    ;

    protected final net.minecraft.block.material.Material internal;
    protected final SoundType soundType;

    Material(net.minecraft.block.material.Material internal, SoundType soundType) {
        this.internal = internal;
        this.soundType = soundType;
    }
}
