package cam72cam.immersiverailroading.gui.container;

import cam72cam.immersiverailroading.library.MultiblockTypes;
import cam72cam.immersiverailroading.multiblock.CustomCrafterMultiblock;
import cam72cam.immersiverailroading.multiblock.CustomTransporterMultiblock;
import cam72cam.immersiverailroading.registry.MultiblockDefinition;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.gui.container.IContainer;
import cam72cam.mod.gui.container.IContainerBuilder;
import cam72cam.mod.model.obj.Vec2f;

import java.util.ArrayList;
import java.util.List;

public class CustomMultiblockGui implements IContainer {
    private final TileMultiblock tile;
    private final MultiblockDefinition def;
    private final List<Vec2f> slots;

    public CustomMultiblockGui(TileMultiblock tile) {
        MultiblockDefinition def1;
        this.tile = tile;
        this.slots = new ArrayList<>();
        try {
            def1 = ((CustomCrafterMultiblock.CrafterMbInstance) tile.getMultiblock()).def;
            //TODO Improve parser
            List<DataBlock> values = def1.gui.getBlocks("slot");
            values.forEach(block -> slots.add(new Vec2f(block.getValue("x").asInteger(), block.getValue("y").asInteger())));
        }catch (ClassCastException e){
            def1 = ((CustomTransporterMultiblock.TransporterMbInstance) tile.getMultiblock()).def;
        }
        this.def = def1;
    }

    @Override
    public void draw(IContainerBuilder container) {
        if (this.def.type == MultiblockTypes.CRAFTER) {

            int currY = 0;
            for (int i = 0; i < slots.size(); i++) {
                Vec2f vec2f = slots.get(i);
                container.drawSlot(tile.getContainer(), i, (int) vec2f.x, (int) vec2f.y);
                currY = Math.max(currY, (int) vec2f.y);
            }
            container.drawPlayerInventory(currY + 7, 0);

        } else if (this.def.type == MultiblockTypes.TRANSPORTER) {
            int currY = 0;
            if (def.tankCapability != 0) {
                currY = container.drawTopBar(0, currY, getSlotsX());
                int slotHeight = (int) Math.ceil(def.tankCapability / 500d / getSlotsX());

                int tankY = currY;
                int slotY = 1;
                for (int i = 0; i < slotHeight; i++) {
                    currY = container.drawSlotRow(null, 0, getSlotsX(), 0, currY);
                    if (i == (slotHeight - 1) / 2)
                        slotY = currY;
                }

                container.drawTankBlock(0, tankY, getSlotsX(), 4,
                        tile.getFluidContainer().getContents().getFluid(),
                        tile.getFluidContainer().getContents().getAmount() / (float) def.tankCapability);

                String quantityStr = String.format("%s/%s", tile.getFluidContainer().getContents().getAmount(), def.tankCapability);
                container.drawCenteredString(quantityStr, 0, slotY);
            } else {
                currY = container.drawTopBar(0, currY, getSlotsX());
            }
            if (tile.getContainer().getSlotCount() != 0) {
                currY = container.drawSlotBlock(tile.getContainer(), 0, getSlotsX(), 0, currY);
                currY = container.drawPlayerInventoryConnector(0, currY, getSlotsX());
            } else {
                currY = container.drawPlayerInventoryConnector(0, currY, getSlotsX());
            }
            currY = container.drawPlayerInventory(currY, getSlotsX());
        }
    }

    @Override
    public int getSlotsX() {
        switch (this.def.type) {
            case TRANSPORTER:
                return this.def.inventoryWidth;
            case CRAFTER:
            default:
                return 0;
        }
    }

    @Override
    public int getSlotsY() {
        switch (this.def.type) {
            case TRANSPORTER:
                return this.def.inventoryHeight;
            case CRAFTER:
            default:
                return 0;
        }
    }
}
