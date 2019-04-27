package cam72cam.mod.util;

import net.minecraft.util.ResourceLocation;

public class Identifier {
    public final ResourceLocation internal;

    public Identifier(ResourceLocation internal) {
        this.internal = internal;
    }

    public Identifier(String ident) {
        this(new ResourceLocation(ident));
    }
}
