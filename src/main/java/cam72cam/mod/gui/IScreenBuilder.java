package cam72cam.mod.gui;

import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.resource.Identifier;

public interface IScreenBuilder {
    void close();
    void addButton(Button btn);
    int getWidth();
    int getHeight();

    void drawImage(Identifier tex, int x, int y, int width, int height);

    void drawTank(double x, int y, double width, double height, Fluid fluid, float fluidPercent, boolean background, int color);

    void drawCenteredString(String str, int x, int y, int color);

    void show();

    void addTextField(TextField textField);
}
