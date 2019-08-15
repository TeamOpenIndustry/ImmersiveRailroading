package cam72cam.mod.render;

import cam72cam.mod.math.Vec3d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Mod.EventBusSubscriber
public class GlobalRender {
    private static List<Consumer<Float>> renderFuncs = new ArrayList<>();

    @SubscribeEvent
    public static void registerGlobalRenderer(RegistryEvent.Register<EntityEntry> event) {
        ClientRegistry.bindTileEntitySpecialRenderer(LargeEntityRenderHelper.class, new TileEntitySpecialRenderer<LargeEntityRenderHelper>() {
            @Override
            public void render(LargeEntityRenderHelper te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
                renderFuncs.forEach(r -> r.accept(partialTicks));
            }
        });
    }

    public static void registerRender(Consumer<Float> func) {
        renderFuncs.add(func);
    }

    public static boolean isTransparentPass() {
        return MinecraftForgeClient.getRenderPass() != 0;
    }


    private static TileEntity lerh = new LargeEntityRenderHelper();
    private static List<TileEntity> lerhList = new ArrayList<>();
    static {
        lerhList.add(lerh);
    }
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        Minecraft.getMinecraft().renderGlobal.updateTileEntities(lerhList, lerhList);
    }

    public static class LargeEntityRenderHelper extends TileEntity {

        public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox() {
            return INFINITE_EXTENT_AABB;
        }

        public double getDistanceSq(double x, double y, double z) {
            return 1;
        }

        public boolean shouldRenderInPass(int pass) {
            return true;
        }

    }

    public static Vec3d getCameraPos(float partialTicks) {
        net.minecraft.entity.Entity playerrRender = Minecraft.getMinecraft().getRenderViewEntity();
        double d0 = playerrRender.lastTickPosX + (playerrRender.posX - playerrRender.lastTickPosX) * partialTicks;
        double d1 = playerrRender.lastTickPosY + (playerrRender.posY - playerrRender.lastTickPosY) * partialTicks;
        double d2 = playerrRender.lastTickPosZ + (playerrRender.posZ - playerrRender.lastTickPosZ) * partialTicks;
        return new Vec3d(d0, d1, d2);
    }

    static ICamera getCamera(float partialTicks) {
        ICamera camera = new Frustum();
        Vec3d cameraPos = getCameraPos(partialTicks);
        camera.setPosition(cameraPos.x, cameraPos.y, cameraPos.z);
        return camera;
    }
}
