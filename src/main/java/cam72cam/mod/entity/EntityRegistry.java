package cam72cam.mod.entity;

import cam72cam.mod.ModCore;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.world.World;
import cam72cam.mod.resource.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class EntityRegistry {
    private static final Map<Class<? extends Entity>, String> identifiers = new HashMap<>();
    private static final Map<String, Supplier<Entity>> constructors = new HashMap<>();
    private static final Map<String, EntitySettings> registered = new HashMap<>();

    private EntityRegistry() {

    }
    public static void register(Class<? extends ModCore.Mod> cls, Supplier<Entity> ctr, EntitySettings settings, int distance) {
        Entity tmp = ctr.get();
        Class<? extends Entity> type = tmp.getClass();

        ModCore.onInit(cls, mod -> {
            Identifier id = new Identifier(mod.modID(), type.getSimpleName());

            // This has back-compat for older entity names
            // TODO expose updateFreq and vecUpdates
            net.minecraftforge.fml.common.registry.EntityRegistry.registerModEntity(id.internal, ModdedEntity.class, type.getSimpleName(), constructors.size(), mod.instance, distance, 20, false);

            identifiers.put(type, id.toString());
            constructors.put(id.toString(), ctr);
            registered.put(id.toString(), settings);
        });
    }
    public static EntitySettings getSettings(String type) {
        return registered.get(type);
    }

    public static Supplier<Entity> getConstructor(String type) {
        return constructors.get(type);
    }

    protected static Entity create(String type, ModdedEntity base) {
        return getConstructor(type).get().setup(base);
    }

    public static Entity create(World world, Class<? extends Entity> cls) {
        //TODO null checks
        ModdedEntity ent = new ModdedEntity(world.internal);
        String id = identifiers.get(cls);
        ent.init(id);
        return ent.getSelf();
    }

    private static String missingResources;

    @EventBusSubscriber
    public static class EntityEvents {
        @SubscribeEvent
        public static void onEntityJoin(EntityJoinWorldEvent event) {
            if (World.get(event.getWorld()) == null) {
                return;
            }


            if (event.getEntity() instanceof ModdedEntity) {
                String msg = ((ModdedEntity)event.getEntity()).getSelf().tryJoinWorld();
                if (msg != null) {
                    event.setCanceled(true);
                    missingResources = msg;
                }
            }
        }
    }

    @EventBusSubscriber(Side.CLIENT)
    public static class EntityClientEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (missingResources != null && !Minecraft.getMinecraft().isSingleplayer()) {
                Minecraft.getMinecraft().getConnection().getNetworkManager().closeChannel(PlayerMessage.direct(missingResources).internal);
                Minecraft.getMinecraft().loadWorld(null);
                Minecraft.getMinecraft().displayGuiScreen(new GuiDisconnected(new GuiMultiplayer(new GuiMainMenu()), "disconnect.lost", PlayerMessage.direct(missingResources).internal));
                missingResources = null;
            }
        }
    }
}
