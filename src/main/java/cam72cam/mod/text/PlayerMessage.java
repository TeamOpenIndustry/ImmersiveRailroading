package cam72cam.mod.text;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.ForgeHooks;

public class PlayerMessage {
    public final ITextComponent internal;
    private PlayerMessage(ITextComponent component) {
        internal = component;
    }

    public static PlayerMessage direct(String msg) {
        return new PlayerMessage(new TextComponentString(msg));
    }

    public static PlayerMessage translate(String msg, Object... objects) {
        return new PlayerMessage(new TextComponentTranslation(msg, objects));
    }

    public static PlayerMessage url(String url) {
        return new PlayerMessage(ForgeHooks.newChatWithLinks(url));
    }
}
