package cam72cam.immersiverailroading.net;

import java.util.UUID;

import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.remotecontrol.WirelessRemotecontrolServer;
import cam72cam.mod.entity.Player;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.StrictTagMapper;
import cam72cam.mod.serialization.TagField;

public class RemoteControlClientPacket extends Packet {
	@TagField(value = "loco", mapper = StrictTagMapper.class)
    private UUID loco;
    @TagField(typeHint = Readouts.class)
    private Readouts readout;
    @TagField
    private float value;

	public RemoteControlClientPacket() {
	}
	
    public RemoteControlClientPacket(UUID loco, Readouts readout, float value) {
        this.loco = loco;
        this.readout = readout;
        this.value = value;
    }
	
	@Override
	protected void handle() {
		Player player = getPlayer();
        if (!WirelessRemotecontrolServer.isActive(player.getUUID(), loco)) {
            return;
        }
        if (!player.hasPermission(Permissions.LOCOMOTIVE_CONTROL)) {
            return;
        }

        Locomotive stock = getWorld().getEntity(loco, Locomotive.class);
        if (stock == null) {
            return;
        }
        readout.setValue(stock, value);
	}

}
