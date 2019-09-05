package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.items.ItemTrackBlueprint;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.*;
import cam72cam.immersiverailroading.net.ItemRailUpdatePacket;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.util.IRFuzzy;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.gui.*;
import cam72cam.mod.gui.helpers.ItemPickerGUI;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.util.Hand;
import com.google.common.base.Predicate;

import java.util.ArrayList;
import java.util.List;

public class TrackGui implements IScreen {
	private TileRailPreview te;
	private Button typeButton;
	private TextField lengthInput;
	private Slider quartersSlider;
	private CheckBox isPreviewCB;
	private CheckBox isGradeCrossingCB;
	private Button gaugeButton;
	private Button trackButton;
	private Button posTypeButton;
	private Button directionButton;
	private Button bedTypeButton;
	private Button bedFillButton;

	private int length;
	private int quarters;
	private Gauge gauge;
	private String track;
	private boolean isPreview;
	private boolean isGradeCrossing;
	private TrackItems type;
	private TrackPositionType posType;
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
		this(MinecraftClient.getPlayer().getHeldItem(Hand.PRIMARY));
	}

	public TrackGui(TileRailPreview te) {
        this(te.getItem());
        this.te = te;
	}

	private TrackGui(ItemStack stack) {
		stack = stack.copy();
		RailSettings settings = ItemTrackBlueprint.settings(stack);
		length = settings.length;
		quarters = settings.quarters;
		type = settings.type;
		gauge = settings.gauge;
		track = settings.track;
		posType = settings.posType;
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

	public String getStackName(ItemStack stack) {
		if (stack.isEmpty()) {
			return GuiText.NONE.toString();
		}
		return stack.getDisplayName();
	}

	public void init(IScreenBuilder screen) {
		trackButton = new Button(screen, 0 - 100, -24 + 0 * 22, GuiText.SELECTOR_TRACK.toString(DefinitionManager.getTrack(track).name)) {
			@Override
			public void onClick(Hand hand) {
				List<String> defs = DefinitionManager.getTrackIDs();
				int idx = defs.indexOf(track);
				idx = (idx + 1) % defs.size();
				track = defs.get(idx);
				trackButton.setText(GuiText.SELECTOR_TRACK.toString(DefinitionManager.getTrack(track).name));
			}
		};

		typeButton = new Button(screen, 0 - 100, -24 + 1 * 22 - 1, GuiText.SELECTOR_TYPE.toString(type)) {
			@Override
			public void onClick(Hand hand) {
				type =  TrackItems.values()[((type.ordinal() + 1) % (TrackItems.values().length))];
				typeButton.setText(GuiText.SELECTOR_TYPE.toString(type));
				quartersSlider.setVisible(type == TrackItems.SWITCH || type == TrackItems.TURN);
			}
		};

		this.lengthInput = new TextField(screen, 0 - 100,  - 24 + 2 * 22, 200, 20);
		this.lengthInput.setText("" + length);
		this.lengthInput.setValidator(this.integerFilter);
		this.lengthInput.setFocused(true);

		this.quartersSlider = new Slider(screen, 0 - 75,  - 24 + 3 * 22+1, "", 1, 4, quarters, false) {
			@Override
			public void onSlider() {
				quartersSlider.setText(GuiText.SELECTOR_QUARTERS.toString(this.getValueInt() * (90.0/4)));
			}
		};
		quartersSlider.onSlider();
		quartersSlider.setVisible(type == TrackItems.SWITCH || type == TrackItems.TURN);

		bedTypeButton = new Button(screen, 0 - 100, -24 + 4 * 22, GuiText.SELECTOR_RAIL_BED.toString(getStackName(bed))) {
			@Override
			public void onClick(Hand hand) {
				ItemPickerGUI ip = new ItemPickerGUI(oreDict, (ItemStack bed) -> {
					TrackGui.this.bed = bed;
					bedTypeButton.setText(GuiText.SELECTOR_RAIL_BED.toString(getStackName(bed)));
					screen.show();
				});
				ip.choosenItem = bed;
				ip.show();
			}
		};

		bedFillButton = new Button(screen, 0 - 100, -24 + 5 * 22, GuiText.SELECTOR_RAIL_BED_FILL.toString(getStackName(bedFill))) {
			@Override
			public void onClick(Hand hand) {
				ItemPickerGUI ip = new ItemPickerGUI(oreDict, (ItemStack bed) -> {
					TrackGui.this.bedFill = bed;
					bedFillButton.setText(GuiText.SELECTOR_RAIL_BED_FILL.toString(getStackName(bedFill)));
					screen.show();
				});
				ip.choosenItem = bedFill;
				ip.show();
			}
		};

		posTypeButton = new Button(screen, 0 - 100, -24 + 6 * 22, GuiText.SELECTOR_POSITION.toString(posType)) {
			@Override
			public void onClick(Hand hand) {
				posType = TrackPositionType.values()[((posType.ordinal() + 1) % (TrackPositionType.values().length))];
				posTypeButton.setText(GuiText.SELECTOR_POSITION.toString(posType));
			}
		};

		directionButton = new Button(screen, 0 - 100, -24 + 7 * 22, GuiText.SELECTOR_DIRECTION.toString(direction)) {
			@Override
			public void onClick(Hand hand) {
				direction = TrackDirection.values()[((direction.ordinal() + 1) % (TrackDirection.values().length))];
				directionButton.setText(GuiText.SELECTOR_DIRECTION.toString(direction));
			}
		};

		gaugeButton = new Button(screen, 0 - 100, -24 + 8 * 22, GuiText.SELECTOR_GAUGE.toString(gauge)) {
			@Override
			public void onClick(Hand hand) {
				gauge = gauge.next();
				gaugeButton.setText(GuiText.SELECTOR_GAUGE.toString(gauge));
			}
		};

		isPreviewCB = new CheckBox(screen, -75, -24 + 9 * 22 + 4, GuiText.SELECTOR_PLACE_BLUEPRINT.toString(), isPreview) {
			@Override
			public void onClick(Hand hand) {
				isPreview = isPreviewCB.isChecked();
			}
		};

		isGradeCrossingCB = new CheckBox(screen, 0 - 75, -24 + 10 * 22 + 4, GuiText.SELECTOR_GRADE_CROSSING.toString(), isGradeCrossing) {
			@Override
			public void onClick(Hand hand) {
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
			RailSettings settings = new RailSettings(gauge, track, type, Integer.parseInt(lengthInput.getText()), quartersSlider.getValueInt(),  posType, direction, bed, bedFill, isPreview, isGradeCrossing);
			if (this.te != null) {
				new ItemRailUpdatePacket(te.pos, settings).sendToServer();
			} else {
				new ItemRailUpdatePacket(settings).sendToServer();
			}
		}
	}

	@Override
	public void draw(IScreenBuilder builder) {

	}

}
