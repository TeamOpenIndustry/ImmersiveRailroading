package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.gui.util.BezierRenderer;
import cam72cam.immersiverailroading.gui.util.Color;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.net.ItemRailUpdatePacket;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.track.BuilderBase;
import cam72cam.immersiverailroading.track.VecYPR;
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
import cam72cam.mod.serialization.TagCompound;
import util.Matrix4;

import java.util.ArrayList;
import java.util.List;

public class TrackExtraGui implements IScreen {
    private double rollMax;
    private double yOffsetMax;
    private double zOffsetMax;
    Color curveColor;
    Color pointColor;
    Color handlePointColor;
    Color handleLineColor;
    Color arrowColor;
    private TileRailPreview te;
    private RailSettings.Mutable settings;
    private RollAndOffsetInfo.Mutable rollAndOffsetInfoCache;
    private int guiOpenType;
    private boolean edited;
    private boolean editLeft;
    private final double length;
    private final RailInfo referenceInfo;//only for calculating length and rendering
    private final List<VecYPR> referenceRenderData;
    //buttons to show state
    private Button rollValueLabel;
    private Button rollSlopeLabel;
    private Button rollHandleXLenLabel;
    private Button yOffsetValueLabel;
    private Button yOffsetSlopeLabel;
    private Button yOffsetHandleXLenLabel;
    private Button zOffsetValueLabel;
    private Button zOffsetSlopeLabel;
    private Button zOffsetHandleXLenLabel;
    private TextField rollValueInput;
    private TextField rollSlopeInput;
    private TextField rollHandleXLenInput;
    private TextField yOffsetValueInput;
    private TextField yOffsetSlopeInput;
    private TextField yOffsetHandleXLenInput;
    private TextField zOffsetValueInput;
    private TextField zOffsetSlopeInput;
    private TextField zOffsetHandleXLenInput;
    private Slider lSlider;
    private Button insertOrDeletePointButton;
    private Button editLeftButton;
    private Button resetAllButton;
    private Button offsetTypeButton;
    private Button railInfoLabel;
    private CheckBox rollEffectTileCB;
    private CheckBox tileTiltCB;
//    private Button wayCircleButton;
    private Button TrackGuiButton;
    public TrackExtraGui() {
        this(MinecraftClient.getPlayer().getHeldItem(Player.Hand.PRIMARY), null);
    }
    public TrackExtraGui(TileRailPreview te) {
        this(te.getItem(), te);
    }
    private TrackExtraGui(ItemStack stack, TileRailPreview te) {
        stack = stack.copy();
        this.settings = RailSettings.from(stack).mutable();
        try{
            this.guiOpenType = RailSettings.getExtraDataFrom(stack).getInteger("guiOpenType");
        }catch (NullPointerException e) {
            this.guiOpenType = 0;
        }
        this.te = te;

        if(this.te != null) {//TODO:Switch and multiSwitch support
            referenceInfo = te.getRailRenderInfo();
        }else {
            referenceInfo = new RailInfo(stack, new PlacementInfo(Vec3d.ZERO, TrackDirection.LEFT, MinecraftClient.getPlayer().getRotationYawHead(), null), null);
        }
        BuilderBase referenceInfoBuilder = referenceInfo.getBuilder(MinecraftClient.getPlayer().getWorld());
        referenceRenderData = referenceInfoBuilder.getRenderData();
        length = referenceRenderData.size() * referenceInfo.settings.gauge.scale() * referenceInfo.getTrackModel().spacing;

        if(settings.pickRollAndOffsetInfo != null) {
            rollAndOffsetInfoCache = settings.rollAndOffsetInfo.mutable();
        } else {
            rollAndOffsetInfoCache = RollAndOffsetInfo.getDefault().mutable();
        }

        //basic Gauge: Standard Gauge. other gauge will scale from standard
        rollMax = 40;//unit:centimeter(1435mm), if in gauge X mm, it will be scaled to rollMax * X / 1435 centimeters
        yOffsetMax = 1;//unit:meter(1435mm), if in gauge X mm, it will be scaled to rollMax * X / 1435 centimeters
        zOffsetMax = 1;//unit:meter(1435mm), if in gauge X mm, it will be scaled to rollMax * X / 1435 centimeters

        curveColor = Color.FLUORESCENT_GREEN;      // GREEN curve
        pointColor = Color.RED;      // RED point
        handlePointColor = Color.BLUE;      // BLUE handle point
        handleLineColor = Color.MAGENTA;      // MAGENTA handle line
        arrowColor = Color.YELLOW;      //YELLOW arrow point

        edited = false;
        editLeft = true;
    }


    public void init(IScreenBuilder screen) {
        int width = 200;
        int height = 20;
        int xtop = -GUIHelpers.getScreenWidth() / 2 + 5;
        int ytop = -GUIHelpers.getScreenHeight() / 4;

        //left panel
        railInfoLabel = new Button(screen, xtop, ytop, width / 2, height,  "Rail Length:" + length) {};//TODO: what else need to be displayed here?
        ytop += height;
        ytop += 5;
        //rollGraph
        ytop += height * 3;
        ytop += 5;
        //yOffsetGraph
        ytop += height * 3;
        ytop += 5;
        //zOffsetGraph
        ytop += height * 3;
        ytop += 5;
        lSlider = new Slider(screen, xtop, ytop, width, height, "", 0.0, 1.0, 0.0, false, (slider) -> {}) {
            @Override
            public void onSlider() {
                lSlider.setText(String.format("%.2f", format(lSlider.getValue())));
                updateSliderRelated();
            }
        };

        //right panel
        insertOrDeletePointButton = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width, ytop, 50, height, "") {
            @Override
            public void onClick(Player.Hand hand) {
                if(rollAndOffsetInfoCache.findPhysicalIndex(format(lSlider.getValue())) == -1) {//insert
                    if(rollAndOffsetInfoCache.tryInsertBySubSplit(format(lSlider.getValue()))) {
                        edited = true;
                        updateSliderRelated();
                    }
                } else {//delete
                    edited = true;
                    rollAndOffsetInfoCache.tryDeleteDirectly(format(lSlider.getValue()));
                    updateSliderRelated();
                }
            }
        };

        resetAllButton = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width + 50, ytop, 50, height, GuiText.TRACK_EXTRA_RESET.toString()) {
            @Override
            public void onClick(Player.Hand hand) {
                edited = true;
                rollAndOffsetInfoCache.resetAll();
                updateSliderRelated();
            }
        };

        editLeftButton = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width + 50 * 2, ytop, 50, height, GuiText.TRACK_EXTRA_EDIT_LEFT.toString()) {
            @Override
            public void onClick(Player.Hand hand) {
                editLeft = !editLeft;
                if(editLeft) {
                    this.setText(GuiText.TRACK_EXTRA_EDIT_LEFT.toString());
                }else {
                    this.setText(GuiText.TRACK_EXTRA_EDIT_RIGHT.toString());
                }
                updateAllCurveInfoDisplay();
            }
        };

        TrackGuiButton = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width + 50 * 3, ytop, 50, height, GuiText.TRACK_EXTRA_TRACKGUI.toString()) {
            @Override
            public void onClick(Player.Hand hand) {
                guiOpenType = 0;
                onClose();
                if (te != null) {
                    GuiTypes.RAIL_PREVIEW.open(MinecraftClient.getPlayer(), te.getPos());
                } else {
                    GuiTypes.RAIL.open(MinecraftClient.getPlayer());
                }
            }
        };

        //back to top
        ytop = -GUIHelpers.getScreenHeight() / 4;

        rollEffectTileCB = new CheckBox(screen, GUIHelpers.getScreenWidth() / 2 - width + 30 - 85 - 60, ytop + 2, GuiText.SELECTOR_ROLL_EFFECT_TILE.toString(), rollAndOffsetInfoCache.rollEffectTile){
            @Override
            public void onClick(Player.Hand hand) {
                edited = true;
                rollAndOffsetInfoCache.rollEffectTile = rollEffectTileCB.isChecked();
                if(!rollAndOffsetInfoCache.rollEffectTile) {
                    rollAndOffsetInfoCache.tileTilt = false;
                    tileTiltCB.setChecked(false);
                }
            }
        };

        tileTiltCB = new CheckBox(screen, GUIHelpers.getScreenWidth() / 2 - width + 30 - 85, ytop + 2, GuiText.SELECTOR_TILE_TILT.toString(), rollAndOffsetInfoCache.tileTilt) {
            @Override
            public void onClick(Player.Hand hand) {
                if(rollAndOffsetInfoCache.rollEffectTile) {
                    edited = true;
                    rollAndOffsetInfoCache.tileTilt = tileTiltCB.isChecked();
                } else if(rollAndOffsetInfoCache.tileTilt){
                    edited = true;
                    rollAndOffsetInfoCache.tileTilt = false;
                    tileTiltCB.setChecked(false);
                } else {
                    tileTiltCB.setChecked(false);
                }
            }
        };

//        wayCircleButton = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width + 30 + 2 , ytop, 85, height, "Selected Way: 0"){};//TODO: waiting for multiSwitch branch merging

        offsetTypeButton = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width + 30 + 85, ytop, 85, height, rollAndOffsetInfoCache.offsetType.toString()) {
            @Override
            public void onClick(Player.Hand hand) {
                edited = true;

                int order = rollAndOffsetInfoCache.offsetType.getOrder();
                int amount = RollAndOffsetInfo.RollYOffsetType.amount;
                order = (order + (hand == Player.Hand.SECONDARY ? 1 : -1) + amount) % amount;

                int finalOrder = order;
                rollAndOffsetInfoCache.offsetType = RollAndOffsetInfo.RollYOffsetType.byOrder(finalOrder);

                this.setText(rollAndOffsetInfoCache.offsetType.toString());
            }
        };
        List<String> offsetTypeButtonText = new ArrayList<>();
        offsetTypeButtonText.add(GuiText.TRACK_ROLL_OFFSET_TYPE.toString());
        offsetTypeButton.setTooltip(offsetTypeButtonText);

        ytop += height;
        ytop += 5;

        rollValueInput = new TextField(screen,GUIHelpers.getScreenWidth() / 2 - width / 4, ytop, width / 4, height);
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
            float max = (float) rollMax;
            if (Math.abs(val) <= max) {
                boolean feedback = rollAndOffsetInfoCache.tryDeltaValue(format(lSlider.getValue()), val, RollAndOffsetInfo.ExtraInfoType.ROLL);
                if(feedback) {
                    updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.ROLL);
                    edited = true;
                }
                return feedback;
            }
            return false;
        });
        rollValueLabel = new Button(screen,GUIHelpers.getScreenWidth() / 2 - width, ytop, 150, height, "") {};

        ytop += height;

        rollSlopeInput = new TextField(screen,GUIHelpers.getScreenWidth() / 2 - width / 4, ytop, width / 4, height);
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
            float max = 100f / 2;
            if (Math.abs(val) <= max) {
                boolean feedback = rollAndOffsetInfoCache.trySetSlope(format(lSlider.getValue()), val, RollAndOffsetInfo.ExtraInfoType.ROLL, length);
                if(feedback) {
                    updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.ROLL);
                    edited = true;
                }
                return feedback;
            }
            return false;
        });
        rollSlopeLabel = new Button(screen,GUIHelpers.getScreenWidth() / 2 - width, ytop, 150, height, "") {};

        ytop += height;

        rollHandleXLenInput = new TextField(screen,GUIHelpers.getScreenWidth() / 2 - width / 4, ytop, width / 4, height);
        rollHandleXLenInput.setText("");
        rollHandleXLenInput.setValidator(s -> {
            if (s == null || s.isEmpty()) {
                return true;
            }
            float val;
            try {
                val = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                return s.equals(".") || s.equals("-");
            }
            float max = (float) length * 0.5f;
            if (Math.abs(val) < max) {
                boolean feedback = rollAndOffsetInfoCache.trySetHandleXLen(format(lSlider.getValue()), val, RollAndOffsetInfo.ExtraInfoType.ROLL, editLeft, length);
                if(feedback) {
                    updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.ROLL);
                    edited = true;
                }
                return feedback;
            }
            return false;
        });
        rollHandleXLenLabel = new Button(screen,GUIHelpers.getScreenWidth() / 2 - width, ytop, 150, height, "") {};

        ytop += height;
        ytop += 5;

        yOffsetValueInput = new TextField(screen, GUIHelpers.getScreenWidth() / 2 - width / 4, ytop, width / 4, height);
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
            float max = (float) yOffsetMax;
            if (Math.abs(val) <= max) {
                boolean feedback = rollAndOffsetInfoCache.tryDeltaValue(format(lSlider.getValue()), val, RollAndOffsetInfo.ExtraInfoType.Y_OFFSET);
                if(feedback) {
                    updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.Y_OFFSET);
                    edited = true;
                }
                return feedback;
            }
            return false;
        });
        yOffsetValueLabel = new Button(screen,GUIHelpers.getScreenWidth() / 2 - width, ytop, 150, height, "") {};

        ytop += height;

        yOffsetSlopeInput = new TextField(screen,GUIHelpers.getScreenWidth() / 2 - width / 4, ytop, width / 4, height);
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
            float max = 100f;
            if (Math.abs(val) <= max) {
                boolean feedback = rollAndOffsetInfoCache.trySetSlope(format(lSlider.getValue()), val, RollAndOffsetInfo.ExtraInfoType.Y_OFFSET, length);
                if(feedback) {
                    updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.Y_OFFSET);
                    edited = true;
                }
                return feedback;
            }
            return false;
        });
        yOffsetSlopeLabel = new Button(screen,GUIHelpers.getScreenWidth() / 2 - width, ytop, 150, height, "") {};

        ytop += height;

        yOffsetHandleXLenInput = new TextField(screen,GUIHelpers.getScreenWidth() / 2 - width / 4, ytop, width / 4, height);
        yOffsetHandleXLenInput.setText("");
        yOffsetHandleXLenInput.setValidator(s -> {
            if (s == null || s.isEmpty()) {
                return true;
            }
            float val;
            try {
                val = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                return s.equals(".") || s.equals("-");
            }
            float max = (float) length * 0.5f;
            if (Math.abs(val) < max) {
                boolean feedback = rollAndOffsetInfoCache.trySetHandleXLen(format(lSlider.getValue()), val, RollAndOffsetInfo.ExtraInfoType.Y_OFFSET, editLeft, length);
                if(feedback) {
                    updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.Y_OFFSET);
                    edited = true;
                }
                return feedback;
            }
            return false;
        });
        yOffsetHandleXLenLabel = new Button(screen,GUIHelpers.getScreenWidth() / 2 - width, ytop, 150, height, "") {};

        ytop += height;
        ytop += 5;

        zOffsetValueInput = new TextField(screen, GUIHelpers.getScreenWidth() / 2 - width / 4, ytop, width / 4, height);
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
            float max = (float) zOffsetMax;
            if (Math.abs(val) <= max) {
                boolean feedback = rollAndOffsetInfoCache.tryDeltaValue(format(lSlider.getValue()), val, RollAndOffsetInfo.ExtraInfoType.Z_OFFSET);
                if(feedback) {
                    updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.Z_OFFSET);
                    edited = true;
                }
                return feedback;
            }
            return false;
        });
        zOffsetValueLabel = new Button(screen,GUIHelpers.getScreenWidth() / 2 - width, ytop, 150, height, "") {};

        ytop += height;

        zOffsetSlopeInput = new TextField(screen,GUIHelpers.getScreenWidth() / 2 - width / 4, ytop, width / 4, height);
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
            float max = 100f;
            if (Math.abs(val) <= max) {
                boolean feedback = rollAndOffsetInfoCache.trySetSlope(format(lSlider.getValue()), val, RollAndOffsetInfo.ExtraInfoType.Z_OFFSET, length);
                if(feedback) {
                    updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.Z_OFFSET);
                    edited = true;
                }
                return feedback;
            }
            return false;
        });
        zOffsetSlopeLabel = new Button(screen,GUIHelpers.getScreenWidth() / 2 - width, ytop, 150, height, "") {};

        ytop += height;

        zOffsetHandleXLenInput = new TextField(screen,GUIHelpers.getScreenWidth() / 2 - width / 4, ytop, width / 4, height);
        zOffsetHandleXLenInput.setText("");
        zOffsetHandleXLenInput.setValidator(s -> {
            if (s == null || s.isEmpty()) {
                return true;
            }
            float val;
            try {
                val = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                return s.equals(".") || s.equals("-");
            }
            float max = (float) length * 0.5f;
            if (Math.abs(val) < max) {
                boolean feedback = rollAndOffsetInfoCache.trySetHandleXLen(format(lSlider.getValue()), val, RollAndOffsetInfo.ExtraInfoType.Z_OFFSET, editLeft, length);
                if(feedback) {
                    updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.Z_OFFSET);
                    edited = true;
                }
                return feedback;
            }
            return false;
        });
        zOffsetHandleXLenLabel = new Button(screen,GUIHelpers.getScreenWidth() / 2 - width, ytop, 150, height, "") {};

        //update after all components init
        lSlider.onSlider();
    }

    @Override
    public void onClose() {
        if(edited) {
            settings = settings.immutable().with(mutable -> {
                mutable.rollAndOffsetInfo = rollAndOffsetInfoCache.immutable();
                mutable.pickRollAndOffsetInfo = rollAndOffsetInfoCache.immutable();
            }).mutable();
        }

        if (this.te != null) {
            new ItemRailUpdatePacket(te.getPos(), settings.immutable(), guiOpenType).sendToServer();

            //also update client Item to update Rail information
            ItemStack clientStack = te.getItem();
            settings.immutable().write(clientStack);
            RailSettings.writeExtraData(clientStack, new TagCompound().setInteger("guiOpenType", guiOpenType));
            te.setItem(clientStack, MinecraftClient.getPlayer());
        } else {
            new ItemRailUpdatePacket(settings.immutable(), guiOpenType).sendToServer();

            //also update client Item to update Rail information
            ItemStack clientStack = MinecraftClient.getPlayer().getHeldItem(Player.Hand.PRIMARY);
            settings.immutable().write(clientStack);
            RailSettings.writeExtraData(clientStack, new TagCompound().setInteger("guiOpenType", guiOpenType));
            MinecraftClient.getPlayer().setHeldItem(Player.Hand.PRIMARY, clientStack);
        }
    }

    @Override
    public void draw(IScreenBuilder builder, RenderState state) {
        int height = 20;
        double xScale = 200;
        double rollYScale = height * 1.5 / rollMax;
        double yOffsetYScale = height * 1.5 / yOffsetMax;
        double zOffsetYScale = height * 1.5 / zOffsetMax;
        RollAndOffsetInfo immutable = rollAndOffsetInfoCache.immutable();

        //Text
        double textScale = 0.5;
        GUIHelpers.drawCenteredString(RollAndOffsetInfo.ExtraInfoType.ROLL.toString(), (int) (105  / textScale) + 1, (int) ((height + 2) / textScale) + 1, 0x000000, new Matrix4().scale(textScale, textScale, textScale));
        GUIHelpers.drawCenteredString(RollAndOffsetInfo.ExtraInfoType.Y_OFFSET.toString(), (int) (105  / textScale) + 1, (int) ((height + 2 + height * 3 + 5) / textScale) + 1, 0x000000, new Matrix4().scale(textScale, textScale, textScale));
        GUIHelpers.drawCenteredString(RollAndOffsetInfo.ExtraInfoType.Z_OFFSET.toString(), (int) (105  / textScale) + 1, (int) ((height + 2 + height * 6 + 10) / textScale) + 1, 0x000000, new Matrix4().scale(textScale, textScale, textScale));

        GUIHelpers.drawCenteredString(RollAndOffsetInfo.ExtraInfoType.ROLL.toString(), (int) (105  / textScale), (int) ((height + 2) / textScale), 0xFFFFFF, new Matrix4().scale(textScale, textScale, textScale));
        GUIHelpers.drawCenteredString(RollAndOffsetInfo.ExtraInfoType.Y_OFFSET.toString(), (int) (105  / textScale), (int) ((height + 2 + height * 3 + 5) / textScale), 0xFFFFFF, new Matrix4().scale(textScale, textScale, textScale));
        GUIHelpers.drawCenteredString(RollAndOffsetInfo.ExtraInfoType.Z_OFFSET.toString(), (int) (105  / textScale), (int) ((height + 2 + height * 6 + 10) / textScale), 0xFFFFFF, new Matrix4().scale(textScale, textScale, textScale));

        //TODO: if choose HIGH or LOW, half of the the roll graph will flip, need to flip it on graph?
        //rollGraph
        state.translate(5, height + 5 + height * 1.5, 0);
        BezierRenderer rollGraph = new BezierRenderer(state, rollAndOffsetInfoCache.toCurves(RollAndOffsetInfo.ExtraInfoType.ROLL, true));
        rollGraph.drawDashLine(Vec3d.ZERO, new Vec3d(1, 0, 0), Color.WHITE, xScale, rollYScale, 1, 0.05f, 0.05f, 0);
        rollGraph.drawBeziers(curveColor, pointColor, handlePointColor, handleLineColor, 100, xScale, rollYScale);
        rollGraph.drawArrow(new Vec3d(format(lSlider.getValue()), immutable.getRoll(format(lSlider.getValue())), 0), Color.YELLOW, 2.4, xScale, rollYScale);

        //yOffsetGraph
        state.translate(0, height * 3 + 5, 0);
        BezierRenderer yOffsetGraph = new BezierRenderer(state, rollAndOffsetInfoCache.toCurves(RollAndOffsetInfo.ExtraInfoType.Y_OFFSET, true));
        yOffsetGraph.drawDashLine(Vec3d.ZERO, new Vec3d(1, 0, 0), Color.WHITE, xScale, yOffsetYScale, 1, 0.05f, 0.05f, 0);
        yOffsetGraph.drawBeziers(curveColor, pointColor, handlePointColor, handleLineColor, 100, xScale, yOffsetYScale);
        yOffsetGraph.drawArrow(new Vec3d(format(lSlider.getValue()), immutable.getYOffset(format(lSlider.getValue())), 0), Color.YELLOW, 2.4, xScale, yOffsetYScale);

        //zOffsetGraph
        state.translate(0, height * 3 + 5, 0);
        BezierRenderer zOffsetGraph = new BezierRenderer(state, rollAndOffsetInfoCache.toCurves(RollAndOffsetInfo.ExtraInfoType.Z_OFFSET, true));
        zOffsetGraph.drawDashLine(Vec3d.ZERO, new Vec3d(1, 0, 0), Color.WHITE, xScale, zOffsetYScale, 1, 0.05f, 0.05f, 0);
        zOffsetGraph.drawBeziers(curveColor, pointColor, handlePointColor, handleLineColor, 100, xScale, zOffsetYScale);
        zOffsetGraph.drawArrow(new Vec3d(format(lSlider.getValue()), immutable.getZOffset(format(lSlider.getValue())), 0), Color.YELLOW, 2.4, xScale, zOffsetYScale);

    }

    private void updateSliderRelated() {
        if(insertOrDeletePointButton != null) {
            if(rollAndOffsetInfoCache.findPhysicalIndex(format(lSlider.getValue())) == -1) {
                insertOrDeletePointButton.setText(GuiText.TRACK_EXTRA_INSERT_POINT.toString());
            } else {
                insertOrDeletePointButton.setText(GuiText.TRACK_EXTRA_DELETE_POINT.toString());
            }
        }
        if(rollValueInput != null)rollValueInput.setText("");
        if(rollSlopeInput != null)rollSlopeInput.setText("");
        if(rollHandleXLenInput != null) rollHandleXLenInput.setText("");
        if(yOffsetValueInput != null)yOffsetValueInput.setText("");
        if(yOffsetSlopeInput != null)yOffsetSlopeInput.setText("");
        if(yOffsetHandleXLenInput != null) yOffsetHandleXLenInput.setText("");
        if(zOffsetValueInput != null)zOffsetValueInput.setText("");
        if(zOffsetSlopeInput != null)zOffsetSlopeInput.setText("");
        if(zOffsetHandleXLenInput != null) zOffsetHandleXLenInput.setText("");
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
        Button handleXLenLabel;

        switch (type) {
            case ROLL:
                valueLabel = rollValueLabel;
                slopeLabel = rollSlopeLabel;
                handleXLenLabel = rollHandleXLenLabel;
                break;
            case Y_OFFSET:
                valueLabel = yOffsetValueLabel;
                slopeLabel = yOffsetSlopeLabel;
                handleXLenLabel = yOffsetHandleXLenLabel;
                break;
            case Z_OFFSET:
                valueLabel = zOffsetValueLabel;
                slopeLabel = zOffsetSlopeLabel;
                handleXLenLabel = zOffsetHandleXLenLabel;
                break;
            default:
                ImmersiveRailroading.warn("invalid ExtraInfoType:" + type);
                return;
        }

        if(type == RollAndOffsetInfo.ExtraInfoType.ROLL) {
            if(valueLabel != null)valueLabel.setText(GuiText.TRACK_EXTRA_POINT_VALUE_CM + rollAndOffsetInfoCache.getValueDisplay(format(lSlider.getValue()), type));
        } else {
            if(valueLabel != null)valueLabel.setText(GuiText.TRACK_EXTRA_POINT_VALUE_M + rollAndOffsetInfoCache.getValueDisplay(format(lSlider.getValue()), type));
        }
        if(slopeLabel != null)slopeLabel.setText(GuiText.TRACK_EXTRA_POINT_SLOPE + rollAndOffsetInfoCache.getSlopeDisplay(format(lSlider.getValue()), type, length));
        if(handleXLenLabel != null)handleXLenLabel.setText(GuiText.TRACK_EXTRA_POINT_WEIGHT + rollAndOffsetInfoCache.getHandleXDisplay(format(lSlider.getValue()), type, editLeft, length));
    }

    private static double format(double value) {
        return Double.parseDouble(String.format("%.2f", value));
    }
 }
