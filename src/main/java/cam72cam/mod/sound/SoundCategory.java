package cam72cam.mod.sound;

public enum SoundCategory {
    MASTER(net.minecraft.util.SoundCategory.MASTER),
    MUSIC(net.minecraft.util.SoundCategory.MUSIC),
    RECORDS(net.minecraft.util.SoundCategory.RECORDS),
    WEATHER(net.minecraft.util.SoundCategory.WEATHER),
    BLOCKS(net.minecraft.util.SoundCategory.BLOCKS),
    HOSTILE(net.minecraft.util.SoundCategory.HOSTILE),
    NEUTRAL(net.minecraft.util.SoundCategory.NEUTRAL),
    PLAYERS(net.minecraft.util.SoundCategory.PLAYERS),
    AMBIENT(net.minecraft.util.SoundCategory.AMBIENT),
    VOICE(net.minecraft.util.SoundCategory.VOICE),
    ;

    final net.minecraft.util.SoundCategory category;

    SoundCategory(net.minecraft.util.SoundCategory category) {
        this.category = category;
    }
}
