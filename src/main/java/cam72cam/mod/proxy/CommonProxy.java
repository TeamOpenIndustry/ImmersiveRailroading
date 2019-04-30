package cam72cam.mod.proxy;

import cam72cam.mod.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber()
public class CommonProxy {

    /* World Hooks */

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        event.getWorld().addEventListener(new WorldListener(World.load(event.getWorld())));
    }
    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        World.unload(event.getWorld());
    }
}
