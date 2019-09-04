package cam72cam.mod.gui;

import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.util.Hand;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ScreenBuilder extends GuiScreen implements IScreenBuilder {
    private final IScreen screen;
    private Map<GuiButton, Button> buttonMap = new HashMap<>();
    private List<GuiTextField> textFields = new ArrayList<>();

    public ScreenBuilder(IScreen screen) {
        this.screen = screen;
    }

    // IScreenBuilder

    @Override
    public void close() {
        screen.onClose();

        this.mc.displayGuiScreen(null);
        if (this.mc.currentScreen == null) {
            this.mc.setIngameFocus();
        }
    }

    @Override
    public void addButton(Button btn) {
        super.buttonList.add(btn.internal());
        this.buttonMap.put(btn.internal(), btn);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void drawImage(Identifier tex, int x, int y, int width, int height) {
        this.mc.getTextureManager().bindTexture(tex.internal);

        GUIHelpers.texturedRect(this.width / 2 + x, this.height / 4 + y, width, height);
    }

    @Override
    public void drawTank(double x, int y, double width, double height, Fluid fluid, float fluidPercent, boolean background, int color) {
        GUIHelpers.drawTankBlock(this.width/2 + x, this.height/2 + y, width, height, fluid, fluidPercent, background, color);
    }

    @Override
    public void drawCenteredString(String str, int x, int y, int color) {
        super.drawCenteredString(this.fontRenderer, str, this.width/2 + x, this.height/4 + y, color);
    }

    @Override
    public void show() {
        this.mc.displayGuiScreen(this);
    }

    @Override
    public void addTextField(TextField textField) {
        this.textFields.add(textField.internal());
    }

    // GuiScreen

    @Override
    public void initGui() {
        screen.init(this);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        for (Button btn : buttonMap.values()) {
            btn.onUpdate();
        }

        screen.draw(this);

        textFields.forEach(GuiTextField::drawTextBox);

        // draw buttons
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            close();
        }

        // Enter
        if (keyCode == 28 || keyCode == 156) {
            screen.onEnterKey(this);
        }

        this.textFields.forEach(x -> x.textboxKeyTyped(typedChar, keyCode));
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        // Copy pasta to support right / left button click

        for (int i = 0; i < this.buttonList.size(); ++i)
        {
            GuiButton guibutton = super.buttonList.get(i);

            if (guibutton.mousePressed(this.mc, mouseX, mouseY))
            {
                this.selectedButton = guibutton;
                guibutton.playPressSound(this.mc.getSoundHandler());
                buttonMap.get(guibutton).onClick(mouseButton == 0 ? Hand.PRIMARY : Hand.SECONDARY);
            }
        }

        this.textFields.forEach(x -> x.mouseClicked(mouseX, mouseY, mouseButton));
    }

    // Default overrides
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
