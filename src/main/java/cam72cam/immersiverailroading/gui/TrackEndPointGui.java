package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.items.ItemTrackBlueprint;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.*;
import cam72cam.immersiverailroading.net.ItemRailUpdatePacket;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.track.CubicCurve;
import cam72cam.immersiverailroading.util.EndPointData;
import cam72cam.immersiverailroading.util.IRFuzzy;
import cam72cam.immersiverailroading.util.TrackSnapSettings;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui.screen.*;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.opengl.RenderState;

import java.util.ArrayList;
import java.util.List;

import static cam72cam.immersiverailroading.gui.ClickListHelper.next;

public class TrackEndPointGui implements IScreen {
    private TileRailPreview te;
    private int targetGuiOpenType;
    private boolean unlockGuiTurnDegree;
    private final List<ItemStack> oreDict;
    private RailSettings.Mutable settings;

    // Near/Far Label
    private Button nearLabel;
    private Button farLabel;

    // Transition Curve (Cubic Parabola)
    private TextField nearRadiusInput;
    private Button nearRadiusLabel;

    private Button farRadiusLabel;
    private TextField farRadiusInput;

    // Height Offset
    private Button nearHeightOffsetLabel;
    private TextField nearHeightOffsetInput;

    private Button farHeightOffsetLabel;
    private TextField farHeightOffsetInput;

    // Pitch
    private Button nearPitchLabel;
    private TextField nearPitchInput;
    private Button nearPitchSettingButton;

    private Button farPitchLabel;
    private TextField farPitchInput;
    private Button farPitchSettingButton;

    // PosType
    private Button nearPosTypeButton;
    private TextField nearYawInput;
    private Button nearPosYawAlignSelector;

    private Button farPosTypeButton;
    private TextField farYawInput;
    private Button farPosYawTypeSelector;

    // Track Snapping
    private CheckBox nearPosSnapCB;
    private CheckBox farPosSnapCB;
    private CheckBox nearHeightSnapCB;
    private CheckBox farHeightSnapCB;
    private CheckBox nearYawSnapCB;
    private CheckBox farYawSnapCB;
    private CheckBox nearPitchSnapCB;
    private CheckBox farPitchSnapCB;
    private CheckBox nearRollSnapCB;
    private CheckBox farRollSnapCB;

    // Track Snapping Pos Offset
    private Button nearSnapOffsetLabel;
    private TextField nearSnapOffsetForwardInput;
    private TextField nearSnapOffsetRightInput;
    private TextField nearSnapOffsetUpInput;
    private Button farSnapOffsetLabel;
    private TextField farSnapOffsetForwardInput;
    private TextField farSnapOffsetRightInput;
    private TextField farSnapOffsetUpInput;

    private Button trackGuiButton;

    public TrackEndPointGui() {
        this(MinecraftClient.getPlayer().getHeldItem(Player.Hand.PRIMARY));
    }

    public TrackEndPointGui(TileRailPreview te) {
        this(te.getItem());
        this.te = te;
    }

    private TrackEndPointGui(ItemStack stack) {
        stack = stack.copy();
        settings = RailSettings.from(stack).mutable();
        ItemTrackBlueprint.Data data = new ItemTrackBlueprint.Data(stack);
        targetGuiOpenType = data.guiOpenType;
        unlockGuiTurnDegree = data.unlockGuiTurnDegree;

        oreDict = new ArrayList<>();
        oreDict.add(ItemStack.EMPTY);
        oreDict.addAll(IRFuzzy.IR_RAIL_BED.enumerate());
    }

    @Override
    public void init(IScreenBuilder screen) {
        int width = 200;
        int height = 20;
        int left_xStart = -GUIHelpers.getScreenWidth() / 2;
        int right_xStart = GUIHelpers.getScreenWidth() / 2 - width + 30;
        int ytop = -GUIHelpers.getScreenHeight() / 4;

        // TrackGui
        trackGuiButton = new Button(screen, - width / 4, ytop, width / 2, height,
                GuiText.TRACK_EXTRA_TO_MAIN.toString(), (_, _) -> {
            targetGuiOpenType = 0;
            onClose();
            if (te != null) {
                GuiTypes.RAIL_PREVIEW.open(MinecraftClient.getPlayer(), te.getPos());
            } else {
                GuiTypes.RAIL.open(MinecraftClient.getPlayer());
            }
        });

        // Near/Far Label
        nearLabel = new Button(screen, left_xStart, ytop, width - 30, height, GuiText.LABEL_NEAR.toString(), (_, _) -> {});
        nearLabel.setEnabled(false);
        farLabel = new Button(screen, right_xStart, ytop, width - 30, height, GuiText.LABEL_FAR.toString(), (_, _) -> {});
        farLabel.setEnabled(false);

        ytop += height;

        // Transition Radius
        nearRadiusLabel = new Button(screen, left_xStart, ytop, width / 2 + 20, height, GuiText.LABEL_TRANSITION_RADIUS.toString(), (hand, button) -> {
            swapTransitionRadius();
        });
        nearRadiusLabel.setTooltip(List.of(GuiText.LABEL_SWAP_RADIUS.toString()));

        farRadiusLabel = new Button(screen, right_xStart, ytop, width / 2 + 20, height, GuiText.LABEL_TRANSITION_RADIUS.toString(), (hand, button) -> {
            swapTransitionRadius();
        });
        farRadiusLabel.setTooltip(List.of(GuiText.LABEL_SWAP_RADIUS.toString()));

        nearRadiusInput = new TextField(screen, left_xStart + width / 2 + 20, ytop, width / 4, height);
        nearRadiusInput.setText("" + (int) settings.nearPointData.radius());
        nearRadiusInput.setValidator(s -> {
            if (s == null || s.length() == 0) {
                return true;
            }
            int val;
            try {
                val = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return s.equals("-");
            }
            int max = 1000;

            if (val > -1e-6 && val <= max && CubicCurve.isCubicParabolaInputValid(val, settings.farPointData.radius(), settings.degrees)) {
                settings.nearPointData = settings.nearPointData.with(mutable -> mutable.radius = val);
                return true;
            }

            return false;
        });
        nearRadiusInput.setFocused(true);

        farRadiusInput = new TextField(screen, right_xStart + width / 2 + 20, ytop, width / 4, height);
        farRadiusInput.setText("" + (int) settings.farPointData.radius());
        farRadiusInput.setValidator(s -> {
            if (s == null || s.length() == 0) {
                return true;
            }
            int val;
            try {
                val = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return s.equals("-");
            }
            int max = 1000;

            if (val > -1e-6 && val <= max && CubicCurve.isCubicParabolaInputValid(settings.nearPointData.radius(), val, settings.degrees)) {
                settings.farPointData = settings.farPointData.with(mutable -> mutable.radius = val);
                return true;
            }

            return false;
        });
        farRadiusInput.setFocused(true);

        ytop += height;

        // Height Offset

        nearHeightOffsetLabel = new Button(screen, left_xStart, ytop, width / 2 + 20, height, GuiText.LABEL_HEIGHT_OFFSET.toString(), (hand, button) -> {
            settings.nearPointData = settings.nearPointData.with(mutable -> mutable.offset = Vec3d.ZERO);
            nearHeightOffsetInput.setText("" + (float) settings.nearPointData.offset().y);
        });
        nearHeightOffsetLabel.setTooltip(List.of(GuiText.LABEL_RESET_HEIGHT_OFFSET.toString()));

        farHeightOffsetLabel = new Button(screen, right_xStart, ytop, width / 2 + 20, height, GuiText.LABEL_HEIGHT_OFFSET.toString(), (hand, button) -> {
            settings.farPointData = settings.farPointData.with(mutable -> mutable.offset = Vec3d.ZERO);
            farHeightOffsetInput.setText("" + (float) settings.farPointData.offset().y);
        });
        farHeightOffsetLabel.setTooltip(List.of(GuiText.LABEL_RESET_HEIGHT_OFFSET.toString()));

        nearHeightOffsetInput = new TextField(screen, left_xStart + width / 2 + 20, ytop, width / 4, height);
        nearHeightOffsetInput.setText("" + (float) settings.nearPointData.offset().y);
        nearHeightOffsetInput.setValidator(s -> {
            if (s == null || s.isEmpty()) {
                return true;
            }
            float val;
            try {
                val = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                return s.equals(".") || s.equals("-");
            }
            float max = 2f;
            float min = -2f;
            if (val >= min && val <= max) {
                settings.nearPointData = settings.nearPointData.with(mutable -> mutable.offset = new Vec3d(0, val, 0));
                return true;
            }
            return false;
        });
        nearHeightOffsetInput.setFocused(true);

        farHeightOffsetInput = new TextField(screen, right_xStart + width / 2 + 20, ytop, width / 4, height);
        farHeightOffsetInput.setText("" + (float) settings.farPointData.offset().y);
        farHeightOffsetInput.setValidator(s -> {
            if (s == null || s.isEmpty()) {
                return true;
            }
            float val;
            try {
                val = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                return s.equals(".") || s.equals("-");
            }
            float max = 2f;
            float min = -2f;
            if (val >= min && val <= max) {
                settings.farPointData = settings.farPointData.with(mutable -> mutable.offset = new Vec3d(0, val, 0));
                return true;
            }
            return false;
        });
        farHeightOffsetInput.setFocused(true);

        ytop += height;

        // Pitch

        nearPitchLabel = new Button(screen, left_xStart, ytop, width / 2 + 20, height, GuiText.LABEL_PITCH.toString(), (hand, button) -> {
            settings.nearPointData = settings.nearPointData.with(mutable -> mutable.pitch = 0);
            nearPitchInput.setText("" + settings.nearPointData.pitch());
        });
        nearPitchLabel.setTooltip(List.of(GuiText.LABEL_RESET_PITCH.toString()));

        farPitchLabel = new Button(screen, right_xStart, ytop, width / 2 + 20, height, GuiText.LABEL_PITCH.toString(), (hand, button) -> {
            settings.farPointData = settings.farPointData.with(mutable -> mutable.pitch = 0);
            farPitchInput.setText("" + settings.farPointData.pitch());
        });
        farPitchLabel.setTooltip(List.of(GuiText.LABEL_RESET_PITCH.toString()));

        nearPitchInput = new TextField(screen, left_xStart + width / 2 + 20, ytop, width / 4, height);
        nearPitchInput.setText("" + settings.nearPointData.pitch());
        nearPitchInput.setValidator(s -> {
            if (s == null || s.isEmpty()) {
                return true;
            }
            float val;
            try {
                val = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                return s.equals(".") || s.equals("-");
            }
            float max = settings.nearPointData.pitchDegreeMode() ? 26.565f : 500;// Math.atan(500 / 1000) ≈ 26.5650
            float min = -max;
            if (val >= min && val <= max) {
                settings.nearPointData = settings.nearPointData.with(mutable -> mutable.pitch = val);
                return true;
            }
            return false;
        });
        nearPitchInput.setFocused(true);

        farPitchInput = new TextField(screen, right_xStart + width / 2 + 20, ytop, width / 4, height);
        farPitchInput.setText("" + settings.farPointData.pitch());
        farPitchInput.setValidator(s -> {
            if (s == null || s.isEmpty()) {
                return true;
            }
            float val;
            try {
                val = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                return s.equals(".") || s.equals("-");
            }
            float max = settings.farPointData.pitchDegreeMode() ? 26.565f : 500;// Math.atan(500 / 1000) ≈ 26.5650
            float min = -max;
            if (val >= min && val <= max) {
                settings.farPointData = settings.farPointData.with(mutable -> mutable.pitch = val);
                return true;
            }
            return false;
        });
        farPitchInput.setFocused(true);

        ytop += height;

        nearPitchSettingButton = new Button(screen, left_xStart, ytop, width - 30, height, settings.nearPointData.getPitchSetting(), (hand, button) -> {
            settings.nearPointData = circlePitchSetting(settings.nearPointData);
            button.setText(settings.nearPointData.getPitchSetting());
            nearPitchInput.setText("" + settings.nearPointData.pitch());
        });
        nearPitchSettingButton.setTooltip(List.of(GuiText.LABEL_PITCH_SETTING.toString()));

        farPitchSettingButton = new Button(screen, right_xStart, ytop, width - 30, height, settings.farPointData.getPitchSetting(), (hand, button) -> {
            settings.farPointData = circlePitchSetting(settings.farPointData);
            button.setText(settings.farPointData.getPitchSetting());
            farPitchInput.setText("" + settings.farPointData.pitch());
        });
        farPitchSettingButton.setTooltip(List.of(GuiText.LABEL_PITCH_SETTING.toString()));

        // Bottom Page
        ytop = (int) (GUIHelpers.getScreenHeight() * 0.75 - height * 7);

        // Pos Type

        nearPosTypeButton = new Button(screen, left_xStart, ytop, width - 30, height, GuiText.SELECTOR_POSITION.toString(settings.nearPointData.posType()), (hand, button) -> {
            settings.nearPointData = settings.nearPointData.with(mutable -> mutable.posType = next(settings.nearPointData.posType(), hand));
            button.setText(GuiText.SELECTOR_POSITION.toString(settings.nearPointData.posType()));
        });
        farPosTypeButton = new Button(screen, right_xStart, ytop, width - 30, height, GuiText.SELECTOR_POSITION.toString(settings.farPointData.posType()), (hand, button) -> {
            settings.farPointData = settings.farPointData.with(mutable -> mutable.posType = next(settings.farPointData.posType(), hand));
            button.setText(GuiText.SELECTOR_POSITION.toString(settings.farPointData.posType()));
        });

        ytop += height;

        nearPosYawAlignSelector = new Button(screen, left_xStart, ytop, width - 30, height, GuiText.SELECTOR_YAW_ALIGN.toString(settings.nearPointData.posYawType()), (hand, button) -> {
            settings.nearPointData = settings.nearPointData.with(mutable -> mutable.posYawType = next(mutable.posYawType, hand));
            nearYawInput.setEnabled(settings.nearPointData.posYawType() == TrackYawAlignmentType.ANGLE_SPECIFIED);
            button.setText(GuiText.SELECTOR_YAW_ALIGN.toString(settings.nearPointData.posYawType()));
        });
        nearPosYawAlignSelector.setTooltip(List.of(GuiText.LABEL_POS_YAW_TYPE.toString()));

        farPosYawTypeSelector = new Button(screen, right_xStart, ytop, width - 30, height, GuiText.SELECTOR_YAW_ALIGN.toString(settings.farPointData.posYawType()), (hand, button) -> {
            settings.farPointData = settings.farPointData.with(mutable -> mutable.posYawType = next(mutable.posYawType, hand));
            farYawInput.setEnabled(settings.farPointData.posYawType() == TrackYawAlignmentType.ANGLE_SPECIFIED);
            button.setText(GuiText.SELECTOR_YAW_ALIGN.toString(settings.farPointData.posYawType()));
        });
        farPosYawTypeSelector.setTooltip(List.of(GuiText.LABEL_POS_YAW_TYPE.toString()));

        ytop += height;

        nearYawInput = new TextField(screen, left_xStart, ytop, width - 30, height);
        nearYawInput.setText("" + settings.nearPointData.posYaw());
        nearYawInput.setValidator(s -> {
            if (s == null || s.isEmpty()) {
                return true;
            }
            float val;
            try {
                val = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                return s.equals(".") || s.equals("-");
            }
            float max = 90;
            float min = 0;
            if (val >= min && val <= max) {
                settings.nearPointData = settings.nearPointData.with(mutable -> mutable.posYaw = val);
                return true;
            }
            return false;
        });
        nearYawInput.setFocused(true);

        farYawInput = new TextField(screen, right_xStart, ytop, width - 30, height);
        farYawInput.setText("" + settings.farPointData.posYaw());
        farYawInput.setValidator(s -> {
            if (s == null || s.isEmpty()) {
                return true;
            }
            float val;
            try {
                val = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                return s.equals(".") || s.equals("-");
            }
            float max = 90;
            float min = 0;
            if (val >= min && val <= max) {
                settings.farPointData = settings.farPointData.with(mutable -> mutable.posYaw = val);
                return true;
            }
            return false;
        });
        farYawInput.setFocused(true);

        ytop += height;

        // Track Snap

        nearPosSnapCB = new CheckBox(screen, left_xStart, ytop, GuiText.LABEL_SNAP_POS.toString(), settings.nearPointData.trackSnapSettings().snapPos(), (hand, self) -> {
            TrackSnapSettings trackSnapSettings = settings.nearPointData.trackSnapSettings().with(mutable -> mutable.snapPos = self.isChecked());
            settings.nearPointData = settings.nearPointData.with(mutable -> mutable.trackSnapSettings = trackSnapSettings);
            setNearSnapComponentsVisibility();
        });

        farPosSnapCB = new CheckBox(screen, right_xStart, ytop, GuiText.LABEL_SNAP_POS.toString(), settings.farPointData.trackSnapSettings().snapPos(), (hand, self) -> {
            TrackSnapSettings trackSnapSettings = settings.farPointData.trackSnapSettings().with(mutable -> mutable.snapPos = self.isChecked());
            settings.farPointData = settings.farPointData.with(mutable -> mutable.trackSnapSettings = trackSnapSettings);
            setFarSnapComponentsVisibility();
        });

        ytop += height;

        nearHeightSnapCB = new CheckBox(screen, left_xStart, ytop, GuiText.LABEL_SNAP_HEIGHT.toString(), settings.nearPointData.trackSnapSettings().snapHeight(), (hand, self) -> {
            TrackSnapSettings trackSnapSettings = settings.nearPointData.trackSnapSettings().with(mutable -> mutable.snapHeight = self.isChecked());
            settings.nearPointData = settings.nearPointData.with(mutable -> mutable.trackSnapSettings = trackSnapSettings);
            setNearSnapComponentsVisibility();
        });

        farHeightSnapCB = new CheckBox(screen, right_xStart, ytop, GuiText.LABEL_SNAP_HEIGHT.toString(), settings.farPointData.trackSnapSettings().snapHeight(), (hand, self) -> {
            TrackSnapSettings trackSnapSettings = settings.farPointData.trackSnapSettings().with(mutable -> mutable.snapHeight = self.isChecked());
            settings.farPointData = settings.farPointData.with(mutable -> mutable.trackSnapSettings = trackSnapSettings);
            setFarSnapComponentsVisibility();
        });

        nearYawSnapCB = new CheckBox(screen, left_xStart + width / 2 - 15, ytop, GuiText.LABEL_SNAP_YAW.toString(), settings.nearPointData.trackSnapSettings().snapYaw(), (hand, self) -> {
            TrackSnapSettings trackSnapSettings = settings.nearPointData.trackSnapSettings().with(mutable -> mutable.snapYaw = self.isChecked());
            settings.nearPointData = settings.nearPointData.with(mutable -> mutable.trackSnapSettings = trackSnapSettings);
        });

        farYawSnapCB = new CheckBox(screen, right_xStart + width / 2 - 15, ytop, GuiText.LABEL_SNAP_YAW.toString(), settings.farPointData.trackSnapSettings().snapYaw(), (hand, self) -> {
            TrackSnapSettings trackSnapSettings = settings.farPointData.trackSnapSettings().with(mutable -> mutable.snapYaw = self.isChecked());
            settings.farPointData = settings.farPointData.with(mutable -> mutable.trackSnapSettings = trackSnapSettings);
        });

        ytop += height;

        nearPitchSnapCB = new CheckBox(screen, left_xStart, ytop, GuiText.LABEL_SNAP_PITCH.toString(), settings.nearPointData.trackSnapSettings().snapPitch(), (hand, self) -> {
            TrackSnapSettings trackSnapSettings = settings.nearPointData.trackSnapSettings().with(mutable -> mutable.snapPitch = self.isChecked());
            settings.nearPointData = settings.nearPointData.with(mutable -> mutable.trackSnapSettings = trackSnapSettings);
        });

        farPitchSnapCB = new CheckBox(screen, right_xStart, ytop, GuiText.LABEL_SNAP_PITCH.toString(), settings.farPointData.trackSnapSettings().snapPitch(), (hand, self) -> {
            TrackSnapSettings trackSnapSettings = settings.farPointData.trackSnapSettings().with(mutable -> mutable.snapPitch = self.isChecked());
            settings.farPointData = settings.farPointData.with(mutable -> mutable.trackSnapSettings = trackSnapSettings);
        });

        nearRollSnapCB = new CheckBox(screen, left_xStart + width / 2 - 15, ytop, GuiText.LABEL_SNAP_ROLL.toString(), settings.nearPointData.trackSnapSettings().snapRoll(), (hand, self) -> {
            TrackSnapSettings trackSnapSettings = settings.nearPointData.trackSnapSettings().with(mutable -> mutable.snapRoll = self.isChecked());
            settings.nearPointData = settings.nearPointData.with(mutable -> mutable.trackSnapSettings = trackSnapSettings);
        });

        farRollSnapCB = new CheckBox(screen, right_xStart + width / 2 - 15, ytop, GuiText.LABEL_SNAP_ROLL.toString(), settings.farPointData.trackSnapSettings().snapRoll(), (hand, self) -> {
            TrackSnapSettings trackSnapSettings = settings.farPointData.trackSnapSettings().with(mutable -> mutable.snapRoll = self.isChecked());
            settings.farPointData = settings.farPointData.with(mutable -> mutable.trackSnapSettings = trackSnapSettings);
        });

        ytop += height;

        // Snap Offset
        nearSnapOffsetLabel = new Button(screen, left_xStart, ytop, 50, height, GuiText.LABEL_SNAP_POS_OFFSET.toString(), (hand, button) -> {
            TrackSnapSettings trackSnapSettings = settings.nearPointData.trackSnapSettings().with(mutable -> mutable.snapOffset = Vec3d.ZERO);
            settings.nearPointData = settings.nearPointData.with(mutable -> mutable.trackSnapSettings = trackSnapSettings);
            nearSnapOffsetForwardInput.setText("" + (float) settings.nearPointData.trackSnapSettings().snapOffset().x);
            nearSnapOffsetUpInput.setText("" + (float) settings.nearPointData.trackSnapSettings().snapOffset().y);
            nearSnapOffsetRightInput.setText("" + (float) settings.nearPointData.trackSnapSettings().snapOffset().z);
        });

        nearSnapOffsetForwardInput = new TextField(screen, left_xStart + 50, ytop, 40, height);
        nearSnapOffsetForwardInput.setText("" + (float) settings.nearPointData.trackSnapSettings().snapOffset().x);
        nearSnapOffsetForwardInput.setValidator(s -> {
            if (s == null || s.isEmpty()) {
                return true;
            }
            float val;
            try {
                val = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                return s.equals(".") || s.equals("-");
            }
            float max = 1f;
            float min = -1f;
            if (val >= min && val <= max) {
                TrackSnapSettings trackSnapSettings = settings.nearPointData.trackSnapSettings().with(mutable -> mutable.snapOffset = new Vec3d(val, mutable.snapOffset.y, mutable.snapOffset.z));
                settings.nearPointData = settings.nearPointData.with(mutable -> mutable.trackSnapSettings = trackSnapSettings);
                return true;
            }
            return false;
        });
        nearSnapOffsetForwardInput.setFocused(true);

        nearSnapOffsetUpInput = new TextField(screen, left_xStart + 50 + 40, ytop, 40, height);
        nearSnapOffsetUpInput.setText("" + (float) settings.nearPointData.trackSnapSettings().snapOffset().y);
        nearSnapOffsetUpInput.setValidator(s -> {
            if (s == null || s.isEmpty()) {
                return true;
            }
            float val;
            try {
                val = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                return s.equals(".") || s.equals("-");
            }
            float max = 1f;
            float min = -1f;
            if (val >= min && val <= max) {
                TrackSnapSettings trackSnapSettings = settings.nearPointData.trackSnapSettings().with(mutable -> mutable.snapOffset = new Vec3d(mutable.snapOffset.x, val, mutable.snapOffset.z));
                settings.nearPointData = settings.nearPointData.with(mutable -> mutable.trackSnapSettings = trackSnapSettings);
                return true;
            }
            return false;
        });
        nearSnapOffsetUpInput.setFocused(true);

        nearSnapOffsetRightInput = new TextField(screen, left_xStart + 50 + 40 * 2, ytop, 40, height);
        nearSnapOffsetRightInput.setText("" + (float) settings.nearPointData.trackSnapSettings().snapOffset().z);
        nearSnapOffsetRightInput.setValidator(s -> {
            if (s == null || s.isEmpty()) {
                return true;
            }
            float val;
            try {
                val = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                return s.equals(".") || s.equals("-");
            }
            float max = 1f;
            float min = -1f;
            if (val >= min && val <= max) {
                TrackSnapSettings trackSnapSettings = settings.nearPointData.trackSnapSettings().with(mutable -> mutable.snapOffset = new Vec3d(mutable.snapOffset.x, mutable.snapOffset.y, val));
                settings.nearPointData = settings.nearPointData.with(mutable -> mutable.trackSnapSettings = trackSnapSettings);
                return true;
            }
            return false;
        });
        nearSnapOffsetRightInput.setFocused(true);

        farSnapOffsetLabel = new Button(screen, right_xStart, ytop, 50, height, GuiText.LABEL_SNAP_POS_OFFSET.toString(), (hand, button) -> {
            TrackSnapSettings trackSnapSettings = settings.farPointData.trackSnapSettings().with(mutable -> mutable.snapOffset = Vec3d.ZERO);
            settings.farPointData = settings.farPointData.with(mutable -> mutable.trackSnapSettings = trackSnapSettings);
            farSnapOffsetForwardInput.setText("" + (float) settings.farPointData.trackSnapSettings().snapOffset().x);
            farSnapOffsetUpInput.setText("" + (float) settings.farPointData.trackSnapSettings().snapOffset().y);
            farSnapOffsetRightInput.setText("" + (float) settings.farPointData.trackSnapSettings().snapOffset().z);
        });

        farSnapOffsetForwardInput = new TextField(screen, right_xStart + 50, ytop, 40, height);
        farSnapOffsetForwardInput.setText("" + (float) settings.farPointData.trackSnapSettings().snapOffset().x);
        farSnapOffsetForwardInput.setValidator(s -> {
            if (s == null || s.isEmpty()) {
                return true;
            }
            float val;
            try {
                val = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                return s.equals(".") || s.equals("-");
            }
            float max = 1f;
            float min = -1f;
            if (val >= min && val <= max) {
                TrackSnapSettings trackSnapSettings = settings.farPointData.trackSnapSettings().with(mutable -> mutable.snapOffset = new Vec3d(val, mutable.snapOffset.y, mutable.snapOffset.z));
                settings.farPointData = settings.farPointData.with(mutable -> mutable.trackSnapSettings = trackSnapSettings);
                return true;
            }
            return false;
        });
        farSnapOffsetForwardInput.setFocused(true);

        farSnapOffsetUpInput = new TextField(screen, right_xStart + 50 + 40, ytop, 40, height);
        farSnapOffsetUpInput.setText("" + (float) settings.farPointData.trackSnapSettings().snapOffset().y);
        farSnapOffsetUpInput.setValidator(s -> {
            if (s == null || s.isEmpty()) {
                return true;
            }
            float val;
            try {
                val = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                return s.equals(".") || s.equals("-");
            }
            float max = 1f;
            float min = -1f;
            if (val >= min && val <= max) {
                TrackSnapSettings trackSnapSettings = settings.farPointData.trackSnapSettings().with(mutable -> mutable.snapOffset = new Vec3d(mutable.snapOffset.x, val, mutable.snapOffset.z));
                settings.farPointData = settings.farPointData.with(mutable -> mutable.trackSnapSettings = trackSnapSettings);
                return true;
            }
            return false;
        });
        farSnapOffsetUpInput.setFocused(true);

        farSnapOffsetRightInput = new TextField(screen, right_xStart + 50 + 40 * 2, ytop, 40, height);
        farSnapOffsetRightInput.setText("" + (float) settings.farPointData.trackSnapSettings().snapOffset().z);
        farSnapOffsetRightInput.setValidator(s -> {
            if (s == null || s.isEmpty()) {
                return true;
            }
            float val;
            try {
                val = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                return s.equals(".") || s.equals("-");
            }
            float max = 1f;
            float min = -1f;
            if (val >= min && val <= max) {
                TrackSnapSettings trackSnapSettings = settings.farPointData.trackSnapSettings().with(mutable -> mutable.snapOffset = new Vec3d(mutable.snapOffset.x, mutable.snapOffset.y, val));
                settings.farPointData = settings.farPointData.with(mutable -> mutable.trackSnapSettings = trackSnapSettings);
                return true;
            }
            return false;
        });
        farSnapOffsetRightInput.setFocused(true);

        // Other
        nearRadiusLabel.setEnabled(settings.type.isTransitionCurve());
        farRadiusLabel.setEnabled(settings.type.isTransitionCurve());
        nearRadiusInput.setEnabled(settings.type.isTransitionCurve());
        farRadiusInput.setEnabled(settings.type.isTransitionCurve());

        nearPitchLabel.setEnabled(settings.type.hasSmoothing() && settings.smoothing == TrackSmoothing.PITCH_SPECIFIED);
        farPitchLabel.setEnabled(settings.type.hasSmoothing() && settings.smoothing == TrackSmoothing.PITCH_SPECIFIED);
        nearPitchInput.setEnabled(settings.type.hasSmoothing() && settings.smoothing == TrackSmoothing.PITCH_SPECIFIED);
        farPitchInput.setEnabled(settings.type.hasSmoothing() && settings.smoothing == TrackSmoothing.PITCH_SPECIFIED);
        nearPitchSettingButton.setEnabled(settings.type.hasSmoothing() && settings.smoothing == TrackSmoothing.PITCH_SPECIFIED);
        farPitchSettingButton.setEnabled(settings.type.hasSmoothing() && settings.smoothing == TrackSmoothing.PITCH_SPECIFIED);

        nearYawInput.setEnabled(settings.nearPointData.posYawType() == TrackYawAlignmentType.ANGLE_SPECIFIED);
        farYawInput.setEnabled(settings.farPointData.posYawType() == TrackYawAlignmentType.ANGLE_SPECIFIED);

        setNearSnapComponentsVisibility();
        setFarSnapComponentsVisibility();
    }

    public void onClose() {
        if (this.te != null) {
            new ItemRailUpdatePacket(te.getPos(), settings.immutable(), targetGuiOpenType, unlockGuiTurnDegree).sendToServer();

            // Update client data here in order to avoid networking lag
            ItemStack clientStack = te.getItem();
            settings.immutable().write(clientStack);
            ItemTrackBlueprint.Data.writeTo(clientStack, targetGuiOpenType, unlockGuiTurnDegree);
            te.setItem(clientStack, MinecraftClient.getPlayer());
        } else {
            new ItemRailUpdatePacket(settings.immutable(), targetGuiOpenType, unlockGuiTurnDegree).sendToServer();

            ItemStack clientStack = MinecraftClient.getPlayer().getHeldItem(Player.Hand.PRIMARY);
            settings.immutable().write(clientStack);
            ItemTrackBlueprint.Data.writeTo(clientStack, targetGuiOpenType, unlockGuiTurnDegree);
            MinecraftClient.getPlayer().setHeldItem(Player.Hand.PRIMARY, clientStack);
        }
    }

    private EndPointData circlePitchSetting(EndPointData data) {
        boolean pitchDegreeMode = data.pitchDegreeMode();
        boolean projectHandle = data.projectHandle();

        return data.with(mutable -> {
            if (!pitchDegreeMode && projectHandle) {
                // ‰ Projection -> ‰ Rotation
                mutable.pitchDegreeMode = false;
                mutable.projectHandle = false;
                mutable.pitch = 0;
            } else if (!pitchDegreeMode) {
                // ‰ Rotation -> Degree Rotation
                mutable.pitchDegreeMode = true;
                mutable.projectHandle = false;
                mutable.pitch = 0;
            } else {
                // Degree Rotation -> ‰ Projection
                mutable.pitchDegreeMode = false;
                mutable.projectHandle = true;
                mutable.pitch = 0;
            }
        });
    }

    private void swapTransitionRadius() {
        float temp = settings.farPointData.radius();
        settings.farPointData = settings.farPointData.with(mutable -> mutable.radius = settings.nearPointData.radius());
        settings.nearPointData = settings.nearPointData.with(mutable -> mutable.radius = temp);

        settings.length = (int) temp;
        nearRadiusInput.setText("" + (int) settings.nearPointData.radius());
        farRadiusInput.setText("" + (int) settings.farPointData.radius());
    }

    private void setNearSnapComponentsVisibility() {
        boolean near = settings.nearPointData.trackSnapSettings().snapPos();
        nearHeightSnapCB.setVisible(near);
        nearYawSnapCB.setVisible(near);
        nearPitchSnapCB.setVisible(near);
        nearRollSnapCB.setVisible(near);

        nearSnapOffsetLabel.setVisible(near);
        nearSnapOffsetForwardInput.setVisible(near);
        nearSnapOffsetRightInput.setVisible(near);
        nearSnapOffsetUpInput.setVisible(near);
    }

    private void setFarSnapComponentsVisibility() {
        boolean far = settings.farPointData.trackSnapSettings().snapPos();
        farHeightSnapCB.setVisible(far);
        farYawSnapCB.setVisible(far);
        farPitchSnapCB.setVisible(far);
        farRollSnapCB.setVisible(far);

        farSnapOffsetLabel.setVisible(far);
        farSnapOffsetForwardInput.setVisible(far);
        farSnapOffsetRightInput.setVisible(far);
        farSnapOffsetUpInput.setVisible(far);
    }

    @Override
    public void draw(IScreenBuilder builder, RenderState state) {
        GUIHelpers.drawRect(GUIHelpers.getScreenWidth() - 200 + 30, 0, 200 - 30, GUIHelpers.getScreenHeight(), 0xEE000000);
        GUIHelpers.drawRect(0, 0, GUIHelpers.getScreenWidth(), GUIHelpers.getScreenHeight(), 0xCC000000);
        GUIHelpers.drawRect(0, 0, 200 - 30, GUIHelpers.getScreenHeight(), 0xEE000000);
    }
}
