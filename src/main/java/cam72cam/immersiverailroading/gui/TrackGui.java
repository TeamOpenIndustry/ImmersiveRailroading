package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.gui.components.ListSelector;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.*;
import cam72cam.immersiverailroading.net.ItemRailUpdatePacket;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.TrackDefinition;
import cam72cam.immersiverailroading.render.rail.RailRender;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.track.*;
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
	long frame;
	private TileRailPreview te;
	private Button typeButton;
	private TextField lengthInput;
    //TODO How do we handle dynamic range?
//	private NumberInputer lengthInput;
	private Slider degreesSlider;
	private Slider curvositySlider;
	private CheckBox isPreviewCB;
	private CheckBox isGradeCrossingCB;
	private Button gaugeButton;
	private Button trackButton;
	private Button posTypeButton;
	private Button smoothingButton;
	private Button directionButton;
	private Button bedTypeButton;
	private Button bedFillButton;

	private Slider transfertableEntryCountSlider;
	private Slider transfertableEntrySpacingSlider;

	private final List<ItemStack> oreDict;

	private RailSettings.Mutable settings;
	private ListSelector<Gauge> gaugeSelector;
	private ListSelector<TrackItems> typeSelector;
	private ListSelector<TrackDefinition> trackSelector;
	private ListSelector<ItemStack> railBedSelector;
	private ListSelector<ItemStack> railBedFillSelector;

	//multiWaySwitch
	private ListSelector<TrackItems> subTypeSelector;
	private MultiSwitchInfo.Mutable multiSwitchInfo;
	private Button wayCircleButton;
	private int selectedWay = 0;//0=default,1=MID1,2=MID2,3=MID3,4=MID4,5=TURN
	private RailSettings.Mutable selectedWaySettings;
	private Button insertWayButton;
	private Button addWayButton;
	private Button delWayButton;
	//spiralCurve
	// curves based on cubic-curve are limited in some case, we may need new types for more complex curve and function,
	// like completely decoupling vertical and horizontal curve, more spiralCurve and real arc turn(this will looks better with big radius)?
	private TextField farRadiusInput;
	private Button toggleStraightAtP1;

	//micro judge
	//TODO: for all types, be able to judge y offset of placement and custom
	private Slider nearHeightOffsetSlider;
	private Slider farHeightOffsetSlider;

	//vertical smooth config
	//TODO: change to textFiled?
	private Slider nearPitchSlider;
	private Slider farPitchSlider;

	//zoom
	private double zoom = 1;

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

	public void init(IScreenBuilder screen) {

		// Left pane
		int width = 200;
		int height = 20;
		int xtop = -GUIHelpers.getScreenWidth() / 2;
		int ytop = -GUIHelpers.getScreenHeight() / 4;
		updateSelectedWay(0);

        this.lengthInput = new TextField(screen, xtop, ytop, width-1, height);
        this.lengthInput.setText("" + (selectedWay==0 ? settings.length : selectedWaySettings.length));
        this.lengthInput.setValidator(s -> {
            if (s == null || s.length() == 0) {
                return true;
            }
            int val;
            try {
                val = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return false;
            }
            int max = 1000;
            if (settings.type.isTable()) {
                max = settings.type == TrackItems.TURNTABLE
                      ? BuilderTurnTable.maxLength(settings.gauge)
                      : BuilderTransferTable.maxLength(settings.gauge);
            }
            if (val > 0 && val <= max) {
				if(settings.type == TrackItems.CUBICPARABOLA && !CubicCurve.isCubicParabolaDeltaValid(settings.length,settings.degrees,val))return false;
				if(selectedWay==0){
					settings.length = val;
				}else{
					selectedWaySettings.length = val;
					syncMultiSwitchInfo();
				}

                return true;
            }
            return false;
        });
        this.lengthInput.setFocused(true);
//		this.lengthInput = new NumberInputer(screen, xtop, ytop, width, height, "Length:", "", 10, 1000, settings.length,
//                                             true, true, val -> {
//            int max = 1000;
//            if (settings.type.isTable()) {
//                max = settings.type == TrackItems.TURNTABLE
//                      ? BuilderTurnTable.maxLength(settings.gauge)
//                      : BuilderTransferTable.maxLength(settings.gauge);
//            }
//            if (val > 0 && val <= max) {
//                settings.length = val.intValue();
//            }
//        });
		ytop += height;

		gaugeSelector = new ListSelector<Gauge>(screen, width, 100, height, settings.gauge,
				Gauge.values().stream().collect(Collectors.toMap(Gauge::toString, g -> g, (u, v) -> u, LinkedHashMap::new))
		) {
			@Override
			public void onClick(Gauge gauge) {
				settings.gauge = gauge;
				updateListSetting(mutable -> mutable.gauge = gauge);

				gaugeButton.setText(GuiText.SELECTOR_GAUGE.toString(settings.gauge));
				if (settings.type.isTable()) {
					int max = settings.type == TrackItems.TURNTABLE
							  ? BuilderTurnTable.maxLength(settings.gauge)
							  : BuilderTransferTable.maxLength(settings.gauge);

                    lengthInput.setText("" + Math.min(Integer.parseInt(lengthInput.getText()), max)); // revalidate
//					lengthInput.setValue(Math.min((int)lengthInput.getValue(), max)); // revalidate
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
						.sorted(Comparator.comparingInt(TrackItems::getOrder))
						.collect(Collectors.toMap(TrackItems::toString, g -> g, (u, v) -> u, LinkedHashMap::new))
		) {
			@Override
			public void onClick(TrackItems option) {
				settings.type = option;

				updateSelectedWay(0);

				if(settings.type == TrackItems.CUBICPARABOLA && settings.degrees > CubicCurve.cubicParabolaMaxAngle) {
					settings.degrees = (float) CubicCurve.cubicParabolaMaxAngle;
					degreesSlider.setValue(settings.degrees * Config.ConfigBalance.AnglePlacementSegmentation / 90);
				}else {
					degreesSlider.setValue(settings.degrees * Config.ConfigBalance.AnglePlacementSegmentation / 90);
				}
				degreesSlider.setText(GuiText.SELECTOR_QUARTERS.toString(degreesSlider.getValueInt() * (90.0/Config.ConfigBalance.AnglePlacementSegmentation)));

				nearPitchSlider.setValue(settings.pitchTag.getFloat("start"));
				nearPitchSlider.setText("near pitch:"+String.format("%.2f", nearPitchSlider.getValue()));

				farPitchSlider.setValue(settings.pitchTag.getFloat("end"));
				farPitchSlider.setText("far pitch:"+String.format("%.2f", farPitchSlider.getValue()));

				lengthInput.setText(""+settings.length);
				farRadiusInput.setText(""+settings.cubicParabolaTag.getInteger("farRadius"));

				typeButton.setText(GuiText.SELECTOR_TYPE.toString(settings.type));
				degreesSlider.setVisible(settings.type.hasQuarters());
				curvositySlider.setVisible(settings.type.hasCurvosity());
				smoothingButton.setVisible(settings.type.hasSmoothing());
				directionButton.setVisible(settings.type.hasDirection());
				farRadiusInput.setVisible(settings.type.hasFarRadius());

				insertWayButton.setVisible(settings.type.isMulti());
				addWayButton.setVisible(settings.type.isMulti());
				delWayButton.setVisible(settings.type.isMulti());
				wayCircleButton.setVisible(settings.type.isMulti());

				if (settings.type.isTable()) {
					int max = settings.type == TrackItems.TURNTABLE
							  ? BuilderTurnTable.maxLength(settings.gauge)
							  : BuilderTransferTable.maxLength(settings.gauge);
                    lengthInput.setText("" + Math.min(Integer.parseInt(lengthInput.getText()), max)); // revalidate
//					lengthInput.setValue(Math.min((int) lengthInput.getValue(), max)); // revalidate
				}
				transfertableEntryCountSlider.setVisible(settings.type == TrackItems.TRANSFERTABLE);
				transfertableEntrySpacingSlider.setVisible(settings.type == TrackItems.TRANSFERTABLE);
			}
		};
		typeButton = new Button(screen, xtop, ytop, width, height, GuiText.SELECTOR_TYPE.toString(settings.type)) {
			@Override
			public void onClick(Player.Hand hand) {
				showSelector(typeSelector);
			}
		};
		ytop += height;

		subTypeSelector = new ListSelector<TrackItems>(screen, width, 100, height, selectedWaySettings.type,
				Arrays.stream(TrackItems.values())
						.filter(i -> i != TrackItems.CROSSING && i != TrackItems.SWITCH && i != TrackItems.MULTISWITCH && i != TrackItems.TRANSFERTABLE && i != TrackItems.TURNTABLE )
						.sorted(Comparator.comparingInt(TrackItems::getOrder))
						.collect(Collectors.toMap(TrackItems::toString, g -> g, (u, v) -> u, LinkedHashMap::new))
		) {
			@Override
			public void onClick(TrackItems option) {
				selectedWaySettings.type = option;
				syncMultiSwitchInfo();

				if(selectedWaySettings.type == TrackItems.CUBICPARABOLA && selectedWaySettings.degrees > CubicCurve.cubicParabolaMaxAngle) {//pitch的slider也需要
					selectedWaySettings.degrees = (float) CubicCurve.cubicParabolaMaxAngle;
					degreesSlider.setValue(selectedWaySettings.degrees * Config.ConfigBalance.AnglePlacementSegmentation / 90);
				}else {
					degreesSlider.setValue(selectedWaySettings.degrees * Config.ConfigBalance.AnglePlacementSegmentation / 90);
				}
				degreesSlider.setText(GuiText.SELECTOR_QUARTERS.toString(degreesSlider.getValueInt() * (90.0/Config.ConfigBalance.AnglePlacementSegmentation)));

				nearPitchSlider.setValue(selectedWaySettings.pitchTag.getFloat("start"));
				nearPitchSlider.setText("near pitch:"+String.format("%.2f", nearPitchSlider.getValue()));

				farPitchSlider.setValue(selectedWaySettings.pitchTag.getFloat("end"));
				farPitchSlider.setText("far pitch:"+String.format("%.2f", farPitchSlider.getValue()));

				lengthInput.setText(""+selectedWaySettings.length);
				farRadiusInput.setText(""+selectedWaySettings.cubicParabolaTag.getInteger("farRadius"));

				typeButton.setText(GuiText.SELECTOR_TYPE.toString(selectedWaySettings.type));
				degreesSlider.setVisible(selectedWaySettings.type.hasQuarters());
				curvositySlider.setVisible(selectedWaySettings.type.hasCurvosity());
				smoothingButton.setVisible(selectedWaySettings.type.hasSmoothing());
				directionButton.setVisible(selectedWaySettings.type.hasDirection());
				farRadiusInput.setVisible(selectedWaySettings.type.hasFarRadius());
			}
		};

		//Transfer table doesn't have these property so we can have them overlapped
		smoothingButton = new Button(screen, xtop, ytop, width, height, GuiText.SELECTOR_SMOOTHING.toString(selectedWay==0?settings.smoothing:selectedWaySettings.smoothing)) {
			@Override
			public void onClick(Player.Hand hand) {
				if(selectedWay == 0) {
					settings.smoothing = next(settings.smoothing, hand);
					smoothingButton.setText(GuiText.SELECTOR_SMOOTHING.toString(settings.smoothing));
				} else{
					selectedWaySettings.smoothing = next(selectedWaySettings.smoothing, hand);
					smoothingButton.setText(GuiText.SELECTOR_SMOOTHING.toString(selectedWaySettings.smoothing));
					syncMultiSwitchInfo();
				}
			}
		};
		smoothingButton.setVisible(selectedWay==0?settings.type.hasSmoothing():selectedWaySettings.type.hasSmoothing());

		transfertableEntryCountSlider = new Slider(screen, 25+xtop, ytop, "", 1, 71, settings.transfertableEntryCount, false) {
			@Override
			public void onSlider() {
				settings.transfertableEntryCount = (int) this.getValue();
				transfertableEntryCountSlider.setText(
						GuiText.SELECTOR_TRANSFER_TABLE_ENTRY_COUNT.toString((int) transfertableEntryCountSlider.getValue()));
			}
		};
		transfertableEntryCountSlider.onSlider();
		ytop += height;

		directionButton = new Button(screen, xtop, ytop, width, height, GuiText.SELECTOR_DIRECTION.toString(selectedWay==0?settings.direction:selectedWaySettings.direction)) {
			@Override
			public void onClick(Player.Hand hand) {
				if(selectedWay==0) {
					settings.direction = next(settings.direction, hand);
					directionButton.setText(GuiText.SELECTOR_DIRECTION.toString(settings.direction));
				}else {
					selectedWaySettings.direction = next(selectedWaySettings.direction, hand);
					directionButton.setText(GuiText.SELECTOR_DIRECTION.toString(selectedWaySettings.direction));
					syncMultiSwitchInfo();
				}
			}
		};
		directionButton.setVisible(selectedWay==0?settings.type.hasDirection():selectedWaySettings.type.hasDirection());

		transfertableEntrySpacingSlider = new Slider(screen, 25+xtop, ytop, "", 1, 15, settings.transfertableEntrySpacing, false) {
			@Override
			public void onSlider() {
				settings.transfertableEntrySpacing = (int) this.getValue();
				transfertableEntrySpacingSlider.setText(
						GuiText.SELECTOR_TRANSFER_TABLE_ENTRY_SPACING.toString((int) transfertableEntrySpacingSlider.getValue()));
			}
		};
		transfertableEntrySpacingSlider.onSlider();
		ytop += height;


		this.degreesSlider = new Slider(screen, 25+xtop,  ytop, "", 1, Config.ConfigBalance.AnglePlacementSegmentation, selectedWay==0?(settings.degrees * Config.ConfigBalance.AnglePlacementSegmentation / 90):(selectedWaySettings.degrees / 90 * Config.ConfigBalance.AnglePlacementSegmentation), false) {
			@Override
			public void onSlider() {
				float degreeValue = degreesSlider.getValueInt() * (90F/Config.ConfigBalance.AnglePlacementSegmentation);
				if(selectedWay==0) {
					if(settings.type == TrackItems.CUBICPARABOLA){
						while (degreeValue >= CubicCurve.cubicParabolaMaxAngle || !CubicCurve.isCubicParabolaDeltaValid(settings.length,settings.degrees,settings.cubicParabolaTag.getInteger("farRadius"))){
							degreeValue -= 90F / Config.ConfigBalance.AnglePlacementSegmentation;
							if(Math.abs(degreeValue) < 1e-6) break;
						}
					}
					settings.degrees = degreeValue;
				}else {
					if(selectedWaySettings.type == TrackItems.CUBICPARABOLA){
						while (degreeValue >= CubicCurve.cubicParabolaMaxAngle || !CubicCurve.isCubicParabolaDeltaValid(selectedWaySettings.length,selectedWaySettings.degrees,selectedWaySettings.cubicParabolaTag.getInteger("farRadius"))){
							degreeValue -= 90F / Config.ConfigBalance.AnglePlacementSegmentation;
							if(Math.abs(degreeValue) < 1e-6) break;
						}
					}
					selectedWaySettings.degrees = degreeValue;
					syncMultiSwitchInfo();
				}
				degreesSlider.setText(GuiText.SELECTOR_QUARTERS.toString(this.getValueInt() * (90.0/Config.ConfigBalance.AnglePlacementSegmentation)));
			}
		};
		degreesSlider.onSlider();
		ytop += height;


		String toggleStraightAtP1Text;
		if(selectedWay==0){
			toggleStraightAtP1Text = "isForward"+settings.cubicParabolaTag.getBoolean("isForward");
		}else {
			toggleStraightAtP1Text = "isForward"+selectedWaySettings.cubicParabolaTag.getBoolean("isForward");
		}
		this.toggleStraightAtP1 = new Button(screen, xtop, ytop, width, height, toggleStraightAtP1Text) {
			@Override
			public void onClick(Player.Hand hand) {
				if(selectedWay==0){
					boolean wasForward = settings.cubicParabolaTag.getBoolean("isForward");
					settings.cubicParabolaTag.setBoolean("isForward",!wasForward);
					toggleStraightAtP1.setText("isForward"+settings.cubicParabolaTag.getBoolean("isForward"));
				}else {
					boolean wasForward = selectedWaySettings.cubicParabolaTag.getBoolean("isForward");
					selectedWaySettings.cubicParabolaTag.setBoolean("isForward",!wasForward);
					toggleStraightAtP1.setText("isForward"+selectedWaySettings.cubicParabolaTag.getBoolean("isForward"));
					syncMultiSwitchInfo();
				}
			}
		};
		toggleStraightAtP1.setVisible(selectedWay==0 ? (settings.type == TrackItems.CUBICPARABOLA) : (selectedWaySettings.type == TrackItems.CUBICPARABOLA));
		ytop += height;

		this.farRadiusInput = new TextField(screen, xtop, ytop, width-1, height);
		this.farRadiusInput.setText("" + (selectedWay==0 ? settings.cubicParabolaTag.getInteger("farRadius") : selectedWaySettings.cubicParabolaTag.getInteger("farRadius")));
		this.farRadiusInput.setValidator(s -> {
			if (s == null || s.length() == 0) {
				return true;
			}
			int val;
			try {
				val = Integer.parseInt(s);
			} catch (NumberFormatException e) {
				if(s.equals("-"))return true;
				return false;
			}
			int max = 0x3f3f3f3f;
			if(selectedWay==0) {
				if (val >= -1 && val!=0 && val <= max && CubicCurve.isCubicParabolaDeltaValid(settings.length,settings.degrees,val)) {
					settings.cubicParabolaTag.setInteger("farRadius", val);
					return true;
				}
			}else {
				if (val >= -1 && val!=0 && val <= max && CubicCurve.isCubicParabolaDeltaValid(selectedWaySettings.length,selectedWaySettings.degrees,val)) {
					selectedWaySettings.cubicParabolaTag.setInteger("farRadius", val);
					syncMultiSwitchInfo();
					return true;
				}
			}

			return false;
		});
		this.farRadiusInput.setFocused(true);
		this.farRadiusInput.setVisible(selectedWay==0 ? (settings.type == TrackItems.CUBICPARABOLA) : (selectedWaySettings.type == TrackItems.CUBICPARABOLA));


		this.curvositySlider = new Slider(screen, 25+xtop, ytop, "", 0.25, 1.5, selectedWay==0?settings.curvosity:selectedWaySettings.curvosity, true) {
			@Override
			public void onSlider() {
				if(selectedWay == 0) {
					settings.curvosity = (float) this.getValue();
					curvositySlider.setText(GuiText.SELECTOR_CURVOSITY.toString(String.format("%.2f", settings.curvosity)));
				} else{
					selectedWaySettings.curvosity = (float) this.getValue();
					syncMultiSwitchInfo();
					curvositySlider.setText(GuiText.SELECTOR_CURVOSITY.toString(String.format("%.2f", selectedWaySettings.curvosity)));
				}
			}
		};
		curvositySlider.onSlider();
		ytop += height;

		directionButton.setVisible(selectedWay==0?settings.type.hasDirection():selectedWaySettings.type.hasDirection());
		degreesSlider.setVisible(selectedWay==0?settings.type.hasQuarters():selectedWaySettings.type.hasQuarters());
		curvositySlider.setVisible(selectedWay==0?settings.type.hasCurvosity():selectedWaySettings.type.hasCurvosity());
		smoothingButton.setVisible(selectedWay==0?settings.type.hasSmoothing():selectedWaySettings.type.hasSmoothing());
		transfertableEntryCountSlider.setVisible(settings.type == TrackItems.TRANSFERTABLE && selectedWay==0);
		transfertableEntrySpacingSlider.setVisible(settings.type == TrackItems.TRANSFERTABLE && selectedWay==0);



		// Bottom Pane
		//width = 200;
		//height = 20;
		//xtop = GUIHelpers.getScreenWidth() / 2 - width;
		//ytop = -GUIHelpers.getScreenHeight() / 4;
		ytop = (int) (GUIHelpers.getScreenHeight() * 0.75 - height * 8);

		trackSelector = new ListSelector<TrackDefinition>(screen, width,  250, height,
				DefinitionManager.getTrack(settings.track),
				DefinitionManager.getTracks().stream().collect(Collectors.toMap(t -> t.name, g -> g, (u, v) -> u, LinkedHashMap::new))) {
			@Override
			public void onClick(TrackDefinition track) {
				settings.track = track.trackID;
				updateListSetting(mutable -> mutable.track = track.trackID);
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


		//multiSwitch buttons
		wayCircleButton = new Button(screen, xtop, ytop, width-105, height, "selected way:"+selectedWay) {
			@Override
			public void onClick(Player.Hand hand) {
				if(hand == Player.Hand.SECONDARY){
					updateSelectedWay((selectedWay+1)%(multiSwitchInfo.wayList.size()+1));
				}else {
					updateSelectedWay((selectedWay-1+multiSwitchInfo.wayList.size()+1)%(multiSwitchInfo.wayList.size()+1));
				}
				if(selectedWay != 0){
					showSelector(subTypeSelector);
					subTypeSelector.onClick(selectedWaySettings.type);
				}else {
					subTypeSelector.setVisible(false);
					typeSelector.onClick(settings.type);
				}
			}
		};
		insertWayButton = new Button(screen, xtop+width-100, ytop, height+20, height, "Insert") {
			@Override
			public void onClick(Player.Hand hand) {
				SingleWayInfo singleWayInfo = new SingleWayInfo(SingleWayInfo.defaultSettings,SingleWayInfo.defaultPos,null,selectedWay-1);
				multiSwitchInfo.wayList.add(selectedWay-1,singleWayInfo);
				for(int i=selectedWay;i<multiSwitchInfo.wayList.size();i++) {
					multiSwitchInfo.wayList.get(i).mutable().wayOrder++;
				}
				updateSelectedWay(selectedWay);
			}
		};
		addWayButton = new Button(screen, xtop+width-60, ytop, height+5, height, "Add") {
			@Override
			public void onClick(Player.Hand hand) {
				SingleWayInfo singleWayInfo = new SingleWayInfo(SingleWayInfo.defaultSettings,SingleWayInfo.defaultPos,null,multiSwitchInfo.wayList.size());
				multiSwitchInfo.wayList.add(singleWayInfo);
			}
		};
		delWayButton = new Button(screen, xtop+width-30, ytop, height+5, height, "Del") {
			@Override
			public void onClick(Player.Hand hand) {
				//in common case waylist and multiSwitchInfo should not be null
				if(selectedWay == 1 && multiSwitchInfo.wayList.size()>1) {
					multiSwitchInfo.wayList.remove(0);
					for(int i=0;i<multiSwitchInfo.wayList.size();i++) {
						multiSwitchInfo.wayList.get(i).mutable().wayOrder--;
					}
					updateSelectedWay(1);
				}else if(selectedWay != 0) {
					multiSwitchInfo.wayList.remove(selectedWay-1);
					for(int i=selectedWay-1;i<multiSwitchInfo.wayList.size();i++) {
						multiSwitchInfo.wayList.get(i).mutable().wayOrder--;
					}
					updateSelectedWay(selectedWay-1);
				}
			}
		};
		insertWayButton.setVisible(settings.type.isMulti());
		addWayButton.setVisible(settings.type.isMulti());
		delWayButton.setVisible(settings.type.isMulti());
		wayCircleButton.setVisible(settings.type.isMulti());
		ytop += height;

		railBedSelector = new ListSelector<ItemStack>(screen, width, 250, height, settings.railBed,
				oreDict.stream().collect(Collectors.toMap(TrackGui::getStackName, g -> g, (u, v) -> u, LinkedHashMap::new))
		) {
			@Override
			public void onClick(ItemStack option) {
				settings.railBed = option;
				updateListSetting(mutable -> mutable.railBed = option);
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
				settings.railBedFill = option;
				updateListSetting(mutable -> mutable.railBedFill = option);
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

		posTypeButton = new Button(screen, xtop, ytop, width, height, GuiText.SELECTOR_POSITION.toString(settings.posType)) {
			@Override
			public void onClick(Player.Hand hand) {
				settings.posType = next(settings.posType, hand);
				updateListSetting(mutable -> mutable.posType = settings.posType);
				posTypeButton.setText(GuiText.SELECTOR_POSITION.toString(settings.posType));
			}
		};
		ytop += height;

		//TODO:change to TextField?
		nearPitchSlider = new Slider(screen, 25+xtop, ytop, "near pitch:", -45.0, 45.0, settings.pitchTag.getFloat("start"), true) {
			@Override
			public void onSlider() {
				settings.pitchTag.setFloat("start",(float) this.getValue());
				updateListSetting(mutable -> mutable.pitchTag.setFloat("start",(float) this.getValue()));
				nearPitchSlider.setText("near pitch:"+String.format("%.2f", settings.pitchTag.getFloat("start")));
			}
		};
		nearPitchSlider.onSlider();

		ytop += height;

		//TODO:far pitch不能全同步修改
		farPitchSlider = new Slider(screen, 25+xtop, ytop, "far pitch:", -45.0, 45.0, settings.pitchTag.getFloat("end"), true) {
			@Override
			public void onSlider() {
				settings.pitchTag.setFloat("end",(float) this.getValue());
				updateListSetting(mutable -> mutable.pitchTag.setFloat("end",(float) this.getValue()));
				farPitchSlider.setText("far pitch:"+String.format("%.2f", settings.pitchTag.getFloat("end")));
			}
		};
		farPitchSlider.onSlider();

		ytop += height;

		isPreviewCB = new CheckBox(screen, xtop+2, ytop+2, GuiText.SELECTOR_PLACE_BLUEPRINT.toString(), settings.isPreview) {
			@Override
			public void onClick(Player.Hand hand) {
				settings.isPreview = isPreviewCB.isChecked();
				updateListSetting(mutable -> mutable.isPreview = settings.isPreview);
			}
		};
//		ytop += height;

		isGradeCrossingCB = new CheckBox(screen, xtop+102, ytop+2, GuiText.SELECTOR_GRADE_CROSSING.toString(), settings.isGradeCrossing) {
			@Override
			public void onClick(Player.Hand hand) {
				settings.isGradeCrossing = isGradeCrossingCB.isChecked();
				updateListSetting(mutable -> mutable.isGradeCrossing = settings.isGradeCrossing);
			}
		};
		ytop += height;

		Slider zoom_slider = new Slider(screen, GUIHelpers.getScreenWidth() / 2 - 150, (int) (GUIHelpers.getScreenHeight()*0.75 - height),
										GuiText.SLIDER_ZOOM.toString(), 0.1, 2, 1, true) {
			@Override
			public void onSlider() {
				zoom = this.getValue();
			}
		};
	}

	private void showSelector(ListSelector<?> selector) {
		boolean isVisible = selector.isVisible();

		gaugeSelector.setVisible(false);
		typeSelector.setVisible(false);
		subTypeSelector.setVisible(false);
		trackSelector.setVisible(false);
		railBedSelector.setVisible(false);
		railBedFillSelector.setVisible(false);

		selector.setVisible(!isVisible);
	}

	private void updateSelectedWay(int i) {
		selectedWay = i;
		selectedWaySettings = selectedWay==0 ? settings : multiSwitchInfo.wayList.get(selectedWay-1).settings.mutable();
		if(wayCircleButton!=null)wayCircleButton.setText("selected way:"+selectedWay);
	}

	private void syncMultiSwitchInfo() {
		if(selectedWay>0){
			SingleWayInfo update = multiSwitchInfo.wayList.get(selectedWay-1).with(mutable -> mutable.settings = selectedWaySettings.immutable());
			multiSwitchInfo.wayList.set(selectedWay-1, update);
		}
	}

	private void updateListSetting(Consumer<RailSettings.Mutable> mod) {
		for (int i = 0; i < multiSwitchInfo.wayList.size(); i++) {
			SingleWayInfo updated = multiSwitchInfo.wayList.get(i);
			RailSettings t = updated.settings.with(mod);
			updated = updated.with(m -> m.settings = t);
			multiSwitchInfo.wayList.set(i, updated);
		}
	}

	@Override
	public void onEnterKey(IScreenBuilder builder) {
		builder.close();
	}

	@Override
	public void onClose() {
		if (!this.lengthInput.getText().isEmpty()) {
			if (this.te != null) {
				new ItemRailUpdatePacket(te.getPos(), settings.immutable(), multiSwitchInfo.immutable()).sendToServer();
			} else {
				new ItemRailUpdatePacket(settings.immutable(),  multiSwitchInfo.immutable()).sendToServer();
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
					null, null, SwitchState.NONE, SwitchState.NONE, 0, true);

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
					null, null, SwitchState.NONE, SwitchState.NONE, 0, true);

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
				null, multiSwitchInfo.immutable(), SwitchState.NONE, SwitchState.NONE, tablePos, true);//why

		int length = info.settings.length;
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
}
