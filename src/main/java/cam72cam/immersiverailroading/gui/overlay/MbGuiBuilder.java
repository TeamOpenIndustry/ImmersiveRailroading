package cam72cam.immersiverailroading.gui.overlay;

import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.multiblock.CustomTransporterMultiblock;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.immersiverailroading.util.MergedBlocks;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.net.Packet;
import cam72cam.mod.render.opengl.BlendMode;
import cam72cam.mod.render.opengl.DirectDraw;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.TagField;
import util.Matrix4;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class MbGuiBuilder {
    private final float x;
    private final float y;
    private final Horizontal screen_x;
    private final Vertical screen_y;

    private final Identifier image;
    private final int width;
    private final int height;

    private final String text;
    private final float textHeight;

    private final Map<Float, Integer> colors = new HashMap<>();

    private final List<MbGuiBuilder> elements;

    private float temporary_value = 0.5f;

    private enum Horizontal {
        LEFT,
        RIGHT,
        MIDDLE,
        ;

        public static Horizontal from(String pos_x) {
            if (pos_x == null) {
                return LEFT;
            }
            pos_x = pos_x.toUpperCase(Locale.ROOT);
            if (pos_x.equals("CENTER")) {
                return MIDDLE;
            }
            return valueOf(pos_x);
        }
    }
    private enum Vertical {
        TOP,
        MIDDLE,
        BOTTOM,
        ;
        public static Vertical from(String pos_y) {
            if (pos_y == null) {
                return TOP;
            }
            pos_y = pos_y.toUpperCase(Locale.ROOT);
            if (pos_y.equals("CENTER")) {
                return MIDDLE;
            }

            return valueOf(pos_y);
        }
    }

    private static DataBlock processImports(DataBlock data) throws IOException {
        List<DataBlock.Value> direct = data.getValues("import");
        if (direct != null) {
            for (DataBlock.Value imp : direct) {
                data = new MergedBlocks(data, processImports(DataBlock.load(imp.asIdentifier())));
            }
        }
        List<DataBlock> imports = data.getBlocks("import");
        if (imports != null) {
            for (DataBlock imp : imports) {
                data = new MergedBlocks(data, processImports(DataBlock.load(imp.getValue("source").asIdentifier(), imp.getBlock("replace"))));
            }
        }
        return data;
    }

    protected MbGuiBuilder(DataBlock data) throws IOException {
        data = processImports(data);

        // common stuff
        this.x = data.getValue("x").asFloat(0f);
        this.y = data.getValue("y").asFloat(0f);
        this.screen_x = Horizontal.from(data.getValue("screen_x").asString());
        this.screen_y = Vertical.from(data.getValue("screen_y").asString());

        // Text stuff
        DataBlock txt = data.getBlock("text");
        if (txt != null) {
            text = txt.getValue("value").asString();
            textHeight = txt.getValue("height").asFloat(0f);
        } else {
            text = null;
            textHeight = 0;
        }

        // Image stuff
        this.image = data.getValue("image").asIdentifier(null);
        if (image != null) {
            BufferedImage tmp = ImageIO.read(this.image.getResourceStream());
            width = tmp.getWidth();
            height = tmp.getHeight();
        } else if (text != null) {
            width = (int) (textHeight/4 * text.length()); // Guesstimate
            height = (int) textHeight;
        } else {
            width = 0;
            height = 0;
        }

        DataBlock color = data.getBlock("color");
        if (color != null) {
            color.getValueMap().forEach((key, value) -> {
                String hex = value.asString();
                if (hex.length() == 8) {
                    hex = hex.replace("0x", "0xFF");
                    hex = hex.replace("0X", "0XFF");
                }
                colors.put(Float.parseFloat(key), (int)(long)Long.decode(hex));
            });
        }

        elements = new ArrayList<>();

        // Children
        List<DataBlock> elem = data.getBlocks("elements");
        if (elem == null) {
            elem = data.getBlocks("element");
        }
        if (elem != null) {
            for (DataBlock element : elem) {
                elements.add(new MbGuiBuilder(element));
            }
        }
    }

    public static MbGuiBuilder parse(Identifier overlay) throws IOException {
        return new MbGuiBuilder(DataBlock.load(overlay));
    }

    private void applyPosition(Matrix4 matrix, int maxx, int maxy) {
        matrix.translate(this.x, this.y, 0);

        switch (screen_x) {
            case LEFT:
                // NOP
                break;
            case MIDDLE:
                matrix.translate(maxx/2f, 0, 0);
                break;
            case RIGHT:
                matrix.translate(maxx, 0, 0);
                break;
        }
        switch (screen_y) {
            case TOP:
                // NOP
                break;
            case MIDDLE:
                matrix.translate(0, maxy/2f, 0);
                break;
            case BOTTOM:
                matrix.translate(0, maxy, 0);
                break;
        }
    }

    public void render(RenderState state, CustomTransporterMultiblock.TransporterMbInstance instance) {
        render(instance, state.clone().color(1, 1, 1, 1), GUIHelpers.getScreenWidth(), GUIHelpers.getScreenHeight(), 0xFFFFFFFF);
    }

    private void render(CustomTransporterMultiblock.TransporterMbInstance instance, RenderState state, int maxx, int maxy, int baseColor) {
        state = state.clone();

        if (image != null) {
            DirectDraw draw = new DirectDraw();
            draw.vertex(0, 0, 0).uv(0, 0);
            draw.vertex(0, height, 0).uv(0, 1);
            draw.vertex(width, height, 0).uv(1, 1);
            draw.vertex(width, 0, 0).uv(1, 0);
            draw.draw(state.clone()
                    .texture(Texture.wrap(image))
                    .alpha_test(false)
                    .blend(new BlendMode(BlendMode.GL_SRC_ALPHA, BlendMode.GL_ONE_MINUS_SRC_ALPHA))
            );
        }
        if (text != null) {
            String out = text;
            for (Stat stat : Stat.values()) {
                if (out.contains(stat.toString())) {
//                    out = out.replace(stat.toString(), stat.getValue(stock));
                }
            }
            for (GuiText label : new GuiText[]{GuiText.LABEL_THROTTLE, GuiText.LABEL_REVERSER, GuiText.LABEL_BRAKE}) {
                out = out.replace(label.getValue(), label.toString());
            }
            // Text is 8px tall
            float scale = textHeight / 8f;
            Matrix4 mat = state.model_view().copy();
            mat.scale(scale, scale, scale);
            GUIHelpers.drawCenteredString(out, 0, 0, baseColor, mat);
        }
        for (MbGuiBuilder element : elements) {
            element.render(instance, state, maxx, maxy, baseColor);
        }
    }

    public static class ControlChangePacket extends Packet {
        @TagField
        private UUID stockUUID;
        @TagField(typeHint = Readouts.class)
        private Readouts readout;
        @TagField
        private String controlGroup;
        @TagField
        private boolean global;
        @TagField
        private float value;

        @TagField
        private String texture_variant;
        public ControlChangePacket() {
            super(); // Reflection
        }

        public ControlChangePacket(EntityRollingStock stock, Readouts readout, String controlGroup, boolean global, String texture_variant, float value) {
            this.stockUUID = stock.getUUID();
            this.readout = readout;
            this.controlGroup = controlGroup;
            this.global = global;
            this.texture_variant = texture_variant;
            this.value = value;
            // Update the UI, server will resync once the update actually happens
            update(stock);
        }

        @Override
        protected void handle() {
            EntityRollingStock stock = getWorld().getEntity(stockUUID, EntityRollingStock.class);
            if (stock != null) {
                update(stock);
            }
        }
        public void update(EntityRollingStock stock) {
            // TODO permissions!
            if (controlGroup != null) {
                switch (controlGroup) {
                    case "REVERSERFORWARD":
                        readout = Readouts.REVERSER;
                        value = 1;
                        break;
                    case "REVERSERNEUTRAL":
                        readout = Readouts.REVERSER;
                        value = 0.5f;
                        break;
                    case "REVERSERBACKWARD":
                        readout = Readouts.REVERSER;
                        value = 0;
                        break;
                    default:
                        if (global) {
                            ((EntityCoupleableRollingStock)stock).mapTrain((EntityCoupleableRollingStock) stock, false, target -> {
                                target.setControlPosition(controlGroup, value);
                            });
                        } else {
                            stock.setControlPosition(controlGroup, value);
                        }
                        return;
                }
            }
            if (readout != null) {
                if (global) {
                    ((EntityCoupleableRollingStock)stock).mapTrain((EntityCoupleableRollingStock) stock, false, target -> {
                        readout.setValue(target, value);
                    });
                } else {
                    readout.setValue(stock, value);
                }
            }
            if (texture_variant != null && value == 1) {
                stock.setTexture(texture_variant);
            }
        }
    }
}
