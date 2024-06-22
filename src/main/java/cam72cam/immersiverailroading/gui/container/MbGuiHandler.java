package cam72cam.immersiverailroading.gui.container;

import cam72cam.mod.event.CommonEvents;

import java.util.HashMap;
import java.util.UUID;

public class MbGuiHandler {
    public static final HashMap<UUID, Integer> playerGUi = new HashMap<>();

    public static void init(){
        CommonEvents.World.TICK.subscribe(world -> {
            for (UUID uuid : playerGUi.keySet()) {
                int i = playerGUi.get(uuid) - 1;
                if(i < 0) {
                    playerGUi.remove(uuid);
                }else{
                    playerGUi.put(uuid, i);
                }
            }
            System.out.println(playerGUi);
        });
    }
}
