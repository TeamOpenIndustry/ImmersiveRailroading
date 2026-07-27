package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.remotecontrol.RemoteControlData;
import cam72cam.immersiverailroading.remotecontrol.WirelessRemotecontrolClient;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;

public class RemoteControlSyncPacket extends Packet {
	@TagField("data")
    private RemoteControlData data;

    public RemoteControlSyncPacket() {
    }

    public RemoteControlSyncPacket(RemoteControlData data) {
        this.data = data;
    }

    @Override
    protected void handle() {
    	WirelessRemotecontrolClient.updateData(data);
    }
}
