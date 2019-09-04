package cam72cam.mod.gui;

import cam72cam.mod.util.Hand;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.fml.client.config.GuiSlider;

public abstract class Slider extends Button {

    private final boolean doublePrecision;
    private double min;
    private double max;
    private double start;

    public Slider(IScreenBuilder builder, int x, int y, String text, double min, double max, double start, boolean doublePrecision) {
        super(builder, x, y, 0, 0, text);
        this.min = min;
        this.max = max;
        this.start = start;
        this.doublePrecision = doublePrecision;
        ((GuiSlider)this.button).showDecimal = doublePrecision;
    }

    @Override
    GuiButton create(IScreenBuilder builder, int x, int y, int width, int height, String text) {
        return new GuiSlider(-1, builder.getWidth()/2 + x, builder.getHeight()/4 +y, text, min, max, start, null) {
            @Override
            public void updateSlider() {
                super.updateSlider();
                onSlider();
            }
        };
    }

    @Override
    public void onClick(Hand hand) {

    }

    public abstract void onSlider();

    public int getValueInt() {
        return ((GuiSlider)button).getValueInt();
    }
    public double getValue() {
        return ((GuiSlider)button).getValue();
    }
}
