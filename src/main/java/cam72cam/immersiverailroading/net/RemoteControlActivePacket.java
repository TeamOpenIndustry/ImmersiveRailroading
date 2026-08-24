package cam72cam.immersiverailroading.net;

import java.util.UUID;

import cam72cam.immersiverailroading.remotecontrol.WirelessRemotecontrolServer;
import cam72cam.mod.entity.Player;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.StrictTagMapper;
import cam72cam.mod.serialization.TagField;

public class RemoteControlActivePacket extends Packet {
	@TagField(value = "loco", mapper = StrictTagMapper.class)
    private UUID loco;
	
	public RemoteControlActivePacket() {
	}
	
	public RemoteControlActivePacket(UUID loco) {
		this.loco = loco;
	}

	@Override
	protected void handle() {
		Player player = getPlayer();
		WirelessRemotecontrolServer.setActive(player.getUUID(), loco);
	}

}
