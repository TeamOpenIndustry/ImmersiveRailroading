package cam72cam.immersiverailroading.net;

import java.util.UUID;

public class WirelessRemotecontrolInputHandler {
    
    private static UUID linkedUUID = null;

    public static void setTarget(UUID uuid) {
        linkedUUID = uuid;
    }

  
    public static UUID getTarget() {
        return linkedUUID;
    }

}
