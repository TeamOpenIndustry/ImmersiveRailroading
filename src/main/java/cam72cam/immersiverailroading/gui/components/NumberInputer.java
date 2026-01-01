package cam72cam.immersiverailroading.gui.components;

import cam72cam.immersiverailroading.util.MathUtil;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.screen.Button;
import cam72cam.mod.gui.screen.IScreenBuilder;
import cam72cam.mod.gui.screen.Slider;
import cam72cam.mod.gui.screen.TextField;

import java.util.function.Consumer;

public class NumberInputer {
    private static final int DEFAULT_WIDTH = 150;
    private static final int DEFAULT_HEIGHT = 20;

    public final TextField number;
    public final Slider slider;
    public final Button btn;

    private double content;
    private final double min;
    private final double max;
    private final boolean doublePrecision;
    private final boolean useLogScale;
    private final String prefix;
    private final String suffix;
    private final Consumer<Double> consumer;
    private boolean showingSlider;

    public NumberInputer(IScreenBuilder builder, int x, int y,
                         String label,
                         double min, double max, double start,
                         boolean doublePrecision, Consumer<Double> callback) {
        this(builder, x, y, DEFAULT_WIDTH, DEFAULT_HEIGHT, label, "", min, max, start, doublePrecision, false, callback);
    }

    public NumberInputer(IScreenBuilder builder, int x, int y,
                         String label,
                         double min, double max, double start,
                         boolean doublePrecision, boolean useLogScale, Consumer<Double> callback) {
        this(builder, x, y, DEFAULT_WIDTH, DEFAULT_HEIGHT, label, "", min, max, start, doublePrecision, useLogScale, callback);
    }

    public NumberInputer(IScreenBuilder builder, int x, int y, int width, int height,
                         String label,
                         double min, double max, double start,
                         boolean doublePrecision,
                         Consumer<Double> callback) {
        this(builder, x, y, width, height, label, "", min, max, start, doublePrecision, false, callback);
    }

    public NumberInputer(IScreenBuilder builder, int x, int y, int width, int height,
                         String prefix, String suffix,
                         double min, double max, double start,
                         boolean doublePrecision, Consumer<Double> callback) {
        this(builder, x, y, width, height, prefix, suffix, min, max, start, doublePrecision, false, callback);
    }

    public NumberInputer(IScreenBuilder builder, int x, int y, int width, int height,
                         String prefix, String suffix,
                         double min, double max, double start,
                         boolean doublePrecision, boolean useLogScale, Consumer<Double> callback) {
        if (width <= 2 * height) {
            throw new IllegalStateException("Width must be greater than 2 * height");
        }

        this.consumer = callback;
        this.min = min;
        this.max = max;
        this.doublePrecision = doublePrecision;
        this.useLogScale = useLogScale;
        this.prefix = prefix;
        this.suffix = suffix;
        this.content = MathUtil.clamp(start, min, max);
        this.showingSlider = false;

        number = new TextField(builder, x + 2, y + 2, width - height - 4, height - 4);
        number.setValidator(this::validateInput);

        double sliderMin = useLogScale ? Math.log10(min) : min;
        double sliderMax = useLogScale ? Math.log10(max) : max;
        double sliderStart = useLogScale ? Math.log10(content) : content;

        slider = new Slider(builder, x, y, width - height, height, prefix,
                            sliderMin, sliderMax, sliderStart, doublePrecision,
                            s -> {
                                content = useLogScale ? Math.pow(10, s.getValue()) : s.getValue();
                                s.setText(this.prefix + formatValue(content) + this.suffix);
                                consumer.accept(doublePrecision ? content : Math.round(content));
                            });

        btn = new Button(builder, x + width - height, y, height, height, "↺",
                         (hand, button) -> {
                             showingSlider = !showingSlider;

                             if (showingSlider) {
                                 number.setVisible(false);
                                 slider.setVisible(true);
                                 slider.setValue(NumberInputer.this.useLogScale ? Math.log10(content) : content);
                                 slider.onSlider();
                             } else {
                                 slider.setVisible(false);
                                 number.setVisible(true);
                                 number.setText(formatValue(content));
                             }
                         });

        btn.setVisible(true);
        btn.onClick(Player.Hand.PRIMARY);
        number.setText(formatValue(content));
    }

    private boolean validateInput(String s) {
        try {
            if (doublePrecision) {
                String toValidate = s.endsWith(".") && !s.substring(0, s.length() - 1).contains(".")
                                    ? s.substring(0, s.length() - 1)
                                    : s;
                double d = Double.parseDouble(toValidate);
                if (d < min || d > max) return false;
                content = d;
            } else {
                int i = Integer.parseInt(s);
                if (i < min || i > max) return false;
                content = i;
            }
            consumer.accept(content);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String formatValue(double value) {
        return String.format(doublePrecision ? "%.4f" : "%.0f", value);
    }

    public void setVisible(boolean visible) {
        if (visible) {
            showingSlider = !showingSlider;
            btn.setVisible(true);
            btn.onClick(Player.Hand.PRIMARY);
        } else {
            btn.setVisible(false);
            number.setVisible(false);
            slider.setVisible(false);
        }
    }

    public double getValue() {
        return content;
    }

    public void setValue(double value) {
        content = MathUtil.clamp(value, min, max);
        slider.setValue(useLogScale ? Math.log10(content) : content);
        number.setText(formatValue(content));
        consumer.accept(content);
    }
}