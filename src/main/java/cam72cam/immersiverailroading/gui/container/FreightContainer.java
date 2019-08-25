package cam72cam.immersiverailroading.gui.container;

import cam72cam.immersiverailroading.entity.CarFreight;
import cam72cam.mod.gui.container.IContainer;

public class FreightContainer {
    public CarFreight freight;
    protected int numRows;

    public FreightContainer(CarFreight stock) {
        this.freight = stock;
        int horizSlots = stock.getInventoryWidth();
        this.numRows = (int) Math.ceil((double)stock.getInventorySize() / horizSlots);
    }

    public void draw(IContainer container){
        int currY = 0;
        currY = container.drawTopBar(0, currY, freight.getInventoryWidth());
        currY = container.drawSlotBlock(freight.cargoItems, freight.getInventorySize(), 0, currY, freight.getInventoryWidth());
        currY = container.drawPlayerInventoryConnector(0, currY, freight.getInventoryWidth());
        currY = container.drawPlayerInventory(currY, freight.getInventoryWidth());
    }
}
