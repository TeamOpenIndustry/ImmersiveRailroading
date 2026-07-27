package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.gui.util.BezierRenderer;
import cam72cam.immersiverailroading.gui.util.Color;
import cam72cam.immersiverailroading.items.ItemTrackBlueprint;
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
import util.Matrix4;

import java.util.List;

//Advanced settings for the track
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
    private int targetGuiOpenType;
    private boolean edited;
    private boolean editLeft;
    private final double length;
    private final RailInfo referenceInfo;//Only for calculating length and rendering
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
    private Slider ArcLenFactorSlider;
    private Button insertOrDeletePointButton;
    private Button editLeftButton;
    private Button resetAllButton;
    private Button rollOffsetTypeButton;
    private Button railInfoLabel;
    private CheckBox rollEffectTileCB;
    private CheckBox railBlockNormalCB;
    private CheckBox degreeModeCB;
    private CheckBox offsetVertByNormalCB;
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
        this.targetGuiOpenType = new ItemTrackBlueprint.Data(stack).guiOpenType;
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

        //Basic Gauge: Standard Gauge. other gauge will scale from standard

        //Common mode:unit:centimeter(1435mm), if in gauge X mm, it will be scaled to rollMax * X / 1435 centimeters
        //Degree mode:degree
        rollMax = rollAndOffsetInfoCache.degreeMode ? 45 : 60;//180 for Degree mode later
        yOffsetMax = 1;//Unit:meter(1435mm), if in gauge X mm, it will be scaled to rollMax * X / 1435 meters
        zOffsetMax = 1;//Unit:meter(1435mm), if in gauge X mm, it will be scaled to rollMax * X / 1435 meters

        curveColor = Color.CHARTREUSE;      // GREEN curve
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

        //Left panel
        //What else are needed to be displayed here?
        railInfoLabel = new Button(screen, xtop, ytop, 90, height,  GuiText.TRACK_LENGTH.toString(length), (_, _) -> {});
        railInfoLabel.setEnabled(false);

        ytop += height;
        ytop += 5;
        //roll Graph
        ytop += height * 3;
        ytop += 5;
        //yOffset Graph
        ytop += height * 3;
        ytop += 5;
        //zOffset Graph
        ytop += height * 3;
        ytop += 5;
        ArcLenFactorSlider = new Slider(screen, xtop, ytop, width, height, "", 0.0, 1.0, 0.0, false, (self) -> {
            self.setText(GuiText.TRACK_EXTRA_ARC_LEN_FACTOR.toString(String.format("%.2f", format(self.getValue()))));
            updateSliderRelated();
        });

        //Right panel
        insertOrDeletePointButton = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width, ytop, 50, height, "",
                                               (_, _) -> {
                                                   if(rollAndOffsetInfoCache.findPhysicalIndex(format(ArcLenFactorSlider.getValue())) == -1) {//insert
                                                       if(rollAndOffsetInfoCache.tryInsertBySubSplit(format(ArcLenFactorSlider.getValue()))) {//TODO: this is the reason why control point number is limited to 101, should we improve this?
                                                           edited = true;
                                                           updateSliderRelated();
                                                       }
                                                   } else {//delete
                                                       edited = true;
                                                       rollAndOffsetInfoCache.tryDeleteDirectly(format(ArcLenFactorSlider.getValue()));
                                                       updateSliderRelated();
                                                   }
                                               });

        resetAllButton = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width + 50, ytop, 50, height,
                                    GuiText.TRACK_EXTRA_RESET.toString(), (_, _) -> {
            edited = true;
            rollAndOffsetInfoCache.resetAll();
            updateSliderRelated();
        });

        editLeftButton = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width + 50 * 2, ytop, 50, height,
                                    GuiText.TRACK_EXTRA_EDIT_LEFT.toString(), (_, self) -> {
            editLeft = !editLeft;
            if(editLeft) {
                self.setText(GuiText.TRACK_EXTRA_EDIT_LEFT.toString());
            }else {
                self.setText(GuiText.TRACK_EXTRA_EDIT_RIGHT.toString());
            }
            updateAllCurveInfoDisplay();
        });

        TrackGuiButton = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width + 50 * 3, ytop, 50, height,
                                    GuiText.TRACK_EXTRA_TO_MAIN.toString(), (_, _) -> {
            targetGuiOpenType = 0;
            onClose();
            if (te != null) {
                GuiTypes.RAIL_PREVIEW.open(MinecraftClient.getPlayer(), te.getPos());
            } else {
                GuiTypes.RAIL.open(MinecraftClient.getPlayer());
            }
        });

        //Back to top
        ytop = -GUIHelpers.getScreenHeight() / 4;

        rollEffectTileCB = new CheckBox(screen, GUIHelpers.getScreenWidth() / 2 - width + 30 - 85 - 75, ytop + 1,
                                        GuiText.SELECTOR_ROLL_EFFECT_TILE.toString(), rollAndOffsetInfoCache.rollEffectTile,
                                        (_, self) -> {
                                            edited = true;
                                            rollAndOffsetInfoCache.rollEffectTile = self.isChecked();
                                            if(!rollAndOffsetInfoCache.rollEffectTile) {
                                                rollAndOffsetInfoCache.railBlockNormal = false;
                                                railBlockNormalCB.setChecked(false);
                                            }
                                        });

        railBlockNormalCB = new CheckBox(screen, GUIHelpers.getScreenWidth() / 2 - width + 30 - 85, ytop + 1,
                                  GuiText.SELECTOR_TILE_TILT.toString(), rollAndOffsetInfoCache.railBlockNormal,
                                  (_, self) -> {
                                      if(rollAndOffsetInfoCache.rollEffectTile) {
                                          edited = true;
                                          rollAndOffsetInfoCache.railBlockNormal = self.isChecked();
                                      } else if(rollAndOffsetInfoCache.railBlockNormal){
                                          edited = true;
                                          rollAndOffsetInfoCache.railBlockNormal = false;
                                          self.setChecked(false);
                                      } else {
                                          self.setChecked(false);
                                      }
                                  });
        railBlockNormalCB.setVisible(false);//modifiable vanilla block model later

        degreeModeCB = new CheckBox(screen, GUIHelpers.getScreenWidth() / 2 - width + 30 + 2, ytop + 1,
                                           GuiText.SELECTOR_DEGREE_MODE.toString(), rollAndOffsetInfoCache.degreeMode,
                                           (_, self) -> {
                                               edited = true;
                                               rollAndOffsetInfoCache.degreeMode = self.isChecked();
                                               rollMax = rollAndOffsetInfoCache.degreeMode ? 45 : 60;//180 for degree mode later
                                           });

        offsetVertByNormalCB = new CheckBox(screen, GUIHelpers.getScreenWidth() / 2 - width + 30 - 85 - 75, ytop + 12,
                GuiText.SELECTOR_OFFSET_VERT_BY_NORMAL_MODE.toString(), rollAndOffsetInfoCache.offsetVertByNormal,
                (_, self) -> {
                    edited = true;
                    rollAndOffsetInfoCache.offsetVertByNormal = self.isChecked();
                });

//        wayCircleButton = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width + 30 - 85 , ytop, 85, height, "Selected Way: 0"){};//need multiSwitch branch merging

        rollOffsetTypeButton = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width + 30 + 85, ytop, 85, height,
                                      rollAndOffsetInfoCache.rollOffsetType.toString(), (hand, self) -> {
            edited = true;

            int order = rollAndOffsetInfoCache.rollOffsetType.getOrder();
            int count = RollAndOffsetInfo.RollAndVertOffsetAlignType.values().length;
            order = (order + (hand == Player.Hand.SECONDARY ? 1 : -1) + count) % count;

            int finalOrder = order;
            rollAndOffsetInfoCache.rollOffsetType = RollAndOffsetInfo.RollAndVertOffsetAlignType.byOrder(finalOrder);

            self.setText(rollAndOffsetInfoCache.rollOffsetType.toString());
        });
        rollOffsetTypeButton.setTooltip(List.of(GuiText.TRACK_EXTRA_ROLL_OFFSET_TYPE.toString()));

        ytop += height;
        ytop += 5;

        rollValueInput = new TextField(screen, GUIHelpers.getScreenWidth() / 2 - width / 4, ytop, width / 4, height);
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
                boolean feedback = rollAndOffsetInfoCache.tryDeltaValue(format(ArcLenFactorSlider.getValue()), val, RollAndOffsetInfo.ExtraInfoType.ROLL);
                if(feedback) {
                    updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.ROLL);
                    edited = true;
                }
                return feedback;
            }
            return false;
        });
        rollValueLabel = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width, ytop, 150, height, "", (_, _) -> {});
        rollValueLabel.setEnabled(false);

        ytop += height;

        rollSlopeInput = new TextField(screen, GUIHelpers.getScreenWidth() / 2 - width / 4, ytop, width / 4, height);
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
            float max = 500f;
            if (Math.abs(val) <= max) {
                boolean feedback = rollAndOffsetInfoCache.trySetSlope(format(ArcLenFactorSlider.getValue()), val, RollAndOffsetInfo.ExtraInfoType.ROLL, length);
                if(feedback) {
                    updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.ROLL);
                    edited = true;
                }
                return feedback;
            }
            return false;
        });
        rollSlopeLabel = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width, ytop, 150, height, "", (_, _) -> {});
        rollSlopeLabel.setEnabled(false);

        ytop += height;

        rollHandleXLenInput = new TextField(screen, GUIHelpers.getScreenWidth() / 2 - width / 4, ytop, width / 4, height);
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
                boolean feedback = rollAndOffsetInfoCache.trySetHandleXLen(format(ArcLenFactorSlider.getValue()), val, RollAndOffsetInfo.ExtraInfoType.ROLL, editLeft, length);
                if(feedback) {
                    updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.ROLL);
                    edited = true;
                }
                return feedback;
            }
            return false;
        });
        rollHandleXLenLabel = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width, ytop, 150, height, "", (_, _) -> {});
        rollHandleXLenLabel.setEnabled(false);

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
                boolean feedback = rollAndOffsetInfoCache.tryDeltaValue(format(ArcLenFactorSlider.getValue()), val, RollAndOffsetInfo.ExtraInfoType.Y_OFFSET);
                if(feedback) {
                    updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.Y_OFFSET);
                    edited = true;
                }
                return feedback;
            }
            return false;
        });
        yOffsetValueLabel = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width, ytop, 150, height, "", (_, _) -> {});
        yOffsetValueLabel.setEnabled(false);

        ytop += height;

        yOffsetSlopeInput = new TextField(screen, GUIHelpers.getScreenWidth() / 2 - width / 4, ytop, width / 4, height);
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
            float max = 500f;
            if (Math.abs(val) <= max) {
                boolean feedback = rollAndOffsetInfoCache.trySetSlope(format(ArcLenFactorSlider.getValue()), val, RollAndOffsetInfo.ExtraInfoType.Y_OFFSET, length);
                if(feedback) {
                    updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.Y_OFFSET);
                    edited = true;
                }
                return feedback;
            }
            return false;
        });
        yOffsetSlopeLabel = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width, ytop, 150, height, "", (_, _) -> {});
        yOffsetSlopeLabel.setEnabled(false);

        ytop += height;

        yOffsetHandleXLenInput = new TextField(screen, GUIHelpers.getScreenWidth() / 2 - width / 4, ytop, width / 4, height);
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
                boolean feedback = rollAndOffsetInfoCache.trySetHandleXLen(format(ArcLenFactorSlider.getValue()), val, RollAndOffsetInfo.ExtraInfoType.Y_OFFSET, editLeft, length);
                if(feedback) {
                    updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.Y_OFFSET);
                    edited = true;
                }
                return feedback;
            }
            return false;
        });
        yOffsetHandleXLenLabel = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width, ytop, 150, height, "", (_, _) -> {});
        yOffsetHandleXLenLabel.setEnabled(false);

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
                boolean feedback = rollAndOffsetInfoCache.tryDeltaValue(format(ArcLenFactorSlider.getValue()), val, RollAndOffsetInfo.ExtraInfoType.Z_OFFSET);
                if(feedback) {
                    updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.Z_OFFSET);
                    edited = true;
                }
                return feedback;
            }
            return false;
        });
        zOffsetValueLabel = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width, ytop, 150, height, "", (_, _) -> {});
        zOffsetValueLabel.setEnabled(false);

        ytop += height;

        zOffsetSlopeInput = new TextField(screen, GUIHelpers.getScreenWidth() / 2 - width / 4, ytop, width / 4, height);
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
            float max = 500f;
            if (Math.abs(val) <= max) {
                boolean feedback = rollAndOffsetInfoCache.trySetSlope(format(ArcLenFactorSlider.getValue()), val, RollAndOffsetInfo.ExtraInfoType.Z_OFFSET, length);
                if(feedback) {
                    updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.Z_OFFSET);
                    edited = true;
                }
                return feedback;
            }
            return false;
        });
        zOffsetSlopeLabel = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width, ytop, 150, height, "", (_, _) -> {});
        zOffsetSlopeLabel.setEnabled(false);

        ytop += height;

        zOffsetHandleXLenInput = new TextField(screen, GUIHelpers.getScreenWidth() / 2 - width / 4, ytop, width / 4, height);
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
                boolean feedback = rollAndOffsetInfoCache.trySetHandleXLen(format(ArcLenFactorSlider.getValue()), val, RollAndOffsetInfo.ExtraInfoType.Z_OFFSET, editLeft, length);
                if(feedback) {
                    updateCurveInfoDisplay(RollAndOffsetInfo.ExtraInfoType.Z_OFFSET);
                    edited = true;
                }
                return feedback;
            }
            return false;
        });
        zOffsetHandleXLenLabel = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width, ytop, 150, height, "", (_, _) -> {});
        zOffsetHandleXLenLabel.setEnabled(false);

        //Update after all components init
        ArcLenFactorSlider.onSlider();
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
            new ItemRailUpdatePacket(te.getPos(), settings.immutable(), targetGuiOpenType).sendToServer();

            //Also update client Item to update Rail information
            ItemStack clientStack = te.getItem();
            settings.immutable().write(clientStack);
            ItemTrackBlueprint.Data data = new ItemTrackBlueprint.Data(clientStack);
            data.guiOpenType = targetGuiOpenType;
            data.write();
            te.setItem(clientStack, MinecraftClient.getPlayer());
        } else {
            new ItemRailUpdatePacket(settings.immutable(), targetGuiOpenType).sendToServer();

            //Also update client Item to update Rail information
            ItemStack clientStack = MinecraftClient.getPlayer().getHeldItem(Player.Hand.PRIMARY);
            settings.immutable().write(clientStack);
            ItemTrackBlueprint.Data data = new ItemTrackBlueprint.Data(clientStack);
            data.guiOpenType = targetGuiOpenType;
            data.write();
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
        double textScale = 1;
        GUIHelpers.drawCenteredString(RollAndOffsetInfo.ExtraInfoType.ROLL.toString(), (int) (105  / textScale), (int) ((height + 2) / textScale), 0xFFFFFF, new Matrix4().scale(textScale, textScale, textScale));
        GUIHelpers.drawCenteredString(RollAndOffsetInfo.ExtraInfoType.Y_OFFSET.toString(), (int) (105  / textScale), (int) ((height + 2 + height * 3 + 5) / textScale), 0xFFFFFF, new Matrix4().scale(textScale, textScale, textScale));
        GUIHelpers.drawCenteredString(RollAndOffsetInfo.ExtraInfoType.Z_OFFSET.toString(), (int) (105  / textScale), (int) ((height + 2 + height * 6 + 10) / textScale), 0xFFFFFF, new Matrix4().scale(textScale, textScale, textScale));

        //If choose HIGH or LOW, half of the the roll graph will flip, need to flip it on graph?
        //roll Graph
        state.translate(5, height + 5 + height * 1.5, 0);
        BezierRenderer rollGraph = new BezierRenderer(state, rollAndOffsetInfoCache.toCurves(RollAndOffsetInfo.ExtraInfoType.ROLL, true));
        rollGraph.drawDashLine(Vec3d.ZERO, new Vec3d(1, 0, 0), Color.WHITE, xScale, rollYScale, 1, 0.05f, 0.05f, 0);
        rollGraph.drawBeziers(curveColor, pointColor, handlePointColor, handleLineColor, 100, xScale, rollYScale);
        rollGraph.drawArrow(new Vec3d(format(ArcLenFactorSlider.getValue()), immutable.getRoll(format(ArcLenFactorSlider.getValue())), 0), Color.YELLOW, 2.4, xScale, rollYScale);

        //yOffset Graph
        state.translate(0, height * 3 + 5, 0);
        BezierRenderer yOffsetGraph = new BezierRenderer(state, rollAndOffsetInfoCache.toCurves(RollAndOffsetInfo.ExtraInfoType.Y_OFFSET, true));
        yOffsetGraph.drawDashLine(Vec3d.ZERO, new Vec3d(1, 0, 0), Color.WHITE, xScale, yOffsetYScale, 1, 0.05f, 0.05f, 0);
        yOffsetGraph.drawBeziers(curveColor, pointColor, handlePointColor, handleLineColor, 100, xScale, yOffsetYScale);
        yOffsetGraph.drawArrow(new Vec3d(format(ArcLenFactorSlider.getValue()), immutable.getYOffset(format(ArcLenFactorSlider.getValue())), 0), Color.YELLOW, 2.4, xScale, yOffsetYScale);

        //zOffset Graph
        state.translate(0, height * 3 + 5, 0);
        BezierRenderer zOffsetGraph = new BezierRenderer(state, rollAndOffsetInfoCache.toCurves(RollAndOffsetInfo.ExtraInfoType.Z_OFFSET, true));
        zOffsetGraph.drawDashLine(Vec3d.ZERO, new Vec3d(1, 0, 0), Color.WHITE, xScale, zOffsetYScale, 1, 0.05f, 0.05f, 0);
        zOffsetGraph.drawBeziers(curveColor, pointColor, handlePointColor, handleLineColor, 100, xScale, zOffsetYScale);
        zOffsetGraph.drawArrow(new Vec3d(format(ArcLenFactorSlider.getValue()), immutable.getZOffset(format(ArcLenFactorSlider.getValue())), 0), Color.YELLOW, 2.4, xScale, zOffsetYScale);

    }

    private void updateSliderRelated() {
        if (insertOrDeletePointButton != null) {
            if (rollAndOffsetInfoCache.findPhysicalIndex(format(ArcLenFactorSlider.getValue())) == -1) {
                insertOrDeletePointButton.setText(GuiText.TRACK_EXTRA_INSERT_POINT.toString());
            } else {
                insertOrDeletePointButton.setText(GuiText.TRACK_EXTRA_DELETE_POINT.toString());
            }
        }
        for (TextField tf : List.of(rollValueInput, rollSlopeInput, rollHandleXLenInput,
                                     yOffsetValueInput, yOffsetSlopeInput, yOffsetHandleXLenInput,
                                     zOffsetValueInput, zOffsetSlopeInput, zOffsetHandleXLenInput)) {
            if (tf != null) tf.setText("");
        }
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
            if (valueLabel != null) valueLabel.setText(GuiText.TRACK_EXTRA_POINT_VALUE_CM_DEGREE + rollAndOffsetInfoCache.getValueDisplay(format(ArcLenFactorSlider.getValue()), type));
        } else {
            if (valueLabel != null) valueLabel.setText(GuiText.TRACK_EXTRA_POINT_VALUE_M + rollAndOffsetInfoCache.getValueDisplay(format(ArcLenFactorSlider.getValue()), type));
        }
        if (slopeLabel != null) slopeLabel.setText(GuiText.TRACK_EXTRA_POINT_SLOPE + rollAndOffsetInfoCache.getSlopeDisplay(format(ArcLenFactorSlider.getValue()), type, length));
        if (handleXLenLabel != null) handleXLenLabel.setText(GuiText.TRACK_EXTRA_POINT_WEIGHT + rollAndOffsetInfoCache.getHandleXDisplay(format(ArcLenFactorSlider.getValue()), type, editLeft, length));
    }

    private static double format(double value) {
        // Cast precision to 0.01
        return Math.round(value * 100.0) / 100.0;
    }
 }
