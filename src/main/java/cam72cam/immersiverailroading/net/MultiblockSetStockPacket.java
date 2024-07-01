package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.multiblock.CustomTransporterMultiblock;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;

public class MultiblockSetStockPacket extends Packet {
    @TagField
    private String name;

    @TagField
    private CustomTransporterMultiblock.MultiblockStorager pack;

    public MultiblockSetStockPacket(String name, CustomTransporterMultiblock.MultiblockStorager packet) {
        this.name = name;
        this.pack = packet;
    }

    public MultiblockSetStockPacket() {

    }

    @Override
    protected void handle() {
        pack.setTargetTank(name);
    }
}
