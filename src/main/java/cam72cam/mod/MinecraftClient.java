package cam72cam.mod;

import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;

public class MinecraftClient {
    public static Player getPlayer(){
        if (Minecraft.getMinecraft().player == null) {
            return null;
        }
        return new Player(Minecraft.getMinecraft().player);
    }

    public static void startProfiler(String section) {
        Minecraft.getMinecraft().mcProfiler.startSection(section);
    }
    public static void endProfiler() {
        Minecraft.getMinecraft().mcProfiler.endSection();
    }

    public static boolean useVBO() {
        return OpenGlHelper.useVbo();
    }

    public static Entity getEntityMouseOver() {
        net.minecraft.entity.Entity ent = Minecraft.getMinecraft().objectMouseOver.entityHit;
        if (ent != null) {
            return getPlayer().getWorld().getEntity(ent.getUniqueID(), Entity.class);
        }
        return null;
    }

    public static boolean isPaused() {
        return Minecraft.getMinecraft().isGamePaused();
    }
}
