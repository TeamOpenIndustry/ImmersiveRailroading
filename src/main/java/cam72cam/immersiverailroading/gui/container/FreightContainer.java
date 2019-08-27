package cam72cam.immersiverailroading.gui.container;

import cam72cam.immersiverailroading.entity.CarFreight;
import cam72cam.mod.gui.container.IContainer;

public class FreightContainer {
    public CarFreight stock;

    public FreightContainer(CarFreight stock) {
        this.stock = stock;
    }

    public void draw(IContainer container){
        int currY = 0;
        currY = container.drawTopBar(0, currY, stock.getInventoryWidth());
        currY = container.drawSlotBlock(stock.cargoItems, 0, stock.getInventoryWidth(), 0, currY);
        currY = container.drawPlayerInventoryConnector(0, currY, stock.getInventoryWidth());
        currY = container.drawPlayerInventory(currY, stock.getInventoryWidth());
    }
}
