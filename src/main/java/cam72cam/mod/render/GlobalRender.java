package cam72cam.mod.render;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemBase;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Hand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(Side.CLIENT)
public class GlobalRender {
    private static List<Consumer<Float>> renderFuncs = new ArrayList<>();
    private static List<Consumer<Float>> overlayFuncs = new ArrayList<>();
    private static Map<ItemBase, MouseoverEvent> itemMouseovers = new HashMap<>();

    @FunctionalInterface
    public interface MouseoverEvent {
        void render(Player player, ItemStack stack, Vec3i pos, Vec3d offset, float partialTicks);
    }

    @SubscribeEvent
    public static void registerGlobalRenderer(RegistryEvent.Register<EntityEntry> event) {
        ClientRegistry.bindTileEntitySpecialRenderer(GlobalRenderHelper.class, new TileEntitySpecialRenderer<GlobalRenderHelper>() {
            @Override
            public void render(GlobalRenderHelper te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
                renderFuncs.forEach(r -> r.accept(partialTicks));
            }
        });
    }
    @SubscribeEvent
    public static void onRenderMouseover(DrawBlockHighlightEvent event) {
        Player player = MinecraftClient.getPlayer();

        if (event.getTarget().typeOfHit == RayTraceResult.Type.BLOCK) {
            Vec3i pos = new Vec3i(event.getTarget().getBlockPos());
            for (ItemBase item : itemMouseovers.keySet()) {
                if (item.internal == player.getHeldItem(Hand.PRIMARY).internal.getItem()) {
                    itemMouseovers.get(item).render(player, player.getHeldItem(Hand.PRIMARY), pos, new Vec3d(event.getTarget().hitVec), event.getPartialTicks());
                }
            }
        }
    }
    @SubscribeEvent
    public static void onOverlayEvent(RenderGameOverlayEvent.Pre event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            overlayFuncs.forEach(x -> x.accept(event.getPartialTicks()));
        }
    }

    public static void registerRender(Consumer<Float> func) {
        renderFuncs.add(func);
    }
    public static void registerOverlay(Consumer<Float> func) {
        overlayFuncs.add(func);
    }
    public static void registerItemMouseover(ItemBase item, MouseoverEvent fn) {
        itemMouseovers.put(item, fn);
    }

    public static boolean isTransparentPass() {
        return MinecraftForgeClient.getRenderPass() != 0;
    }


    private static TileEntity grh = new GlobalRenderHelper();
    private static List<TileEntity> grhList = new ArrayList<>();
    static {
        grhList.add(grh);
    }
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        Minecraft.getMinecraft().renderGlobal.updateTileEntities(grhList, grhList);
    }

    public static class GlobalRenderHelper extends TileEntity {

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

    @SubscribeEvent
    public static void onDebugRender(RenderGameOverlayEvent.Text event) {
        if (Minecraft.getMinecraft().gameSettings.showDebugInfo && GPUInfo.hasGPUInfo()) {
            int i;
            for (i = 0; i < event.getRight().size(); i++) {
                if (event.getRight().get(i).startsWith("Display: ")) {
                    i++;
                    break;
                }
            }
            event.getRight().add(i, GPUInfo.debug());
        }
    }
}
