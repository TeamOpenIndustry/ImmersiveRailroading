package cam72cam.immersiverailroading.gui.container;

import cam72cam.immersiverailroading.entity.CarTank;
import cam72cam.mod.gui.container.IContainer;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;

public class TankContainer {
    public final CarTank stock;
    private final ItemStack template;

    public TankContainer(CarTank tank) {
        this.stock = tank;
        this.template = Fuzzy.BUCKET.example();
    }

    public void draw(IContainer container){
        int currY = 0;
        int horizSlots = 10;
        int inventoryRows = 4;
        int slotY = 0;

        currY = container.drawTopBar(0, currY, horizSlots);

        int tankY = currY;
        for (int i = 0; i < 4; i++) {
            currY = container.drawSlotRow(null, 0, horizSlots, 0, currY);
            if (i == 0) {
                slotY = currY;
            }
        }

        container.drawTankBlock(0, tankY, horizSlots, inventoryRows, stock.getLiquid(), stock.getLiquidAmount() / (float) stock.getTankCapacity().MilliBuckets());

        container.drawSlotOverlay(template, 1, slotY);
        container.drawSlot(stock.cargoItems, 0, 1, slotY);
        container.drawSlot(stock.cargoItems, 1,  1 + horizSlots * 16, slotY);

        String quantityStr = String.format("%s/%s", stock.getLiquidAmount(), stock.getTankCapacity().MilliBuckets());
        container.drawCenteredString(quantityStr, 0, slotY);

        currY = container.drawPlayerInventoryConnector(0, currY, horizSlots);
        currY = container.drawPlayerInventory(currY, horizSlots);

    }
}
