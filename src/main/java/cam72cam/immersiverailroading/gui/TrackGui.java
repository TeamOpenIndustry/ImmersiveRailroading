package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.gui.components.ListSelector;
import cam72cam.immersiverailroading.items.ItemTrackBlueprint;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.*;
import cam72cam.immersiverailroading.net.ItemRailUpdatePacket;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.TrackDefinition;
import cam72cam.immersiverailroading.render.rail.RailRender;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.track.BuilderTransferTable;
import cam72cam.immersiverailroading.track.BuilderTurnTable;
import cam72cam.immersiverailroading.track.CubicCurve;
import cam72cam.immersiverailroading.track.TrackBase;
import cam72cam.immersiverailroading.util.*;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui.screen.*;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.opengl.RenderState;
import util.Matrix4;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static cam72cam.immersiverailroading.gui.ClickListHelper.next;
import static cam72cam.immersiverailroading.gui.components.GuiUtils.fitString;

public class TrackGui implements IScreen {
	private static final int MAX_BRANCHES = SwitchState.values().length - 2;

	long frame;

	private TileRailPreview te;
	private int targetGuiOpenType;
	private int multiSwitchBranchSelected;
	private final List<ItemStack> oreDict;
	private RailSettings.Mutable settings;
	private MultiSwitchInfo.Mutable multiSwitchInfo;
	private RailSettings.Mutable selectedWaySettings;

	// Length / Radius
	private TextField lengthInput;
	private Button lengthLabel;

	// Gauge
	private Button gaugeButton;
	private ListSelector<Gauge> gaugeSelector;

	// Track Type
	private Button typeButton;
	private ListSelector<TrackItems> typeSelector;
	private ListSelector<TrackItems> branchTypeSelector;

	// Turn Angle
	boolean unlockGuiTurnDegree;
	private Slider degreesSlider;
	private TextField degreesInput;
	private Button degreesInputTypeButton;

	private Slider curvositySlider;
	private Button smoothingButton;
	private Button directionButton;

	// Transfer Table
	private Slider transfertableEntryCountSlider;
	private Slider transfertableEntrySpacingSlider;

	// Rail Bed
	private Button bedTypeButton;
	private ListSelector<ItemStack> railBedSelector;

	// Bed Fill
	private Button bedFillButton;
	private ListSelector<ItemStack> railBedFillSelector;

	private Button bedThicknessLabel;
	private TextField bedThicknessInput;
	private Button trackFaceTransTypeButton;
	private Button trackFaceOffsetHeightLabel;
	private TextField trackFaceOffsetHeightInput;

	// Track Model
	private Button trackButton;
	private ListSelector<TrackDefinition> trackSelector;

	// Position Type
	private Button nearPosTypeButton;
	private Button farPosTypeButton;

	private CheckBox isPreviewCB;
	private CheckBox isGradeCrossingCB;

	// multiSwitch
	private Button branchIndexButton;
	private Button insertBranchButton;
	private Button removeBranchButton;

	// Other Gui
	private Button trackExtraGuiButton;
	private Button trackEndPointGuiButton;

	private Slider zoomSlider;
	private double zoom = 0.5;

	public TrackGui() {
		this(MinecraftClient.getPlayer().getHeldItem(Player.Hand.PRIMARY));
	}

	public TrackGui(TileRailPreview te) {
		this(te.getItem());
		this.te = te;
	}

	private TrackGui(ItemStack stack) {
		stack = stack.copy();
		settings = RailSettings.from(stack).mutable();
		multiSwitchInfo = MultiSwitchInfo.from(stack).mutable();
		ItemTrackBlueprint.Data data = new ItemTrackBlueprint.Data(stack);
		targetGuiOpenType = data.guiOpenType;
		unlockGuiTurnDegree = data.unlockGuiTurnDegree;
		multiSwitchBranchSelected = data.multiSwitchBranchSelected;

		if (multiSwitchBranchSelected < 0) multiSwitchBranchSelected = 0;
		if (multiSwitchBranchSelected > multiSwitchInfo.multiSwitchBranches.size()) {
			multiSwitchBranchSelected = multiSwitchInfo.multiSwitchBranches.size();
		}
		if (multiSwitchInfo.realShapeType == null) {
			multiSwitchInfo.realShapeType = settings.type == TrackItems.MULTISWITCH ? TrackItems.STRAIGHT : settings.type;
		}
		selectedWaySettings = settings;

		oreDict = new ArrayList<>();
		oreDict.add(ItemStack.EMPTY);
		oreDict.addAll(IRFuzzy.IR_RAIL_BED.enumerate());
	}

	public static String getStackName(ItemStack stack) {
		if (stack.isEmpty()) {
			return GuiText.NONE.toString();
		}
		return stack.getDisplayName();
	}

	private void updateSelectedWay(int i) {
		if (i < 0) i = 0;
		if (i > multiSwitchInfo.multiSwitchBranches.size()) i = multiSwitchInfo.multiSwitchBranches.size();
		multiSwitchBranchSelected = i;
		selectedWaySettings = i == 0 ? settings : multiSwitchInfo.multiSwitchBranches.get(i - 1).settings().mutable();
		if (branchIndexButton != null) branchIndexButton.setText(branchLabel());
	}

	private void syncMultiSwitchInfo() {
		if (multiSwitchBranchSelected <= 0) return;
		int idx = multiSwitchBranchSelected - 1;
		if (idx >= multiSwitchInfo.multiSwitchBranches.size()) return;
		RailSettings merged = selectedWaySettings.immutable().with(m -> {
			m.gauge = settings.gauge;
			m.track = settings.track;
			m.railBed = settings.railBed;
			m.railBedFill = settings.railBedFill;
			m.isPreview = settings.isPreview;
			m.isGradeCrossing = settings.isGradeCrossing;
		});
		multiSwitchInfo.multiSwitchBranches.set(idx,
				multiSwitchInfo.multiSwitchBranches.get(idx).with(m -> m.settings = merged));
	}

	private void propagateGlobalsToBranches() {
		for (int i = 0; i < multiSwitchInfo.multiSwitchBranches.size(); i++) {
			MultiSwitchBranch b = multiSwitchInfo.multiSwitchBranches.get(i);
			MultiSwitchBranch updated = b.with(m -> m.settings = m.settings.with(s -> {
				s.gauge = settings.gauge;
				s.track = settings.track;
				s.railBed = settings.railBed;
				s.railBedFill = settings.railBedFill;
				s.isPreview = settings.isPreview;
				s.isGradeCrossing = settings.isGradeCrossing;
			}));
			multiSwitchInfo.multiSwitchBranches.set(i, updated);
		}
	}

	private void applyLocal(Consumer<RailSettings.Mutable> mod) {
		if (multiSwitchBranchSelected == 0) {
			mod.accept(settings);
		} else {
			mod.accept(selectedWaySettings);
			syncMultiSwitchInfo();
		}
	}

	private void applyGlobal(Consumer<RailSettings.Mutable> mod) {
		mod.accept(settings);
	}

	private TrackItems currentType() {
		if (multiSwitchBranchSelected == 0 && settings.type == TrackItems.MULTISWITCH) {
			return multiSwitchInfo.realShapeType != null ? multiSwitchInfo.realShapeType : TrackItems.STRAIGHT;
		}
		return selectedWaySettings.type;
	}

	private void setCurrentType(TrackItems option) {
		if (multiSwitchBranchSelected == 0) {
			if (settings.type == TrackItems.MULTISWITCH) {
				multiSwitchInfo.realShapeType = option;
			} else {
				settings.type = option;
				settings.pickType = option;
			}
		} else {
			selectedWaySettings.type = option;
			selectedWaySettings.pickType = option;
			syncMultiSwitchInfo();
		}
	}

	private MultiSwitchBranch createDefaultBranch() {
		RailSettings base = settings.immutable();
		TrackItems defaultType = multiSwitchInfo.realShapeType != null && multiSwitchInfo.realShapeType.canRoll()
				? multiSwitchInfo.realShapeType
				: (base.type.canRoll() ? base.type : TrackItems.STRAIGHT);
		RailSettings branch = base.with(m -> {
			m.type = defaultType;
			m.pickType = defaultType;
			m.length = Math.max(1, Math.min(base.length, 20));
			m.degrees = 90;
			m.curvosity = 1;
			m.smoothing = TrackSmoothing.BOTH;
			m.direction = TrackDirection.NONE;
			m.trackFaceTransSetting = new TrackFaceTransSetting();
			m.transfertableEntryCount = 1;
			m.transfertableEntrySpacing = 1;
		});
		return new MultiSwitchBranch(branch, null);
	}

	private String branchLabel() {
		return GuiText.TRACK_CURRENT_SWITCH_BRANCH.toString(multiSwitchBranchSelected, multiSwitchInfo.multiSwitchBranches.size());
	}

	public void init(IScreenBuilder screen) {
		updateSelectedWay(multiSwitchBranchSelected);

		// Left pane
		int width = 200;
		int height = 20;
		int xtop = -GUIHelpers.getScreenWidth() / 2;
		int ytop = -GUIHelpers.getScreenHeight() / 4;

		this.lengthLabel = new Button(screen, xtop, ytop, width / 2 + 10, height, getLengthLabelType(currentType()));
		this.lengthLabel.setEnabled(false);

		this.lengthInput = new TextField(screen, xtop + width / 2 + 10, ytop, width / 2 - 10, height);
		this.lengthInput.setText("" + selectedWaySettings.length);
		this.lengthInput.setValidator(s -> {
			if (s == null || s.isEmpty()) return true;
			int val;
			try {
				val = Integer.parseInt(s);
			} catch (NumberFormatException e) {
				return false;
			}
			int max = 1000;
			if (multiSwitchBranchSelected == 0 && settings.type.isTable()) {
				max = settings.type == TrackItems.TURNTABLE
						? BuilderTurnTable.maxLength(settings.gauge)
						: BuilderTransferTable.maxLength(settings.gauge);
			}
			if (val > 0 && val <= max) {
				applyLocal(m -> m.length = val);
				return true;
			}
			return false;
		});
		this.lengthInput.setFocused(true);
		lengthInput.setEnabled(!currentType().isTransitionCurve());

		ytop += height;

		gaugeSelector = new ListSelector<Gauge>(screen, width, 100, height, settings.gauge,
				Gauge.values().stream().collect(Collectors.toMap(Gauge::toString, g -> g, (u, v) -> u, LinkedHashMap::new))
		) {
			@Override
			public void onClick(Gauge gauge) {
				applyGlobal(m -> m.gauge = gauge);
				gaugeButton.setText(GuiText.SELECTOR_GAUGE.toString(settings.gauge));
				if (settings.type.isTable()) {
					int max = settings.type == TrackItems.TURNTABLE
							? BuilderTurnTable.maxLength(settings.gauge)
							: BuilderTransferTable.maxLength(settings.gauge);
					lengthInput.setText("" + Math.min(Integer.parseInt(lengthInput.getText()), max));
				}
			}
		};
		gaugeButton = new Button(screen, xtop, ytop, width, height, GuiText.SELECTOR_GAUGE.toString(settings.gauge)) {
			@Override
			public void onClick(Player.Hand hand) {
				showSelector(gaugeSelector);
			}
		};
		ytop += height;

		typeSelector = new ListSelector<TrackItems>(screen, width, 100, height, settings.type,
				Arrays.stream(TrackItems.values())
						.filter(i -> i != TrackItems.CROSSING)
						.filter(i -> i != TrackItems.MULTISWITCH)
						.filter(i -> Config.ConfigBalance.EnableLegacyTrackSettingOption || i != TrackItems.TURN)
						.sorted(Comparator.comparingInt(TrackItems::getOrder))
						.collect(Collectors.toMap(TrackItems::toString, g -> g, (u, v) -> u, LinkedHashMap::new))
		) {
			@Override
			public void onClick(TrackItems option) {
				settings.type = option;
				settings.pickType = option;
				if (settings.type.isTable()) {
					int max = settings.type == TrackItems.TURNTABLE
							? BuilderTurnTable.maxLength(settings.gauge)
							: BuilderTransferTable.maxLength(settings.gauge);
					lengthInput.setText("" + Math.min(Integer.parseInt(lengthInput.getText()), max));
				}
				refreshUI();
			}
		};

		branchTypeSelector = new ListSelector<TrackItems>(screen, width, 100, height, currentType(),
				Arrays.stream(TrackItems.values())
						.filter(TrackItems::canRoll)
						.sorted(Comparator.comparingInt(TrackItems::getOrder))
						.collect(Collectors.toMap(TrackItems::toString, g -> g, (u, v) -> u, LinkedHashMap::new))
		) {
			@Override
			public void onClick(TrackItems option) {
				setCurrentType(option);
				refreshUI();
			}
		};

		typeButton = new Button(screen, xtop, ytop, width, height, GuiText.SELECTOR_TYPE.toString(currentType())) {
			@Override
			public void onClick(Player.Hand hand) {
				showSelector(settings.type == TrackItems.MULTISWITCH ? branchTypeSelector : typeSelector);
			}
		};
		ytop += height;

		//Transfer table doesn't have these property so we can have them overlapped
		smoothingButton = new Button(screen, xtop, ytop, width, height, GuiText.SELECTOR_SMOOTHING.toString(selectedWaySettings.smoothing)) {
			@Override
			public void onClick(Player.Hand hand) {
				applyLocal(m -> {
					TrackSmoothing nextVal = next(m.smoothing, hand);
					if (nextVal == TrackSmoothing.NEITHER && !Config.ConfigBalance.EnableLegacyTrackSettingOption) {
						nextVal = next(nextVal, hand);
					}
					m.smoothing = nextVal;
				});
				smoothingButton.setText(GuiText.SELECTOR_SMOOTHING.toString(selectedWaySettings.smoothing));
			}
		};
		smoothingButton.setVisible(currentType().hasSmoothing());

		transfertableEntryCountSlider = new Slider(screen, 25 + xtop, ytop, "", 1, 71, settings.transfertableEntryCount, false) {
			@Override
			public void onSlider() {
				settings.transfertableEntryCount = (int) this.getValue();
				transfertableEntryCountSlider.setText(
						GuiText.SELECTOR_TRANSFER_TABLE_ENTRY_COUNT.toString((int) transfertableEntryCountSlider.getValue()));
			}
		};
		transfertableEntryCountSlider.onSlider();
		ytop += height;

		directionButton = new Button(screen, xtop, ytop, width, height, GuiText.SELECTOR_DIRECTION.toString(selectedWaySettings.direction)) {
			@Override
			public void onClick(Player.Hand hand) {
				applyLocal(m -> m.direction = next(m.direction, hand));
				directionButton.setText(GuiText.SELECTOR_DIRECTION.toString(selectedWaySettings.direction));
			}
		};
		directionButton.setVisible(currentType().hasDirection());

		transfertableEntrySpacingSlider = new Slider(screen, 25 + xtop, ytop, "", 1, 15, settings.transfertableEntrySpacing, false) {
			@Override
			public void onSlider() {
				settings.transfertableEntrySpacing = (int) this.getValue();
				transfertableEntrySpacingSlider.setText(
						GuiText.SELECTOR_TRANSFER_TABLE_ENTRY_SPACING.toString((int) transfertableEntrySpacingSlider.getValue()));
			}
		};
		transfertableEntrySpacingSlider.onSlider();
		ytop += height;

		degreesInput = new TextField(screen, 25 + xtop, ytop, width - 50, height);
		degreesInput.setText("" + selectedWaySettings.degrees);
		degreesInput.setValidator(s -> {
			if (s == null || s.isEmpty()) return true;
			float val;
			try {
				val = Float.parseFloat(s);
			} catch (NumberFormatException e) {
				return false;
			}
			float max = 90f;
			float min = 1f;
			if (val >= min && val <= max) {
				if (currentType().isTransitionCurve()
						&& !CubicCurve.isCubicParabolaInputValid(selectedWaySettings.nearPointData.radius(), selectedWaySettings.farPointData.radius(), val)) {
					return false;
				}
				applyLocal(m -> m.degrees = val);
				degreesSlider.setValue(val / (90F / Config.ConfigBalance.AnglePlacementSegmentation));
				return true;
			}
			return false;
		});
		degreesInput.setFocused(true);

		this.degreesSlider = new Slider(screen, 25 + xtop, ytop, "", 1, Config.ConfigBalance.AnglePlacementSegmentation, selectedWaySettings.degrees / 90 * Config.ConfigBalance.AnglePlacementSegmentation, false) {
			@Override
			public void onSlider() {
				if (unlockGuiTurnDegree) {
					float degrees = selectedWaySettings.degrees;
					while (currentType().isTransitionCurve()
							&& !CubicCurve.isCubicParabolaInputValid(selectedWaySettings.nearPointData.radius(), selectedWaySettings.farPointData.radius(), degrees)) {
						degrees--;
						if (Math.abs(degrees) < 1e-6) {
							degrees = 0;
							break;
						}
					}
					final float finalDegrees = degrees;
					applyLocal(m -> m.degrees = finalDegrees);
					degreesInput.setText("" + selectedWaySettings.degrees);
					degreesSlider.setText(GuiText.SELECTOR_QUARTERS.toString(this.getValueInt() * (90.0 / Config.ConfigBalance.AnglePlacementSegmentation)));
					return;
				}
				float val = this.getValueInt() * (90F / Config.ConfigBalance.AnglePlacementSegmentation);
				if (currentType().isTransitionCurve()) {
					boolean shouldReset = false;
					while (!CubicCurve.isCubicParabolaInputValid(selectedWaySettings.nearPointData.radius(), selectedWaySettings.farPointData.radius(), val)) {
						shouldReset = true;
						val -= 90F / Config.ConfigBalance.AnglePlacementSegmentation;
						if (Math.abs(val) < 1e-6) break;
					}
					if (shouldReset) this.setValue(val / (90F / Config.ConfigBalance.AnglePlacementSegmentation));
				}
				final float finalVal = val;
				applyLocal(m -> m.degrees = finalVal);
				degreesInput.setText("" + selectedWaySettings.degrees);
				degreesSlider.setText(GuiText.SELECTOR_QUARTERS.toString(this.getValueInt() * (90.0 / Config.ConfigBalance.AnglePlacementSegmentation)));
			}
		};
		degreesSlider.onSlider();

		degreesInputTypeButton = new Button(screen, xtop + width - 20, ytop, height, height, "↔") {
			@Override
			public void onClick(Player.Hand hand) {
				unlockGuiTurnDegree = !unlockGuiTurnDegree;
				degreesInput.setVisible(unlockGuiTurnDegree && currentType().hasQuarters());
				degreesSlider.setVisible(!unlockGuiTurnDegree && currentType().hasQuarters());
				if (!unlockGuiTurnDegree) degreesSlider.onSlider();
			}
		};

		ytop += height;

		this.curvositySlider = new Slider(screen, 25 + xtop, ytop, "", 0.25, 1.5, selectedWaySettings.curvosity, true) {
			@Override
			public void onSlider() {
				float val = (float) this.getValue();
				applyLocal(m -> m.curvosity = val);
				curvositySlider.setText(GuiText.SELECTOR_CURVOSITY.toString(String.format("%.2f", selectedWaySettings.curvosity)));
			}
		};
		curvositySlider.onSlider();
		ytop += height;

		directionButton.setVisible(currentType().hasDirection());

		degreesSlider.setVisible(currentType().hasQuarters() && !unlockGuiTurnDegree);
		degreesInput.setVisible(currentType().hasQuarters() && unlockGuiTurnDegree);
		degreesInputTypeButton.setVisible(currentType().hasQuarters());

		curvositySlider.setVisible(currentType().hasCurvosity());
		smoothingButton.setVisible(currentType().hasSmoothing());

		transfertableEntryCountSlider.setVisible(multiSwitchBranchSelected == 0 && settings.type == TrackItems.TRANSFERTABLE);
		transfertableEntrySpacingSlider.setVisible(multiSwitchBranchSelected == 0 && settings.type == TrackItems.TRANSFERTABLE);

		// Bottom Pane
		ytop = (int) (GUIHelpers.getScreenHeight() * 0.75 - height * 5);

		trackSelector = new ListSelector<TrackDefinition>(screen, width, 250, height,
				DefinitionManager.getTrack(settings.track),
				DefinitionManager.getTracks().stream().collect(Collectors.toMap(t -> t.name, g -> g, (u, v) -> u, LinkedHashMap::new))) {
			@Override
			public void onClick(TrackDefinition track) {
				applyGlobal(m -> m.track = track.trackID);
				trackButton.setText(GuiText.SELECTOR_TRACK.toString(fitString(DefinitionManager.getTrack(settings.track).name, 24)));
			}
		};
		trackButton = new Button(screen, xtop, ytop, width, height, GuiText.SELECTOR_TRACK.toString(fitString(DefinitionManager.getTrack(settings.track).name, 24))) {
			@Override
			public void onClick(Player.Hand hand) {
				showSelector(trackSelector);
			}
		};
		ytop += height;

		railBedSelector = new ListSelector<ItemStack>(screen, width, 250, height, settings.railBed,
				oreDict.stream().collect(Collectors.toMap(TrackGui::getStackName, g -> g, (u, v) -> u, LinkedHashMap::new))
		) {
			@Override
			public void onClick(ItemStack option) {
				applyGlobal(m -> m.railBed = option);
				bedTypeButton.setText(GuiText.SELECTOR_RAIL_BED.toString(getStackName(settings.railBed)));
			}
		};
		bedTypeButton = new Button(screen, xtop, ytop, width, height, GuiText.SELECTOR_RAIL_BED.toString(getStackName(settings.railBed))) {
			@Override
			public void onClick(Player.Hand hand) {
				showSelector(railBedSelector);
			}
		};
		ytop += height;

		railBedFillSelector = new ListSelector<ItemStack>(screen, width, 250, height, settings.railBedFill,
				oreDict.stream().collect(Collectors.toMap(TrackGui::getStackName, g -> g, (u, v) -> u, LinkedHashMap::new))
		) {
			@Override
			public void onClick(ItemStack option) {
				applyGlobal(m -> m.railBedFill = option);
				bedFillButton.setText(GuiText.SELECTOR_RAIL_BED_FILL.toString(getStackName(settings.railBedFill)));
			}
		};
		bedFillButton = new Button(screen, xtop, ytop, width, height, GuiText.SELECTOR_RAIL_BED_FILL.toString(getStackName(settings.railBedFill))) {
			@Override
			public void onClick(Player.Hand hand) {
				showSelector(railBedFillSelector);
			}
		};
		ytop += height;

		nearPosTypeButton = new Button(screen, xtop, ytop, width / 2, height, GuiText.SELECTOR_POSITION_NEAR.toString(selectedWaySettings.nearPointData.posType())) {
			@Override
			public void onClick(Player.Hand hand) {
				applyLocal(m -> m.nearPointData = m.nearPointData.with(n -> n.posType = next(n.posType, hand)));
				nearPosTypeButton.setText(GuiText.SELECTOR_POSITION_NEAR.toString(selectedWaySettings.nearPointData.posType()));
			}
		};
		farPosTypeButton = new Button(screen, xtop + width / 2, ytop, width / 2, height, GuiText.SELECTOR_POSITION_FAR.toString(selectedWaySettings.farPointData.posType())) {
			@Override
			public void onClick(Player.Hand hand) {
				applyLocal(m -> m.farPointData = m.farPointData.with(n -> n.posType = next(n.posType, hand)));
				farPosTypeButton.setText(GuiText.SELECTOR_POSITION_FAR.toString(selectedWaySettings.farPointData.posType()));
			}
		};
		ytop += height;

		isPreviewCB = new CheckBox(screen, xtop + 2, ytop + 2, GuiText.SELECTOR_PLACE_BLUEPRINT.toString(), settings.isPreview) {
			@Override
			public void onClick(Player.Hand hand) {
				applyGlobal(m -> m.isPreview = isPreviewCB.isChecked());
			}
		};

		isGradeCrossingCB = new CheckBox(screen, xtop + 102, ytop + 2, GuiText.SELECTOR_GRADE_CROSSING.toString(), settings.isGradeCrossing) {
			@Override
			public void onClick(Player.Hand hand) {
				applyGlobal(m -> m.isGradeCrossing = isGradeCrossingCB.isChecked());
			}
		};
		ytop += height;

		trackEndPointGuiButton = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width / 2, -GUIHelpers.getScreenHeight() / 4, width / 2, height, GuiText.TRACK_MAIN_TO_END_POINT.toString()) {
			@Override
			public void onClick(Player.Hand hand) {
				targetGuiOpenType = 2;
				onClose();
				if (te != null) {
					GuiTypes.RAIL_PREVIEW.open(MinecraftClient.getPlayer(), te.getPos());
				} else {
					GuiTypes.RAIL.open(MinecraftClient.getPlayer());
				}
			}
		};

		trackExtraGuiButton = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width / 2, -GUIHelpers.getScreenHeight() / 4 + height, width / 2, height, GuiText.TRACK_MAIN_TO_EXTRA.toString()) {
			@Override
			public void onClick(Player.Hand hand) {
				targetGuiOpenType = 1;
				onClose();
				if (te != null) {
					GuiTypes.RAIL_PREVIEW.open(MinecraftClient.getPlayer(), te.getPos());
				} else {
					GuiTypes.RAIL.open(MinecraftClient.getPlayer());
				}
			}
		};

		trackFaceTransTypeButton = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width / 2 - width / 4, -GUIHelpers.getScreenHeight() / 4 + height * 2, width / 2 + width / 4, height, GuiText.SELECTOR_TRACK_PIVOT_TYPE.toString(selectedWaySettings.trackFaceTransSetting.facePivotType())) {
			@Override
			public void onClick(Player.Hand hand) {
				applyLocal(m -> m.trackFaceTransSetting = m.trackFaceTransSetting.with(t -> t.facePivotType = next(t.facePivotType, hand)));
				this.setText(GuiText.SELECTOR_TRACK_PIVOT_TYPE.toString(selectedWaySettings.trackFaceTransSetting.facePivotType()));
			}
		};

		trackFaceOffsetHeightLabel = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width / 2 - width / 4, -GUIHelpers.getScreenHeight() / 4 + height * 3, width / 2, height, GuiText.LABEL_TRACK_PIVOT_OFFSET.toString()) {};
		trackFaceOffsetHeightLabel.setEnabled(false);
		trackFaceOffsetHeightInput = new TextField(screen, GUIHelpers.getScreenWidth() / 2 - width / 2 + width / 4, -GUIHelpers.getScreenHeight() / 4 + height * 3, width / 4, height);
		trackFaceOffsetHeightInput.setText("" + (float) selectedWaySettings.trackFaceTransSetting.facePivotOffset().y);
		trackFaceOffsetHeightInput.setValidator(s -> {
			if (s == null || s.isEmpty()) return true;
			float val;
			try {
				val = Float.parseFloat(s);
			} catch (NumberFormatException e) {
				return false;
			}
			if (val >= 0f && val <= 1f) {
				applyLocal(m -> m.trackFaceTransSetting = m.trackFaceTransSetting.with(t -> t.facePivotOffset = new Vec3d(0, val, 0)));
				return true;
			}
			return false;
		});
		trackFaceOffsetHeightInput.setFocused(true);

		bedThicknessLabel = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width / 2 - width / 4, -GUIHelpers.getScreenHeight() / 4 + height * 4, width / 2, height, GuiText.LABEL_BED_THICKNESS.toString()) {};
		bedThicknessLabel.setEnabled(false);
		bedThicknessInput = new TextField(screen, GUIHelpers.getScreenWidth() / 2 - width / 2 + width / 4, -GUIHelpers.getScreenHeight() / 4 + height * 4, width / 4, height);
		bedThicknessInput.setText("" + (float) selectedWaySettings.trackFaceTransSetting.bedThickness());
		bedThicknessInput.setValidator(s -> {
			if (s == null || s.isEmpty()) return true;
			float val;
			try {
				val = Float.parseFloat(s);
			} catch (NumberFormatException e) {
				return false;
			}
			if (val >= 0f && val <= 1f) {
				applyLocal(m -> m.trackFaceTransSetting = m.trackFaceTransSetting.with(t -> t.bedThickness = val));
				return true;
			}
			return false;
		});
		bedThicknessInput.setFocused(true);

		// multiSwitch
		branchIndexButton = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width / 2 - width / 4, -GUIHelpers.getScreenHeight() / 4 + height * 5, width / 4, height, branchLabel()) {
			@Override
			public void onClick(Player.Hand hand) {
				syncMultiSwitchInfo();
				int total = multiSwitchInfo.multiSwitchBranches.size() + 1;
				int step = (hand == Player.Hand.PRIMARY) ? 1 : -1;
				updateSelectedWay((multiSwitchBranchSelected + step + total) % total);
				refreshUI();
			}
		};
		insertBranchButton = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width / 2, -GUIHelpers.getScreenHeight() / 4 + height * 5, width / 4, height, GuiText.TRACK_INSERT_SWITCH_BRANCH.toString()) {
			@Override
			public void onClick(Player.Hand hand) {
				syncMultiSwitchInfo();
				if (multiSwitchInfo.multiSwitchBranches.size() >= MAX_BRANCHES) return;

				if (multiSwitchInfo.multiSwitchBranches.isEmpty() && settings.type != TrackItems.MULTISWITCH) {
					if (!settings.type.canRoll()) return;
					multiSwitchInfo.realShapeType = settings.type;
					settings.type = TrackItems.MULTISWITCH;
					settings.pickType = TrackItems.MULTISWITCH;
				}

				int insertIdx = multiSwitchBranchSelected;
				multiSwitchInfo.multiSwitchBranches.add(insertIdx, createDefaultBranch());
				updateSelectedWay(insertIdx + 1);
				refreshUI();
			}
		};
		removeBranchButton = new Button(screen, GUIHelpers.getScreenWidth() / 2 - width / 4, -GUIHelpers.getScreenHeight() / 4 + height * 5, width / 4, height,  GuiText.TRACK_REMOVE_SWITCH_BRANCH.toString()) {
			@Override
			public void onClick(Player.Hand hand) {
				if (multiSwitchBranchSelected == 0) return;
				if (multiSwitchInfo.multiSwitchBranches.isEmpty()) return;
				int idx = multiSwitchBranchSelected - 1;
				if (idx >= multiSwitchInfo.multiSwitchBranches.size()) return;
				multiSwitchInfo.multiSwitchBranches.remove(idx);

				if (multiSwitchInfo.multiSwitchBranches.isEmpty()) {
					TrackItems restore = multiSwitchInfo.realShapeType != null ? multiSwitchInfo.realShapeType : TrackItems.STRAIGHT;
					settings.type = restore;
					settings.pickType = restore;
					multiSwitchInfo.realShapeType = null;
				}

				updateSelectedWay(multiSwitchBranchSelected);
				refreshUI();
			}
		};

		zoomSlider = new Slider(screen, GUIHelpers.getScreenWidth() / 2 - 150, (int) (GUIHelpers.getScreenHeight() * 0.75 - height),
				GuiText.SLIDER_ZOOM.toString(), 0.1, 2, zoom, true) {
			@Override
			public void onSlider() {
				zoom = this.getValue();
			}
		};
	}

	private void refreshUI() {
		TrackItems type = currentType();

		lengthInput.setText("" + selectedWaySettings.length);
		lengthLabel.setText(getLengthLabelType(type));
		lengthInput.setEnabled(!type.isTransitionCurve());

		typeButton.setText(GuiText.SELECTOR_TYPE.toString(type));

		smoothingButton.setText(GuiText.SELECTOR_SMOOTHING.toString(selectedWaySettings.smoothing));
		smoothingButton.setVisible(type.hasSmoothing());

		directionButton.setText(GuiText.SELECTOR_DIRECTION.toString(selectedWaySettings.direction));
		directionButton.setVisible(type.hasDirection());

		curvositySlider.setValue(selectedWaySettings.curvosity);
		curvositySlider.setText(GuiText.SELECTOR_CURVOSITY.toString(String.format("%.2f", selectedWaySettings.curvosity)));
		curvositySlider.setVisible(type.hasCurvosity());

		degreesSlider.setValue(selectedWaySettings.degrees / 90.0 * Config.ConfigBalance.AnglePlacementSegmentation);
		degreesSlider.setText(GuiText.SELECTOR_QUARTERS.toString(selectedWaySettings.degrees));
		degreesSlider.setVisible(type.hasQuarters() && !unlockGuiTurnDegree);

		degreesInput.setText("" + selectedWaySettings.degrees);
		degreesInput.setVisible(type.hasQuarters() && unlockGuiTurnDegree);
		degreesInputTypeButton.setVisible(type.hasQuarters());

		nearPosTypeButton.setText(GuiText.SELECTOR_POSITION_NEAR.toString(selectedWaySettings.nearPointData.posType()));
		farPosTypeButton.setText(GuiText.SELECTOR_POSITION_FAR.toString(selectedWaySettings.farPointData.posType()));

		trackFaceTransTypeButton.setText(GuiText.SELECTOR_TRACK_PIVOT_TYPE.toString(selectedWaySettings.trackFaceTransSetting.facePivotType()));
		trackFaceOffsetHeightInput.setText("" + (float) selectedWaySettings.trackFaceTransSetting.facePivotOffset().y);
		bedThicknessInput.setText("" + (float) selectedWaySettings.trackFaceTransSetting.bedThickness());

		boolean isTransferMain = multiSwitchBranchSelected == 0 && settings.type == TrackItems.TRANSFERTABLE;
		transfertableEntryCountSlider.setVisible(isTransferMain);
		transfertableEntrySpacingSlider.setVisible(isTransferMain);

		branchIndexButton.setText(branchLabel());
	}

	private void showSelector(ListSelector<?> selector) {
		boolean isVisible = selector.isVisible();

		gaugeSelector.setVisible(false);
		typeSelector.setVisible(false);
		branchTypeSelector.setVisible(false);
		trackSelector.setVisible(false);
		railBedSelector.setVisible(false);
		railBedFillSelector.setVisible(false);
		branchIndexButton.setVisible(false);
		insertBranchButton.setVisible(false);
		removeBranchButton.setVisible(false);

		trackEndPointGuiButton.setVisible(isVisible);
		trackExtraGuiButton.setVisible(isVisible);

		trackFaceTransTypeButton.setVisible(isVisible);
		trackFaceOffsetHeightLabel.setVisible(isVisible);
		trackFaceOffsetHeightInput.setVisible(isVisible);
		bedThicknessLabel.setVisible(isVisible);
		bedThicknessInput.setVisible(isVisible);
		branchIndexButton.setVisible(isVisible);
		insertBranchButton.setVisible(isVisible);
		removeBranchButton.setVisible(isVisible);

		zoomSlider.setVisible(isVisible);

		selector.setVisible(!isVisible);
	}

	@Override
	public void onEnterKey(IScreenBuilder builder) {
		builder.close();
	}

	@Override
	public void onClose() {
		syncMultiSwitchInfo();
		propagateGlobalsToBranches();

		if (!this.lengthInput.getText().isEmpty()) {
			if (this.te != null) {
				new ItemRailUpdatePacket(te.getPos(), settings.immutable(), multiSwitchInfo.immutable(), targetGuiOpenType, unlockGuiTurnDegree, multiSwitchBranchSelected).sendToServer();

				// Update client data here in order to avoid networking lag
				ItemStack clientStack = te.getItem();
				settings.immutable().write(clientStack);
				multiSwitchInfo.immutable().write(clientStack);
				ItemTrackBlueprint.Data.writeTo(clientStack, targetGuiOpenType, unlockGuiTurnDegree, multiSwitchBranchSelected);
				te.setItem(clientStack, MinecraftClient.getPlayer());
			} else {
				new ItemRailUpdatePacket(settings.immutable(), multiSwitchInfo.immutable(), targetGuiOpenType, unlockGuiTurnDegree, multiSwitchBranchSelected).sendToServer();

				ItemStack clientStack = MinecraftClient.getPlayer().getHeldItem(Player.Hand.PRIMARY);
				settings.immutable().write(clientStack);
				multiSwitchInfo.immutable().write(clientStack);
				ItemTrackBlueprint.Data.writeTo(clientStack, targetGuiOpenType, unlockGuiTurnDegree, multiSwitchBranchSelected);
				MinecraftClient.getPlayer().setHeldItem(Player.Hand.PRIMARY, clientStack);
			}
		}
	}

	@Override
	public void draw(IScreenBuilder builder, RenderState state) {
		frame++;

		GUIHelpers.drawRect(200, 0, GUIHelpers.getScreenWidth() - 200, GUIHelpers.getScreenHeight(), 0xCC000000);
		GUIHelpers.drawRect(0, 0, 200, GUIHelpers.getScreenHeight(), 0xEE000000);

		if (gaugeSelector.isVisible()) {
			double textScale = 1.5;
			GUIHelpers.drawCenteredString(GuiText.SELECTOR_GAUGE.toString(settings.gauge.toString()), (int) ((300 + (GUIHelpers.getScreenWidth()-300) / 2) / textScale), (int) (10 / textScale), 0xFFFFFF, new Matrix4().scale(textScale, textScale, textScale));

			RailInfo info = new RailInfo(
					settings.immutable().with(rendered -> {
						rendered.length = 5;
						rendered.type = TrackItems.STRAIGHT;
					}),
					new PlacementInfo(new Vec3d(0.5, 0, 0.5), TrackDirection.NONE, 0, null),
					null, SwitchState.NONE, SwitchState.NONE, 0, true, null);

			double scale = GUIHelpers.getScreenWidth() / 12.0 * zoom;

			state.translate(300 + (GUIHelpers.getScreenWidth() - 300) / 2, builder.getHeight(), 100);
			state.rotate(90, 1, 0, 0);
			state.scale(-scale, scale, scale);
			state.translate(0, 0, 1);
			RailRender.get(info).renderRailModel(state);
			state.translate(-0.5, 0, -0.5);
			RailRender.get(info).renderRailBase(state);
			return;
		}

		if (trackSelector.isVisible() || railBedSelector.isVisible() || railBedFillSelector.isVisible()) {
			ListSelector.ButtonRenderer<ItemStack> icons = (button, x, y, value) -> {
				Matrix4 zMatrix = new Matrix4();
				zMatrix.translate(0, 0, 100);

				GUIHelpers.drawItem(value, x+2, y+2, zMatrix);
			};

			railBedSelector.render(icons);
			railBedFillSelector.render(icons);


			double textScale = 1.5;
			String str = trackSelector.isVisible() ? GuiText.SELECTOR_TRACK.toString(DefinitionManager.getTrack(settings.track).name) :
					railBedSelector.isVisible() ? GuiText.SELECTOR_RAIL_BED.toString(getStackName(settings.railBed)) :
							GuiText.SELECTOR_RAIL_BED_FILL.toString(getStackName(settings.railBedFill));

			GUIHelpers.drawCenteredString(str, (int) ((450 + (GUIHelpers.getScreenWidth()-450) / 2) / textScale), (int) (10 / textScale), 0xFFFFFF, new Matrix4().scale(textScale, textScale, textScale));

			RailInfo info = new RailInfo(
					settings.immutable().with(rendered -> {
						rendered.length = 3;
						rendered.type = TrackItems.STRAIGHT;
					}),
					new PlacementInfo(new Vec3d(0.5, 0, 0.5), TrackDirection.NONE, 0, null),
					null, SwitchState.NONE, SwitchState.NONE, 0, true, null);

			double scale = GUIHelpers.getScreenWidth() / 15.0 * zoom;

			state.translate(450 + (GUIHelpers.getScreenWidth() - 450) / 2, builder.getHeight()/2, 500);
			state.rotate(90, 1, 0, 0);
			state.scale(-scale, scale, scale);
			state.translate(0, 0, -1);
			//state.rotate(60, 1, -1, -0.6);
			state.rotate(60, 1, 0, 0);

			state.translate(0, 0, 1);
			state.rotate(frame/2.0, 0, 1, 0);
			state.translate(0, 0, -1);

			RailRender.get(info).renderRailModel(state);
			state.translate(-0.5, 0, -0.5);
			RailRender.get(info).renderRailBase(state);

			if (!info.settings.railBedFill.isEmpty()) {
				StandardModel model = new StandardModel();
				for (TrackBase base : info.getBuilder(MinecraftClient.getPlayer().getWorld()).getTracksForRender()) {
					Vec3i basePos = base.getPos();
					model.addItemBlock(info.settings.railBedFill, new Matrix4()
							.translate(basePos.x, basePos.y-1, basePos.z)
					);
				}
				model.render(state);
			}

			return;
		}

		if (lengthInput.getText().isEmpty()) {
			return;
		}

		// This could be more efficient...
		double tablePos = settings.type == TrackItems.TURNTABLE
						  ? (frame / 2.0) % 360
						  : settings.type == TrackItems.TRANSFERTABLE
							? (frame / 50.0) % (settings.transfertableEntrySpacing * (settings.transfertableEntryCount - 1))
							: 0;
		RailInfo info = new RailInfo(
				settings.immutable().with(b -> {
					int length = b.length;
					if (length < 5) {
						length = 5;
					}
					if (settings.type == TrackItems.TURNTABLE) {
						length = MathUtil.clamp(length, 10, 25);
					}
					b.length = length;
				}),
				new PlacementInfo(new Vec3d(0.5, 0, 0.5), settings.direction, 0, null),
				null, SwitchState.NONE, SwitchState.NONE, tablePos, true, multiSwitchInfo.immutable());

		int length = (int) info.settings.getValidSize();
		double scale = (GUIHelpers.getScreenWidth() / (length * 2.25)) * zoom;
		if (settings.type.isTable()) {
			scale /= 2;
		}

		state.translate(200 + (GUIHelpers.getScreenWidth() - 200) / 2, builder.getHeight() - 30, 100);
		state.rotate(90, 1, 0, 0);
		state.scale(-scale, scale, scale);
		state.translate(0, 0, 1);
		if (settings.type.hasDirection()) {
			switch (settings.direction) {
				case LEFT:
					state.translate(length / 2.0, 0, 0);
					break;
				case NONE:
				case RIGHT:
					state.translate(-length / 2.0, 0, 0);
					break;
			}
		}
		if (settings.type == TrackItems.CUSTOM) {
			state.translate(-length / 2.0, 0, 0);
		}
		RailRender.get(info).renderRailModel(state);
		state.translate(-0.5, 0, -0.5);
		RailRender.get(info).renderRailBase(state);
		if (!info.settings.railBedFill.isEmpty()) {
			StandardModel model = new StandardModel();
			for (TrackBase base : info.getBuilder(MinecraftClient.getPlayer().getWorld()).getTracksForRender()) {
				Vec3i basePos = base.getPos();
				model.addItemBlock(info.settings.railBedFill, new Matrix4()
						.translate(basePos.x, basePos.y-1, basePos.z)
				);
			}
			model.render(state);
		}
	}

	private static String getLengthLabelType(TrackItems type) {
		switch (type) {
			case STRAIGHT:
			case SLOPE:
			case TRANSFERTABLE:
			case CUSTOM:
				return GuiText.LABEL_LENGTH.toString();
			case TURN:
			case TURN_V2:
			case TURNTABLE:
				return GuiText.LABEL_RADIUS.toString();
			case SWITCH:
				return GuiText.LABEL_LENGTH_RADIUS.toString();
			case CUBICPARABOLA:
			default:
				return GuiText.LABEL_NO_LENGTH.toString();
		}
	}
}