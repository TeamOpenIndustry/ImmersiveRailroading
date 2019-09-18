package cam72cam.mod.render;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.world.World;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(Side.CLIENT)
public class EntityRenderer extends Render<ModdedEntity> {
    private static Map<Class<? extends Entity>, IEntityRender> renderers = new HashMap<>();

    public EntityRenderer(RenderManager factory) {
        super(factory);
    }

    @Override
    public void doRender(ModdedEntity stock, double x, double y, double z, float entityYaw, float partialTicks) {
        Entity self = stock.getSelf();

        GL11.glPushMatrix();
        {
            GL11.glTranslated(x, y, z);
            GL11.glRotatef(180 - entityYaw, 0, 1, 0);
            GL11.glRotatef(self.getRotationPitch(), 1, 0, 0);
            GL11.glRotatef(-90, 0, 1, 0);
            renderers.get(self.getClass()).render(self, partialTicks);

            for (ModdedEntity.StaticPassenger pass : stock.getStaticPassengers()) {
                if (pass.cache == null) {
                    pass.cache = pass.reconstitute(stock.world);
                }
                Vec3d pos = stock.getRidingOffset(pass.uuid);
                if (pos == null) {
                    continue;
                }

                //TODO pos = pos.add(stock.getDefinition().getPassengerCenter(stock.gauge));
                //TileEntityMobSpawnerRenderer
                EntityLiving ent = (EntityLiving) pass.cache;
                GL11.glPushMatrix();
                {
                    GL11.glTranslated(pos.x, pos.y - 0.5 + 0.35, pos.z);
                    GL11.glRotated(pass.rotation, 0, 1, 0);
                    Minecraft.getMinecraft().getRenderManager().renderEntity(ent, 0, 0, 0, 0, 0, false);
                }
                GL11.glPopMatrix();
            }

        }
        GL11.glPopMatrix();

    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(ModdedEntity entity) {
        return null;
    }

    //TODO client only
    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        RenderingRegistry.registerEntityRenderingHandler(ModdedEntity.class, manager -> new EntityRenderer(manager));
    }

    public static void register(Class<? extends Entity> type, IEntityRender render) {
        renderers.put(type, render);
    }

    static {
        GlobalRender.registerRender(EntityRenderer::renderLargeEntities);
    }

    private static void renderLargeEntities(float partialTicks) {
        if (GlobalRender.isTransparentPass()) {
            return;
        }

        Minecraft.getMinecraft().mcProfiler.startSection("large_entity_helper");

        ICamera camera = GlobalRender.getCamera(partialTicks);

        World world = MinecraftClient.getPlayer().getWorld();
        List<Entity> entities = world.getEntities(Entity.class);
        for (Entity entity : entities) {
            // Duplicate forge logic and render entity if the chunk is not rendered but entity is visible (MC entitysize issues/optimization)
            AxisAlignedBB chunk = new AxisAlignedBB(entity.getBlockPosition().toChunkMin().internal, entity.getBlockPosition().toChunkMax().internal);
            if (!camera.isBoundingBoxInFrustum(chunk) && camera.isBoundingBoxInFrustum(entity.internal.getRenderBoundingBox())) {
                Minecraft.getMinecraft().getRenderManager().renderEntityStatic(entity.internal, partialTicks, true);
            }
        }

        Minecraft.getMinecraft().mcProfiler.endSection();
    }
}
