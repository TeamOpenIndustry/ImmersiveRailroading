package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.*;
import cam72cam.immersiverailroading.net.ItemRailUpdatePacket;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.util.IRFuzzy;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.screen.*;
import cam72cam.mod.gui.helpers.ItemPickerGUI;
import cam72cam.mod.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static cam72cam.immersiverailroading.gui.ClickListHelper.next;

public class TrackGui implements IScreen {
	private TileRailPreview te;
	private Button typeButton;
	private TextField lengthInput;
	private Slider degreesSlider;
	private CheckBox isPreviewCB;
	private CheckBox isGradeCrossingCB;
	private Button gaugeButton;
	private Button trackButton;
	private Button posTypeButton;
	private Button smoothingButton;
	private Button directionButton;
	private Button bedTypeButton;
	private Button bedFillButton;

	private int length;
	private float degrees;
	private Gauge gauge;
	private String track;
	private boolean isPreview;
	private boolean isGradeCrossing;
	private TrackItems type;
	private TrackPositionType posType;
	private TrackSmoothing smoothing;
	private TrackDirection direction;
	private ItemStack bed;
	private ItemStack bedFill;
	List<ItemStack> oreDict;

	private final Predicate<String> integerFilter = inputString -> {
		if (inputString == null || inputString.length() == 0) {
			return true;
		}
		int val;
		try {
			val = Integer.parseInt(inputString);
		} catch (NumberFormatException e) {
			return false;
		}
		return val > 0 && val <= 1000;
	};

	public TrackGui() {
		this(MinecraftClient.getPlayer().getHeldItem(Player.Hand.PRIMARY));
	}

	public TrackGui(TileRailPreview te) {
        this(te.getItem());
        this.te = te;
	}

	private TrackGui(ItemStack stack) {
		stack = stack.copy();
		RailSettings settings = RailSettings.from(stack);
		length = settings.length;
		degrees = settings.degrees;
		type = settings.type;
		gauge = settings.gauge;
		track = settings.track;
		posType = settings.posType;
		smoothing = settings.smoothing;
		direction = settings.direction;
		isPreview = settings.isPreview;
		isGradeCrossing = settings.isGradeCrossing;
		bed = settings.railBed;
		bedFill = settings.railBedFill;
		oreDict = new ArrayList<>();

		//if (!DefinitionManager.getTrackIDs().contains(type)) {
		//	track = DefinitionManager.getTrackIDs().stream().findFirst().getContents();
		//}
		
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
		trackButton = new Button(screen, 0 - 100, -24 + 0 * 22, GuiText.SELECTOR_TRACK.toString(DefinitionManager.getTrack(track).name)) {
			@Override
			public void onClick(Player.Hand hand) {
				track = next(DefinitionManager.getTrackIDs(), track, hand);
				trackButton.setText(GuiText.SELECTOR_TRACK.toString(DefinitionManager.getTrack(track).name));
			}
		};

		typeButton = new Button(screen, 0 - 100, -24 + 1 * 22 - 1, GuiText.SELECTOR_TYPE.toString(type)) {
			@Override
			public void onClick(Player.Hand hand) {
				type = next(type, hand);
				typeButton.setText(GuiText.SELECTOR_TYPE.toString(type));
				degreesSlider.setVisible(type == TrackItems.SWITCH || type == TrackItems.TURN);
				smoothingButton.setVisible(type == TrackItems.CUSTOM || type == TrackItems.SLOPE || type == TrackItems.TURN);
			}
		};

		this.lengthInput = new TextField(screen, 0 - 100,  - 24 + 2 * 22, 200, 20);
		this.lengthInput.setText("" + length);
		this.lengthInput.setValidator(this.integerFilter);
		this.lengthInput.setFocused(true);

		this.degreesSlider = new Slider(screen, 0 - 75,  - 24 + 3 * 22+1, "", 1, Config.ConfigBalance.AnglePlacementSegmentation, degrees / 90 * Config.ConfigBalance.AnglePlacementSegmentation, false) {
			@Override
			public void onSlider() {
				degreesSlider.setText(GuiText.SELECTOR_QUARTERS.toString(this.getValueInt() * (90.0/Config.ConfigBalance.AnglePlacementSegmentation)));
			}
		};
		degreesSlider.onSlider();
		degreesSlider.setVisible(type == TrackItems.SWITCH || type == TrackItems.TURN);

		bedTypeButton = new Button(screen, 0 - 100, -24 + 4 * 22, GuiText.SELECTOR_RAIL_BED.toString(getStackName(bed))) {
			@Override
			public void onClick(Player.Hand hand) {
				ItemPickerGUI ip = new ItemPickerGUI(oreDict, (ItemStack bed) -> {
					if (bed != null) {
						TrackGui.this.bed = bed;
						bedTypeButton.setText(GuiText.SELECTOR_RAIL_BED.toString(getStackName(bed)));
					}
					screen.show();
				});
				ip.choosenItem = bed;
				ip.show();
			}
		};

		bedFillButton = new Button(screen, 0 - 100, -24 + 5 * 22, GuiText.SELECTOR_RAIL_BED_FILL.toString(getStackName(bedFill))) {
			@Override
			public void onClick(Player.Hand hand) {
				ItemPickerGUI ip = new ItemPickerGUI(oreDict, (ItemStack bed) -> {
					if (bed != null) {
						TrackGui.this.bedFill = bed;
						bedFillButton.setText(GuiText.SELECTOR_RAIL_BED_FILL.toString(getStackName(bedFill)));
					}
					screen.show();
				});
				ip.choosenItem = bedFill;
				ip.show();
			}
		};

		posTypeButton = new Button(screen, 0 - 100, -24 + 6 * 22, GuiText.SELECTOR_POSITION.toString(posType)) {
			@Override
			public void onClick(Player.Hand hand) {
				posType = next(posType, hand);
				posTypeButton.setText(GuiText.SELECTOR_POSITION.toString(posType));
			}
		};

		smoothingButton = new Button(screen, 0 - 100, -24 + 7 * 22, GuiText.SELECTOR_SMOOTHING.toString(smoothing)) {
			@Override
			public void onClick(Player.Hand hand) {
				smoothing = next(smoothing, hand);
				smoothingButton.setText(GuiText.SELECTOR_SMOOTHING.toString(smoothing));
			}
		};
		smoothingButton.setVisible(type == TrackItems.CUSTOM || type == TrackItems.SLOPE || type == TrackItems.TURN);

		directionButton = new Button(screen, 0 - 100, -24 + 8 * 22, GuiText.SELECTOR_DIRECTION.toString(direction)) {
			@Override
			public void onClick(Player.Hand hand) {
				direction = next(direction, hand);
				directionButton.setText(GuiText.SELECTOR_DIRECTION.toString(direction));
			}
		};

		gaugeButton = new Button(screen, 0 - 100, -24 + 9 * 22, GuiText.SELECTOR_GAUGE.toString(gauge)) {
			@Override
			public void onClick(Player.Hand hand) {
				gauge = next(Gauge.values(), gauge, hand);
				gaugeButton.setText(GuiText.SELECTOR_GAUGE.toString(gauge));
			}
		};

		isPreviewCB = new CheckBox(screen, -75, -24 + 10 * 22 + 4, GuiText.SELECTOR_PLACE_BLUEPRINT.toString(), isPreview) {
			@Override
			public void onClick(Player.Hand hand) {
				isPreview = isPreviewCB.isChecked();
			}
		};

		isGradeCrossingCB = new CheckBox(screen, 0 - 75, -24 + 11 * 22 + 4, GuiText.SELECTOR_GRADE_CROSSING.toString(), isGradeCrossing) {
			@Override
			public void onClick(Player.Hand hand) {
				isGradeCrossing = isGradeCrossingCB.isChecked();
			}
		};
	}

	@Override
	public void onEnterKey(IScreenBuilder builder) {
		builder.close();
	}

	@Override
	public void onClose() {
		if (!this.lengthInput.getText().isEmpty()) {
			RailSettings settings = new RailSettings(gauge, track, type, Integer.parseInt(lengthInput.getText()), degreesSlider.getValueInt() * (90F/Config.ConfigBalance.AnglePlacementSegmentation),  posType, smoothing, direction, bed, bedFill, isPreview, isGradeCrossing);
			if (this.te != null) {
				new ItemRailUpdatePacket(te.getPos(), settings).sendToServer();
			} else {
				new ItemRailUpdatePacket(settings).sendToServer();
			}
		}
	}

	@Override
	public void draw(IScreenBuilder builder) {
		if (lengthInput.getText().isEmpty()) {
			return;
		}
		int scale = 8;

		// This could be more efficient...
		RailSettings settings = new RailSettings(gauge, track, type, Integer.parseInt(lengthInput.getText()), degreesSlider.getValueInt() * (90F/Config.ConfigBalance.AnglePlacementSegmentation),  posType, smoothing, direction, bed, bedFill, isPreview, isGradeCrossing);
		ItemStack stack = new ItemStack(IRItems.ITEM_TRACK_BLUEPRINT, 1);
		settings.write(stack);
		/*
		try (OpenGL.With matrix = OpenGL.matrix()) {
			GL11.glTranslated(GUIHelpers.getScreenWidth() / 2 + builder.getWidth() / 4, builder.getHeight() / 4, 0);
			GL11.glScaled(scale, scale, 1);
			GUIHelpers.drawItem(stack, 0, 0);
		}
		try (OpenGL.With matrix = OpenGL.matrix()) {
			GL11.glTranslated(GUIHelpers.getScreenWidth() / 2 - builder.getWidth() / 4, builder.getHeight() / 4, 0);
			GL11.glScaled(-scale, scale, 1);
			GUIHelpers.drawItem(stack, 0, 0);
		}*/
	}

}
