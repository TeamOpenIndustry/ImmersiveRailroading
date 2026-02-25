package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.net.ItemRailUpdatePacket;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.track.BuilderBase;
import cam72cam.immersiverailroading.util.PlacementInfo;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.RollAndOffsetInfo;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui.screen.*;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.opengl.RenderState;
import net.minecraftforge.fml.client.config.GuiSlider;

public class TrackExtraGui implements IScreen {
    long frame;
    private TileRailPreview te;
    private RailSettings.Mutable settings;
    private RollAndOffsetInfo rollAndOffsetInfoCache;
    private boolean edited;
    private boolean editLeft;
    private final double length;//TODO: must prevent situation editing by more than one player! or there might cause sync problem
    private int currentLogicIndex = -1;
    private RailInfo referenceInfo;//only for calculating length and rendering
    //buttons to show state
    private Button rollValueLabel;
    private Button rollSlopeLabel;
    private Button rollHandlerXLenLabel;
    private Button yOffsetValueLabel;
    private Button yOffsetSlopeLabel;
    private Button yOffsetHandlerXLenLabel;
    private Button zOffsetValueLabel;
    private Button zOffsetSlopeLabel;
    private Button zOffsetHandlerXLenLabel;
    private TextField rollValueInput;
    private TextField rollSlopeInput;
    private TextField rollHandlerXLenInput;
    private TextField yOffsetValueInput;
    private TextField yOffsetSlopeInput;
    private TextField yOffsetHandlerXLenInput;
    private TextField zOffsetValueInput;
    private TextField zOffsetSlopeInput;
    private TextField zOffsetHandlerXLenInput;
    private Button rollGraph;
    private Button yOffsetGraph;
    private Button zOffsetGraph;
    private Slider lSlider;
    private Button insertPointButton;
    private Button deletePointButton;
    private Button editLeftButton;
    private Button resetAllButton;
    private Button offsetTypeButton;
    private Button railInfoLabel;
    private Button wayCircleButton;//TODO:Switch and multiSwitch support
    private Button TrackGuiButton;
    public TrackExtraGui() {
        this(MinecraftClient.getPlayer().getHeldItem(Player.Hand.PRIMARY));
    }
    public TrackExtraGui(TileRailPreview te) {
        this(te.getItem());
        this.te = te;
        this.referenceInfo = te.getRailRenderInfo();
    }
    private TrackExtraGui(ItemStack stack) {
        stack = stack.copy();
        settings = RailSettings.from(stack).mutable();

        if(referenceInfo != null) {//TODO:Switch and multiSwitch support
            BuilderBase builder = referenceInfo.getBuilder(MinecraftClient.getPlayer().getWorld());
            length = builder.getRenderData().size() * referenceInfo.settings.gauge.scale() * referenceInfo.getTrackModel().spacing;//TODO this might not be precious
        }else {
            RailInfo info = new RailInfo(stack, new PlacementInfo(Vec3d.ZERO, TrackDirection.LEFT, MinecraftClient.getPlayer().getRotationYawHead(), null), null);
            BuilderBase builder = info.getBuilder(MinecraftClient.getPlayer().getWorld());
            length = builder.getRenderData().size() * info.settings.gauge.scale() * info.getTrackModel().spacing;//TODO this might not be precious
        }

        if(settings.pickRollAndOffsetInfo != null) {
            rollAndOffsetInfoCache = settings.rollAndOffsetInfo;
        } else {
            rollAndOffsetInfoCache = RollAndOffsetInfo.getDefault();
        }

        edited = false;
        editLeft = true;
    }


    public void init(IScreenBuilder screen) {
        int width = 200;
        int height = 20;
        int xtop = -GUIHelpers.getScreenWidth() / 2;
        int ytop = -GUIHelpers.getScreenHeight() / 4;

        //left panel
        railInfoLabel = new Button(screen, xtop, ytop, width, height,  "Rail Length:" + length) {};//todo
        ytop += height;
        ytop += 5;
        rollGraph = new Button(screen, xtop, ytop, width, height, "roll") {};//todo
        ytop += height * 3;
        ytop += 5;
        yOffsetGraph = new Button(screen, xtop, ytop, width, height, "y-offset") {};//todo
        ytop += height * 3;
        ytop += 5;
        zOffsetGraph = new Button(screen, xtop, ytop, width, height, "z-offset") {};//todo
        ytop += height * 3;
        ytop += 5;
        lSlider = new Slider(screen, xtop, ytop, width, height, "", 0.0, 1.0, 0.0, false, (slider) -> {}) {
            @Override
            public void onSlider() {
                lSlider.setText(String.format("%.2f", lSlider.getValue()));
                currentLogicIndex = rollAndOffsetInfoCache.findPhysicalIndex(lSlider.getValue());
                updateSliderRelated();
            }
            @Override
            public double getValue() {
                try {
                    return Double.parseDouble(String.format("%.2f",((GuiSlider) button).getValue()));
                } catch (NumberFormatException e) {
                    ImmersiveRailroading.warn("invalid text");//所见即索引，max点数100
                    return 0d;
                }
            }
        };

        //right panel
        insertPointButton = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width, ytop, 50, height, "Insert Point") {
            @Override
            public void onClick(Player.Hand hand) {
                edited = true;
                rollAndOffsetInfoCache.tryInsertBySubSplit(lSlider.getValue());
                currentLogicIndex = rollAndOffsetInfoCache.findPhysicalIndex(lSlider.getValue());
                updateSliderRelated();
            }
        };

        deletePointButton = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width, ytop, 50, height, "Delete Point") {
            @Override
            public void onClick(Player.Hand hand) {
                edited = true;
                rollAndOffsetInfoCache.tryDeleteDirectly(lSlider.getValue());
                currentLogicIndex = rollAndOffsetInfoCache.findPhysicalIndex(lSlider.getValue());
                updateSliderRelated();
            }
        };

        resetAllButton = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width + 50, ytop, 50, height, "Reset All") {
            @Override
            public void onClick(Player.Hand hand) {
                edited = true;
                rollAndOffsetInfoCache.resetAll();
                currentLogicIndex = rollAndOffsetInfoCache.findPhysicalIndex(lSlider.getValue());
                updateSliderRelated();
            }
        };

        editLeftButton = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width + 50 * 2, ytop, 50, height, "Edit Left") {
            @Override
            public void onClick(Player.Hand hand) {
                editLeft = !editLeft;
                if(editLeft) {
                    this.setText("Edit Left");
                }else {
                    this.setText("Edit Right");
                }
                updateAllCurveInfoDisplay();
            }
        };

        TrackGuiButton = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width + 50 * 3, ytop, 50, height, "Track Gui") {
            @Override
            public void onClick(Player.Hand hand) {
                if (te != null) {
                    te.shouldTrackGuiActive = true;
                    GuiTypes.RAIL_PREVIEW.open(MinecraftClient.getPlayer(),te.getPos());
                } else {
                    GuiTypes.RAIL.open(MinecraftClient.getPlayer());
                }
            }
        };

        //back to top
        ytop = -GUIHelpers.getScreenHeight() / 4;

        offsetTypeButton = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width, ytop, width, height, GuiText.TRACK_ROLL_OFFSET_TYPE.toString() + rollAndOffsetInfoCache.offsetType) {//todo:guiText
            @Override
            public void onClick(Player.Hand hand) {
                edited = true;

                int order = rollAndOffsetInfoCache.offsetType.getOrder();
                int amount = RollAndOffsetInfo.RollYOffsetType.amount;
                order = (order + (hand == Player.Hand.SECONDARY ? 1 : -1) + amount) % amount;

                int finalOrder = order;
                rollAndOffsetInfoCache = rollAndOffsetInfoCache.with(mutable -> mutable.offsetType = RollAndOffsetInfo.RollYOffsetType.byOrder(finalOrder));

                this.setText(GuiText.TRACK_ROLL_OFFSET_TYPE.toString() + rollAndOffsetInfoCache.offsetType);
            }
        };

        ytop += height;
        ytop += 5;

        rollValueInput = new TextField(screen,GUIHelpers.getScreenWidth() / 2 - width / 2, ytop, width / 2, height);
        rollValueInput.setText("");
        rollValueInput.setValidator(s -> {
            if (s == null || s.isEmpty()) {
                return true;
            }
            float val;
            try {
                val = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                return s.equals(".") || s.equals("-");
            }
            float max = 20f;
            if (Math.abs(val) < max) {
                boolean feedback = rollAndOffsetInfoCache.tryDeltaValue(lSlider.getValue(), val, RollAndOffsetInfo.ExtraInfoType.ROLL);
                if(feedback) {
                    updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.ROLL);
                    edited = true;
                }
                return feedback;
            }
            return false;
        });
        rollValueLabel = new Button(screen,GUIHelpers.getScreenWidth() / 2 - width, ytop, width / 2, height, "") {};

        ytop += height;

        rollSlopeInput = new TextField(screen,GUIHelpers.getScreenWidth() / 2 - width / 2, ytop, width / 2, height);
        rollSlopeInput.setText("");
        rollSlopeInput.setValidator(s -> {
            if (s == null || s.isEmpty()) {
                return true;
            }
            float val;
            try {
                val = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                return s.equals(".") || s.equals("-");
            }
            float max = 20f;
            if (Math.abs(val) <= max) {
                boolean feedback = rollAndOffsetInfoCache.trySetSlope(lSlider.getValue(), val, RollAndOffsetInfo.ExtraInfoType.ROLL, length);
                if(feedback) {
                    updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.ROLL);
                    edited = true;
                }
                return feedback;
            }
            return false;
        });
        rollSlopeLabel = new Button(screen,GUIHelpers.getScreenWidth() / 2 - width, ytop, width / 2, height, "") {};

        ytop += height;

        rollHandlerXLenInput = new TextField(screen,GUIHelpers.getScreenWidth() / 2 - width / 2, ytop, width / 2, height);
        rollHandlerXLenInput.setText("");
        rollHandlerXLenInput.setValidator(s -> {
            if (s == null || s.isEmpty()) {
                return true;
            }
            float val;
            try {
                val = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                return s.equals(".") || s.equals("-");
            }
            float max = 20f;
            if (Math.abs(val) < max) {
                boolean feedback = rollAndOffsetInfoCache.trySetHandlerXLen(lSlider.getValue(), val, RollAndOffsetInfo.ExtraInfoType.ROLL, editLeft, length);
                if(feedback) {
                    updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.ROLL);
                    edited = true;
                }
                return feedback;
            }
            return false;
        });
        rollHandlerXLenLabel = new Button(screen,GUIHelpers.getScreenWidth() / 2 - width, ytop, width / 2, height, "") {};

        ytop += height;
        ytop += 5;

        yOffsetValueInput = new TextField(screen, GUIHelpers.getScreenWidth() / 2 - width / 2, ytop, width / 2, height);
        yOffsetValueInput.setText("");
        yOffsetValueInput.setValidator(s -> {
            if (s == null || s.isEmpty()) {
                return true;
            }
            float val;
            try {
                val = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                return s.equals(".") || s.equals("-");
            }
            float max = (float) settings.gauge.scale();
            if (Math.abs(val) <= max) {
                boolean feedback = rollAndOffsetInfoCache.tryDeltaValue(lSlider.getValue(), val, RollAndOffsetInfo.ExtraInfoType.Y_OFFSET);
                if(feedback) {
                    updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.Y_OFFSET);
                    edited = true;
                }
                return feedback;
            }
            return false;
        });
        yOffsetValueLabel = new Button(screen,GUIHelpers.getScreenWidth() / 2 - width, ytop, width / 2, height, "") {};

        ytop += height;

        yOffsetSlopeInput = new TextField(screen,GUIHelpers.getScreenWidth() / 2 - width / 2, ytop, width / 2, height);
        yOffsetSlopeInput.setText("");
        yOffsetSlopeInput.setValidator(s -> {
            if (s == null || s.isEmpty()) {
                return true;
            }
            float val;
            try {
                val = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                return s.equals(".") || s.equals("-");
            }
            float max = 20f;
            if (Math.abs(val) < max) {
                boolean feedback = rollAndOffsetInfoCache.trySetSlope(lSlider.getValue(), val, RollAndOffsetInfo.ExtraInfoType.Y_OFFSET, length);
                if(feedback) {
                    updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.Y_OFFSET);
                    edited = true;
                }
                return feedback;
            }
            return false;
        });
        yOffsetSlopeLabel = new Button(screen,GUIHelpers.getScreenWidth() / 2 - width, ytop, width / 2, height, "") {};

        ytop += height;

        yOffsetHandlerXLenInput = new TextField(screen,GUIHelpers.getScreenWidth() / 2 - width / 2, ytop, width / 2, height);
        yOffsetHandlerXLenInput.setText("");
        yOffsetHandlerXLenInput.setValidator(s -> {
            if (s == null || s.isEmpty()) {
                return true;
            }
            float val;
            try {
                val = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                return s.equals(".") || s.equals("-");
            }
            float max = 20f;
            if (Math.abs(val) < max) {
                boolean feedback = rollAndOffsetInfoCache.trySetHandlerXLen(lSlider.getValue(), val, RollAndOffsetInfo.ExtraInfoType.Y_OFFSET, editLeft, length);
                if(feedback) {
                    updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.Y_OFFSET);
                    edited = true;
                }
                return feedback;
            }
            return false;
        });
        yOffsetHandlerXLenLabel = new Button(screen,GUIHelpers.getScreenWidth() / 2 - width, ytop, width / 2, height, "") {};

        ytop += height;
        ytop += 5;

        zOffsetValueInput = new TextField(screen, GUIHelpers.getScreenWidth() / 2 - width / 2, ytop, width / 2, height);
        zOffsetValueInput.setText("");
        zOffsetValueInput.setValidator(s -> {
            if (s == null || s.isEmpty()) {
                return true;
            }
            float val;
            try {
                val = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                return s.equals(".") || s.equals("-");
            }
            float max = (float) settings.gauge.scale();
            if (Math.abs(val) <= max) {
                boolean feedback = rollAndOffsetInfoCache.tryDeltaValue(lSlider.getValue(), val, RollAndOffsetInfo.ExtraInfoType.Z_OFFSET);
                if(feedback) {
                    updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.Z_OFFSET);
                    edited = true;
                }
                return feedback;
            }
            return false;
        });
        zOffsetValueLabel = new Button(screen,GUIHelpers.getScreenWidth() / 2 - width, ytop, width / 2, height, "") {};

        ytop += height;

        zOffsetSlopeInput = new TextField(screen,GUIHelpers.getScreenWidth() / 2 - width / 2, ytop, width / 2, height);
        zOffsetSlopeInput.setText("");
        zOffsetSlopeInput.setValidator(s -> {
            if (s == null || s.isEmpty()) {
                return true;
            }
            float val;
            try {
                val = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                return s.equals(".") || s.equals("-");
            }
            float max = 20f;
            if (Math.abs(val) < max) {
                boolean feedback = rollAndOffsetInfoCache.trySetSlope(lSlider.getValue(), val, RollAndOffsetInfo.ExtraInfoType.Z_OFFSET, length);
                if(feedback) {
                    updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.Z_OFFSET);
                    edited = true;
                }
                return feedback;
            }
            return false;
        });
        zOffsetSlopeLabel = new Button(screen,GUIHelpers.getScreenWidth() / 2 - width, ytop, width / 2, height, "") {};

        ytop += height;

        zOffsetHandlerXLenInput = new TextField(screen,GUIHelpers.getScreenWidth() / 2 - width / 2, ytop, width / 2, height);
        zOffsetHandlerXLenInput.setText("");
        zOffsetHandlerXLenInput.setValidator(s -> {
            if (s == null || s.isEmpty()) {
                return true;
            }
            float val;
            try {
                val = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                return s.equals(".") || s.equals("-");
            }
            float max = 20f;
            if (Math.abs(val) < max) {
                boolean feedback = rollAndOffsetInfoCache.trySetHandlerXLen(lSlider.getValue(), val, RollAndOffsetInfo.ExtraInfoType.Z_OFFSET, editLeft, length);
                if(feedback) {
                    updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.Z_OFFSET);
                    edited = true;
                }
                return feedback;
            }
            return false;
        });
        zOffsetHandlerXLenLabel = new Button(screen,GUIHelpers.getScreenWidth() / 2 - width, ytop, width / 2, height, "") {};

        //update after all components init
        lSlider.onSlider();
    }

    @Override
    public void onClose() {
        if(edited) {
            settings = settings.immutable().with(mutable -> {
                mutable.rollAndOffsetInfo = rollAndOffsetInfoCache;
                mutable.pickRollAndOffsetInfo = rollAndOffsetInfoCache;
            }).mutable();
        }

        if (this.te != null) {
            new ItemRailUpdatePacket(te.getPos(), settings.immutable()).sendToServer();
        } else {
            new ItemRailUpdatePacket(settings.immutable()).sendToServer();
        }
    }

    @Override
    public void draw(IScreenBuilder builder, RenderState state) {
        frame ++;
    }

    private void updateSliderRelated() {
        if(insertPointButton != null)insertPointButton.setVisible(currentLogicIndex == -1);
        if(deletePointButton != null)deletePointButton.setVisible(currentLogicIndex != -1);
        if(rollValueInput != null)rollValueInput.setText("");
        if(rollSlopeInput != null)rollSlopeInput.setText("");
        if(rollHandlerXLenInput != null)rollHandlerXLenInput.setText("");
        if(yOffsetValueInput != null)yOffsetValueInput.setText("");
        if(yOffsetSlopeInput != null)yOffsetSlopeInput.setText("");
        if(yOffsetHandlerXLenInput != null)yOffsetHandlerXLenInput.setText("");
        if(zOffsetValueInput != null)zOffsetValueInput.setText("");
        if(zOffsetSlopeInput != null)zOffsetSlopeInput.setText("");
        if(zOffsetHandlerXLenInput != null)zOffsetHandlerXLenInput.setText("");
        updateAllCurveInfoDisplay();
    }

    private void updateAllCurveInfoDisplay() {
        updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.ROLL);
        updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.Y_OFFSET);
        updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.Z_OFFSET);
    }

    private void updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType type) {
        Button valueLabel;
        Button slopeLabel;
        Button handlerXLenLabel;

        switch (type) {
            case ROLL:
                valueLabel = rollValueLabel;
                slopeLabel = rollSlopeLabel;
                handlerXLenLabel = rollHandlerXLenLabel;
                break;
            case Y_OFFSET:
                valueLabel = yOffsetValueLabel;
                slopeLabel = yOffsetSlopeLabel;
                handlerXLenLabel = yOffsetHandlerXLenLabel;
                break;
            case Z_OFFSET:
                valueLabel = zOffsetValueLabel;
                slopeLabel = zOffsetSlopeLabel;
                handlerXLenLabel = zOffsetHandlerXLenLabel;
                break;
            default:
                ImmersiveRailroading.warn("invalid ExtraInfoType:" + type);
                return;
        }

        if(valueLabel != null)valueLabel.setText(type + " Value:" + rollAndOffsetInfoCache.getValueDisplay(lSlider.getValue(), type));
        if(slopeLabel != null)slopeLabel.setText(type + " Slope:" + rollAndOffsetInfoCache.getSlopeDisplay(lSlider.getValue(), type, length));
        if(handlerXLenLabel != null)handlerXLenLabel.setText(type + " Handler X:" + rollAndOffsetInfoCache.getHandlerXDisplay(lSlider.getValue(), type, editLeft, length));
    }
 }
