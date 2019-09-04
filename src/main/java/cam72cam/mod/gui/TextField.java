package cam72cam.mod.gui;

import com.google.common.base.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;

public class TextField {
    private final GuiTextField textfield;

    public TextField(IScreenBuilder builder, int x, int y, int width, int height) {
        this.textfield = create(builder, x, y, width, height);
        builder.addTextField(this);
    }
    GuiTextField create(IScreenBuilder builder, int x, int y, int width, int height) {
        return new GuiTextField(-1, Minecraft.getMinecraft().fontRenderer, builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, width, height);
    }
    GuiTextField internal() {
        return textfield;
    }

    public void setText(String s) {
        textfield.setText(s);
    }

    public void setValidator(Predicate<String> filter) {
        textfield.setValidator(filter);
    }

    public void setFocused(boolean b) {
        textfield.setFocused(b);
    }

    public String getText() {
        return textfield.getText();
    }
}
