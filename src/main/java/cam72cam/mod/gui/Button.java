package cam72cam.mod.gui;

import cam72cam.mod.util.Hand;
import net.minecraft.client.gui.GuiButton;

public abstract class Button {
    final GuiButton button;

    public Button(IScreenBuilder builder, int x, int y, String text) {
        this(builder, x, y, 200, 20, text);
    }

    public Button(IScreenBuilder builder, int x, int y, int width, int height, String text) {
        this.button = create(builder, x, y, width, height, text);
        builder.addButton(this);
    }

    GuiButton create(IScreenBuilder builder, int x, int y, int width, int height, String text) {
        return new GuiButton(-1, builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, width, height, text);
    }

    public void setText(String text) {
        button.displayString = text;
    }

    public abstract void onClick(Hand hand);

    GuiButton internal() {
        return button;
    }

    public void onUpdate() {

    }

    public void setTextColor(int i) {
        button.packedFGColour = i;
    }

    public void setVisible(boolean b) {
        button.visible = b;
    };
}
