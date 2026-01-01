package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.library.*;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui.screen.*;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.net.Packet;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.text.TextUtil;

import java.util.function.Function;

import static cam72cam.immersiverailroading.gui.ClickListHelper.next;

public class AugmentFilterGUI implements IScreen {
    private final Vec3i pos;
    private final Augment augment;
    private final Augment.Properties properties;
    private TextField includeTags;
    private TextField excludeTags;
    private TextField doorActuatorFilter;
    private Button stockDetectorMode;
    private Button redstoneMode;
    private CheckBox pushpull;
    private Button couplerMode;
    private Button locoControlMode;

    public AugmentFilterGUI(TileRailBase tileRailBase) {
        this.pos = tileRailBase.getPos();
        this.augment = tileRailBase.getAugment();
        this.properties = tileRailBase.getAugmentProperties() == null
                          ? Augment.Properties.EMPTY
                          : tileRailBase.getAugmentProperties();
    }

    @Override
    public void init(IScreenBuilder screen) {
        int xtop = -GUIHelpers.getScreenWidth() / 2;
        int ytop = -GUIHelpers.getScreenHeight() / 4;

        int xOffset = 0;
        int yOffset = 40;

        int buttonWidth = 220;
        int buttonHeight = 20;

        doorActuatorFilter = new TextField(screen, xtop + xOffset + 240, ytop + yOffset, buttonWidth - 50, buttonHeight);
        doorActuatorFilter.setText(properties.doorActuatorFilter);
        doorActuatorFilter.setVisible(augment.equals(Augment.ACTUATOR));
        doorActuatorFilter.setValidator(s -> {
            properties.doorActuatorFilter = s;
            return true;
        });

        includeTags = new TextField(screen, xtop + xOffset, ytop + yOffset, buttonWidth-1, buttonHeight);
        includeTags.setText(properties.positiveFilter);
        includeTags.setValidator(s -> {
            properties.positiveFilter = s;
            return true;
        });
        yOffset += 40;

        excludeTags = new TextField(screen, xtop + xOffset, ytop + yOffset, buttonWidth-1, buttonHeight);
        excludeTags.setText(properties.negativeFilter);
        excludeTags.setValidator(s -> {
            properties.negativeFilter = s;
            return true;
        });
        yOffset += 25;

        Function<Enum<?>, String> translate = e -> TextUtil.translate(e.toString());

        stockDetectorMode = new Button(screen, xtop + xOffset, ytop + yOffset, buttonWidth, buttonHeight, GuiText.SELECTOR_AUGMENT_DETECT + translate.apply(properties.stockDetectorMode)) {
            @Override
            public void onClick(Player.Hand hand) {
                properties.stockDetectorMode = next(properties.stockDetectorMode, Player.Hand.PRIMARY);
                stockDetectorMode.setText(GuiText.SELECTOR_AUGMENT_DETECT + translate.apply(properties.stockDetectorMode));
            }
        };
        stockDetectorMode.setEnabled(this.augment == Augment.DETECTOR);
        yOffset += 25;

        redstoneMode = new Button(screen, xtop + xOffset, ytop + yOffset, buttonWidth, buttonHeight, GuiText.SELECTOR_AUGMENT_REDSTONE + translate.apply(properties.redstoneMode)) {
            @Override
            public void onClick(Player.Hand hand) {
                properties.redstoneMode = next(properties.redstoneMode, Player.Hand.PRIMARY);
                redstoneMode.setText(GuiText.SELECTOR_AUGMENT_REDSTONE + translate.apply(properties.redstoneMode));
            }
        };
        redstoneMode.setEnabled(this.augment == Augment.COUPLER
                                || this.augment == Augment.ITEM_LOADER
                                || this.augment == Augment.ITEM_UNLOADER
                                || this.augment == Augment.FLUID_LOADER
                                || this.augment == Augment.FLUID_UNLOADER);
        yOffset += 25;

        pushpull = new CheckBox(screen, xtop + xOffset, ytop + yOffset, GuiText.SELECTOR_AUGMENT_PUSHPULL.toString(), properties.pushpull) {
            @Override
            public void onClick(Player.Hand hand) {
                properties.pushpull = !properties.pushpull;
                pushpull.setChecked(properties.pushpull);
            }
        };
        pushpull.setEnabled(this.augment == Augment.COUPLER
                            || this.augment == Augment.ITEM_LOADER
                            || this.augment == Augment.ITEM_UNLOADER
                            || this.augment == Augment.FLUID_LOADER
                            || this.augment == Augment.FLUID_UNLOADER);
        yOffset += 15;

        couplerMode = new Button(screen, xtop + xOffset, ytop + yOffset, buttonWidth, buttonHeight, GuiText.SELECTOR_AUGMENT_COUPLER + translate.apply(properties.couplerAugmentMode)) {
            @Override
            public void onClick(Player.Hand hand) {
                properties.couplerAugmentMode = next(properties.couplerAugmentMode, Player.Hand.PRIMARY);
                couplerMode.setText(GuiText.SELECTOR_AUGMENT_COUPLER + translate.apply(properties.couplerAugmentMode));
            }
        };
        couplerMode.setEnabled(this.augment == Augment.COUPLER);
        yOffset += 25;

        locoControlMode = new Button(screen, xtop + xOffset, ytop + yOffset, buttonWidth, buttonHeight, GuiText.SELECTOR_AUGMENT_CONTROL + translate.apply(properties.locoControlMode)) {
            @Override
            public void onClick(Player.Hand hand) {
                properties.locoControlMode = next(properties.locoControlMode, Player.Hand.PRIMARY);
                locoControlMode.setText(GuiText.SELECTOR_AUGMENT_CONTROL + translate.apply(properties.locoControlMode));
            }
        };
        locoControlMode.setEnabled(this.augment == Augment.LOCO_CONTROL);
    }

    @Override
    public void onEnterKey(IScreenBuilder builder) {
        builder.close();
    }

    @Override
    public void onClose() {
        if(properties.positiveFilter == null) {
            properties.positiveFilter = "";
        }
        if (properties.negativeFilter == null) {
            properties.negativeFilter = "";
        }
        if (properties.doorActuatorFilter == null) {
            properties.doorActuatorFilter = "";
        }
        new AugmentFilterChangePacket(pos, properties).sendToServer();
    }

    @Override
    public void draw(IScreenBuilder builder, RenderState state) {
        IScreen.super.draw(builder, state);

        GUIHelpers.drawRect(0, 0, GUIHelpers.getScreenWidth(), GUIHelpers.getScreenHeight(), 0x88000000);

        GUIHelpers.drawRect(0, 0, 220, GUIHelpers.getScreenHeight(), 0xCC000000);

        int xtop = -GUIHelpers.getScreenWidth() / 2;
        int ytop = -GUIHelpers.getScreenHeight() / 4;

        int xOffset = 110;
        int yOffset = 30;
        GUIHelpers.drawCenteredString(GuiText.LABEL_CURRENT_AUGMENT + this.augment.toString(), xOffset,  10, 0xFFFFFFFF);

        GUIHelpers.drawCenteredString(GuiText.LABEL_INCLUDED_TAG.toString(), xOffset,  yOffset, 0xFFFFFFFF);
        includeTags.setText(properties.positiveFilter);
        if (augment == Augment.ACTUATOR) {
            GUIHelpers.drawCenteredString(GuiText.LABEL_ACTUATOR_FILTER.toString(), xOffset + 210, yOffset, 0xFFFFFFFF);
            doorActuatorFilter.setText(properties.doorActuatorFilter);
        }
        yOffset+=40;

        GUIHelpers.drawCenteredString(GuiText.LABEL_EXCLUDED_TAG.toString(), xOffset,  yOffset, 0xFFFFFFFF);
        excludeTags.setText(properties.negativeFilter);
    }

    public static class AugmentFilterChangePacket extends Packet {
        @TagField
        Vec3i pos;

        @TagField
        Augment.Properties properties;

        public AugmentFilterChangePacket() {
        }

        public AugmentFilterChangePacket(Vec3i pos, Augment.Properties filter) {
            this.pos = pos;
            this.properties = filter;
        }

        @Override
        protected void handle() {
            TileRailBase railBase = this.getWorld().getBlockEntity(pos, TileRailBase.class);
            if (railBase != null && railBase.getAugment() != null) {
                railBase.setAugmentProperties(properties);
            }
        }
    }
}
