package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.items.ItemTrackBlueprint;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.TrackPosYawType;
import cam72cam.immersiverailroading.library.TrackSmoothing;
import cam72cam.immersiverailroading.net.ItemRailUpdatePacket;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.track.CubicCurve;
import cam72cam.immersiverailroading.util.EndPointData;
import cam72cam.immersiverailroading.util.IRFuzzy;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui.screen.*;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

import static cam72cam.immersiverailroading.gui.ClickListHelper.next;

public class TrackEndPointGui implements IScreen {
    private TileRailPreview te;
    private int targetGuiOpenType;
    private final List<ItemStack> oreDict;
    private RailSettings.Mutable settings;

//    private EndPointData.Mutable nearPointData;
//    private EndPointData.Mutable farPointData;

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
    private Button nearPosYawTypeSelector;

    private Button farPosTypeButton;
    private TextField farYawInput;
    private Button farPosYawTypeSelector;

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
        targetGuiOpenType = new ItemTrackBlueprint.Data(stack).guiOpenType;

//        nearPointData = settings.nearPointData.mutable();
//        farPointData = settings.farPointData.mutable();

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

        // Near/Far Label
        nearLabel = new Button(screen, left_xStart, ytop, width - 30, height, GuiText.LABEL_NEAR.toString());
        nearLabel.setEnabled(false);
        farLabel = new Button(screen, right_xStart, ytop, width - 30, height, GuiText.LABEL_FAR.toString());
        farLabel.setEnabled(false);

        ytop += height;

        // Transition Radius
        nearRadiusLabel = new Button(screen, left_xStart, ytop, width / 2 + 20, height, GuiText.LABEL_TRANSITION_RADIUS.toString()) {
            @Override
            public void onClick(Player.Hand hand) {
                swapTransitionRadius();
            }
        };
        nearRadiusLabel.setTooltip(List.of(GuiText.LABEL_SWAP_RADIUS.toString()));

        farRadiusLabel = new Button(screen, right_xStart, ytop, width / 2 + 20, height, GuiText.LABEL_TRANSITION_RADIUS.toString()) {
            @Override
            public void onClick(Player.Hand hand) {
                swapTransitionRadius();
            }
        };
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
                if(s.equals("-")) return true;
                return false;
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
                if(s.equals("-")) return true;
                return false;
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

        nearHeightOffsetLabel = new Button(screen, left_xStart, ytop, width / 2 + 20, height, GuiText.LABEL_HEIGHT_OFFSET.toString()) {
            @Override
            public void onClick(Player.Hand hand) {
                settings.nearPointData = settings.nearPointData.with(mutable -> mutable.offset = Vec3d.ZERO);
                nearHeightOffsetInput.setText("" + (float) settings.nearPointData.offset().y);
            }
        };
        nearHeightOffsetLabel.setTooltip(List.of(GuiText.LABEL_RESET_HEIGHT_OFFSET.toString()));

        farHeightOffsetLabel = new Button(screen, right_xStart, ytop, width / 2 + 20, height, GuiText.LABEL_HEIGHT_OFFSET.toString()) {
            @Override
            public void onClick(Player.Hand hand) {
                settings.farPointData = settings.farPointData.with(mutable -> mutable.offset = Vec3d.ZERO);
                farHeightOffsetInput.setText("" + (float) settings.farPointData.offset().y);
            }
        };
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
                return false;
            }
            float max = 1.0f;
            float min = -1.0f;
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
                return false;
            }
            float max = 1.0f;
            float min = -1.0f;
            if (val >= min && val <= max) {
                settings.farPointData = settings.farPointData.with(mutable -> mutable.offset = new Vec3d(0, val, 0));
                return true;
            }
            return false;
        });
        farHeightOffsetInput.setFocused(true);

        ytop += height;

        // Pitch

        nearPitchLabel = new Button(screen, left_xStart, ytop, width / 2 + 20, height, GuiText.LABEL_PITCH.toString()) {
            @Override
            public void onClick(Player.Hand hand) {
                settings.nearPointData = settings.nearPointData.with(mutable -> mutable.pitch = 0);
                nearPitchInput.setText("" + settings.nearPointData.pitch());
            }
        };
        nearPitchLabel.setTooltip(List.of(GuiText.LABEL_RESET_PITCH.toString()));

        farPitchLabel = new Button(screen, right_xStart, ytop, width / 2 + 20, height, GuiText.LABEL_PITCH.toString()) {
            @Override
            public void onClick(Player.Hand hand) {
                settings.farPointData = settings.farPointData.with(mutable -> mutable.pitch = 0);
                farPitchInput.setText("" + settings.farPointData.pitch());
            }
        };
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
                return false;
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
                return false;
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

        nearPitchSettingButton = new Button(screen, left_xStart, ytop, width - 30, height, settings.nearPointData.getPitchSetting()) {
            @Override
            public void onClick(Player.Hand hand) {
                settings.nearPointData = circlePitchSetting(settings.nearPointData);
                this.setText(settings.nearPointData.getPitchSetting());
                nearPitchInput.setText("" + settings.nearPointData.pitch());
            }
        };
        nearPitchSettingButton.setTooltip(List.of(GuiText.LABEL_PITCH_SETTING.toString()));

        farPitchSettingButton = new Button(screen, right_xStart, ytop, width - 30, height, settings.farPointData.getPitchSetting()) {
            @Override
            public void onClick(Player.Hand hand) {
                settings.farPointData = circlePitchSetting(settings.farPointData);
                this.setText(settings.farPointData.getPitchSetting());
                farPitchInput.setText("" + settings.farPointData.pitch());
            }
        };
        farPitchSettingButton.setTooltip(List.of(GuiText.LABEL_PITCH_SETTING.toString()));

        // Bottom Page
        ytop = (int) (GUIHelpers.getScreenHeight() * 0.75 - height * 4);

        // TrackGui

        trackGuiButton = new Button(screen, right_xStart + width / 2 - 30, ytop, width / 2, height,
                GuiText.TRACK_EXTRA_TO_MAIN.toString(), (_, _) -> {
            targetGuiOpenType = 0;
            onClose();
            if (te != null) {
                GuiTypes.RAIL_PREVIEW.open(MinecraftClient.getPlayer(), te.getPos());
            } else {
                GuiTypes.RAIL.open(MinecraftClient.getPlayer());
            }
        });

        ytop += height;

        // Pos Type TODO: Track Snapping Gui Components

        nearPosTypeButton = new Button(screen, left_xStart, ytop, width - 30, height, GuiText.SELECTOR_POSITION.toString(settings.nearPointData.posType())) {
            @Override
            public void onClick(Player.Hand hand) {
                settings.nearPointData = settings.nearPointData.with(mutable -> mutable.posType = next(settings.nearPointData.posType(), hand));
                nearPosTypeButton.setText(GuiText.SELECTOR_POSITION.toString(settings.nearPointData.posType()));
            }
        };
        farPosTypeButton = new Button(screen, right_xStart, ytop, width - 30, height, GuiText.SELECTOR_POSITION.toString(settings.farPointData.posType())) {
            @Override
            public void onClick(Player.Hand hand) {
                settings.farPointData = settings.farPointData.with(mutable -> mutable.posType = next(settings.farPointData.posType(), hand));
                farPosTypeButton.setText(GuiText.SELECTOR_POSITION.toString(settings.farPointData.posType()));
            }
        };

        ytop += height;

        nearPosYawTypeSelector = new Button(screen, left_xStart, ytop, width - 30, height, GuiText.SELECTOR_POSITION_YAW.toString(settings.nearPointData.posYawType())) {
            @Override
            public void onClick(Player.Hand hand) {
                settings.nearPointData = settings.nearPointData.with(mutable -> mutable.posYawType = next(mutable.posYawType, hand));
                nearYawInput.setEnabled(settings.nearPointData.posYawType() == TrackPosYawType.ANGLE_SPECIFIED);
                this.setText( GuiText.SELECTOR_POSITION_YAW.toString(settings.nearPointData.posYawType()));
            }
        };
        nearPosYawTypeSelector.setTooltip(List.of(GuiText.LABEL_POS_YAW_TYPE.toString()));

        farPosYawTypeSelector = new Button(screen, right_xStart, ytop, width - 30, height, GuiText.SELECTOR_POSITION_YAW.toString(settings.farPointData.posYawType())) {
            @Override
            public void onClick(Player.Hand hand) {
                settings.farPointData = settings.farPointData.with(mutable -> mutable.posYawType = next(mutable.posYawType, hand));
                farYawInput.setEnabled(settings.farPointData.posYawType() == TrackPosYawType.ANGLE_SPECIFIED);
                this.setText(GuiText.SELECTOR_POSITION_YAW.toString(settings.farPointData.posYawType()));
            }
        };
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
                return false;
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
                return false;
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

        nearRadiusLabel.setEnabled(settings.type.isTransitionCurve());
        farRadiusLabel.setEnabled(settings.type.isTransitionCurve());
        nearRadiusInput.setEnabled(settings.type.isTransitionCurve());
        farRadiusInput.setEnabled(settings.type.isTransitionCurve());

        nearPitchLabel.setEnabled(settings.type.hasSmoothing() && settings.smoothing == TrackSmoothing.PITCH_LOCKED);
        farPitchLabel.setEnabled(settings.type.hasSmoothing() && settings.smoothing == TrackSmoothing.PITCH_LOCKED);
        nearPitchInput.setEnabled(settings.type.hasSmoothing() && settings.smoothing == TrackSmoothing.PITCH_LOCKED);
        farPitchInput.setEnabled(settings.type.hasSmoothing() && settings.smoothing == TrackSmoothing.PITCH_LOCKED);
        nearPitchSettingButton.setEnabled(settings.type.hasSmoothing() && settings.smoothing == TrackSmoothing.PITCH_LOCKED);
        farPitchSettingButton.setEnabled(settings.type.hasSmoothing() && settings.smoothing == TrackSmoothing.PITCH_LOCKED);

        nearYawInput.setEnabled(settings.nearPointData.posYawType() == TrackPosYawType.ANGLE_SPECIFIED);
        farYawInput.setEnabled(settings.farPointData.posYawType() == TrackPosYawType.ANGLE_SPECIFIED);
    }

    public void onClose() {
        if (this.te != null) {
            new ItemRailUpdatePacket(te.getPos(), settings.immutable(), targetGuiOpenType).sendToServer();

            // Update client data here in order to avoid networking lag
            ItemStack clientStack = te.getItem();
            settings.immutable().write(clientStack);
            ItemTrackBlueprint.Data.writeTo(clientStack, targetGuiOpenType);
            te.setItem(clientStack, MinecraftClient.getPlayer());
        } else {
            new ItemRailUpdatePacket(settings.immutable(), targetGuiOpenType).sendToServer();

            ItemStack clientStack = MinecraftClient.getPlayer().getHeldItem(Player.Hand.PRIMARY);
            settings.immutable().write(clientStack);
            ItemTrackBlueprint.Data.writeTo(clientStack, targetGuiOpenType);
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
}
