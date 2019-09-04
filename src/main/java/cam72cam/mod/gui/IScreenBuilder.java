package cam72cam.mod.gui;

import cam72cam.immersiverailroading.library.CraftingType;
import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.resource.Identifier;

import java.util.function.Consumer;

public interface IScreenBuilder {
    void close();
    Button addButton(Button btn);
    int getWidth();
    int getHeight();

    void drawImage(Identifier tex, int x, int y, int width, int height);

    void drawTank(double x, int y, double width, double height, Fluid fluid, float fluidPercent, boolean background, int color);

    void drawCenteredString(String str, int x, int y, int color);

    void showCraftPicker(ItemStack currentItem, CraftingType casting, Consumer<ItemStack> o);
}
