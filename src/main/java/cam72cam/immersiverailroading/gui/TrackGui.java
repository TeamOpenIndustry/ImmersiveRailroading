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
import cam72cam.immersiverailroading.track.BuilderTurnTable;
import cam72cam.immersiverailroading.track.TrackBase;
import cam72cam.immersiverailroading.util.IRFuzzy;
import cam72cam.immersiverailroading.util.PlacementInfo;
import cam72cam.immersiverailroading.util.RailInfo;
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
import java.util.stream.Collectors;

import static cam72cam.immersiverailroading.gui.ClickListHelper.next;
import static cam72cam.immersiverailroading.gui.components.GuiUtils.fitString;

public class TrackGui implements IScreen {
	long frame;

	private TileRailPreview te;
	private Button typeButton;
	private TextField lengthInput;
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
	private ListSelector<TrackDefinition>  trackSelector;
	private ListSelector<ItemStack> railBedSelector;
	private ListSelector<ItemStack> railBedFillSelector;

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

		this.lengthInput = new TextField(screen, xtop, ytop, width-1, height);
		this.lengthInput.setText("" + settings.length);
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
			if (settings.type.shouldRestrictLength()) {
				max = BuilderTurnTable.maxLength(settings.gauge);
			}
			if (val > 0 && val <= max) {
				settings.length = val;
				return true;
			}
			return false;
		});
		this.lengthInput.setFocused(true);
		ytop += height;

		gaugeSelector = new ListSelector<Gauge>(screen, width, 100, height, settings.gauge,
				Gauge.values().stream().collect(Collectors.toMap(Gauge::toString, g -> g, (u, v) -> u, LinkedHashMap::new))
		) {
			@Override
			public void onClick(Gauge gauge) {
				settings.gauge = gauge;
				gaugeButton.setText(GuiText.SELECTOR_GAUGE.toString(settings.gauge));
				if (settings.type.shouldRestrictLength()) {
					lengthInput.setText("" + Math.min(Integer.parseInt(lengthInput.getText()), BuilderTurnTable.maxLength(settings.gauge))); // revalidate
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
						.collect(Collectors.toMap(TrackItems::toString, g -> g, (u, v) -> u, LinkedHashMap::new))
		) {
			@Override
			public void onClick(TrackItems option) {
				settings.type = option;
				typeButton.setText(GuiText.SELECTOR_TYPE.toString(settings.type));
				degreesSlider.setVisible(settings.type.hasQuarters());
				curvositySlider.setVisible(settings.type.hasCurvosity());
				smoothingButton.setVisible(settings.type.hasSmoothing());
				directionButton.setVisible(settings.type.hasDirection());
				if (settings.type.shouldRestrictLength()) {
					lengthInput.setText("" + Math.min(Integer.parseInt(lengthInput.getText()), BuilderTurnTable.maxLength(settings.gauge))); // revalidate
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

		//Transfer table doesn't have this property so we can have them overlapped
		smoothingButton = new Button(screen, xtop, ytop, width, height, GuiText.SELECTOR_SMOOTHING.toString(settings.smoothing)) {
			@Override
			public void onClick(Player.Hand hand) {
				settings.smoothing = next(settings.smoothing, hand);
				smoothingButton.setText(GuiText.SELECTOR_SMOOTHING.toString(settings.smoothing));
			}
		};
		smoothingButton.setVisible(settings.type.hasSmoothing());

		transfertableEntryCountSlider = new Slider(screen, 25+xtop, ytop, "", 1, 71, settings.transfertableEntryCount, false) {
			@Override
			public void onSlider() {
				settings.transfertableEntryCount = (int) this.getValue();
				transfertableEntryCountSlider.setText(
						GuiText.TRACK_TRANSFER_TABLE_ENTRY_COUNT.toString((int) transfertableEntryCountSlider.getValue()));
			}
		};
		transfertableEntryCountSlider.onSlider();
		ytop += height;

		directionButton = new Button(screen, xtop, ytop, width, height, GuiText.SELECTOR_DIRECTION.toString(settings.direction)) {
			@Override
			public void onClick(Player.Hand hand) {
				settings.direction = next(settings.direction, hand);
				directionButton.setText(GuiText.SELECTOR_DIRECTION.toString(settings.direction));
			}
		};
		directionButton.setVisible(settings.type.hasDirection());

		transfertableEntrySpacingSlider = new Slider(screen, 25+xtop, ytop, "", 1, 15, settings.transfertableEntrySpacing, false) {
			@Override
			public void onSlider() {
				settings.transfertableEntrySpacing = (int) this.getValue();
				transfertableEntrySpacingSlider.setText(
						GuiText.TRACK_TRANSFER_TABLE_ENTRY_SPACING.toString((int) transfertableEntrySpacingSlider.getValue()));
			}
		};
		transfertableEntrySpacingSlider.onSlider();
		ytop += height;


		this.degreesSlider = new Slider(screen, 25+xtop,  ytop, "", 1, Config.ConfigBalance.AnglePlacementSegmentation, settings.degrees / 90 * Config.ConfigBalance.AnglePlacementSegmentation, false) {
			@Override
			public void onSlider() {
				settings.degrees = degreesSlider.getValueInt() * (90F/Config.ConfigBalance.AnglePlacementSegmentation);
				degreesSlider.setText(GuiText.SELECTOR_QUARTERS.toString(this.getValueInt() * (90.0/Config.ConfigBalance.AnglePlacementSegmentation)));
			}
		};
		degreesSlider.onSlider();
		ytop += height;


		this.curvositySlider = new Slider(screen, 25+xtop, ytop, "", 0.25, 1.5, settings.curvosity, true) {
			@Override
			public void onSlider() {
				settings.curvosity = (float) this.getValue();
				curvositySlider.setText(GuiText.SELECTOR_CURVOSITY.toString(String.format("%.2f", settings.curvosity)));
			}
		};
		curvositySlider.onSlider();
		ytop += height;

		directionButton.setVisible(settings.type.hasDirection());
		degreesSlider.setVisible(settings.type.hasQuarters());
		curvositySlider.setVisible(settings.type.hasCurvosity());
		smoothingButton.setVisible(settings.type.hasSmoothing());
		transfertableEntryCountSlider.setVisible(settings.type == TrackItems.TRANSFERTABLE);
		transfertableEntrySpacingSlider.setVisible(settings.type == TrackItems.TRANSFERTABLE);



		// Bottom Pane
		//width = 200;
		//height = 20;
		//xtop = GUIHelpers.getScreenWidth() / 2 - width;
		//ytop = -GUIHelpers.getScreenHeight() / 4;
		ytop = (int) (GUIHelpers.getScreenHeight() * 0.75 - height * 6);

		trackSelector = new ListSelector<TrackDefinition>(screen, width,  250, height,
				DefinitionManager.getTrack(settings.track),
				DefinitionManager.getTracks().stream().collect(Collectors.toMap(t -> t.name, g -> g, (u, v) -> u, LinkedHashMap::new))) {
			@Override
			public void onClick(TrackDefinition track) {
				settings.track = track.trackID;
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
				settings.railBed = option;
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
				posTypeButton.setText(GuiText.SELECTOR_POSITION.toString(settings.posType));
			}
		};
		ytop += height;

		isPreviewCB = new CheckBox(screen, xtop+2, ytop+2, GuiText.SELECTOR_PLACE_BLUEPRINT.toString(), settings.isPreview) {
			@Override
			public void onClick(Player.Hand hand) {
				settings.isPreview = isPreviewCB.isChecked();
			}
		};
		ytop += height;

		isGradeCrossingCB = new CheckBox(screen, xtop+2, ytop+2, GuiText.SELECTOR_GRADE_CROSSING.toString(), settings.isGradeCrossing) {
			@Override
			public void onClick(Player.Hand hand) {
				settings.isGradeCrossing = isGradeCrossingCB.isChecked();
			}
		};
		ytop += height;

		Slider zoom_slider = new Slider(screen, GUIHelpers.getScreenWidth() / 2 - 150, (int) (GUIHelpers.getScreenHeight()*0.75 - height), "Zoom: ", 0.1, 2, 1, true) {
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
		trackSelector.setVisible(false);
		railBedSelector.setVisible(false);
		railBedFillSelector.setVisible(false);

		selector.setVisible(!isVisible);
	}

	@Override
	public void onEnterKey(IScreenBuilder builder) {
		builder.close();
	}

	@Override
	public void onClose() {
		if (!this.lengthInput.getText().isEmpty()) {
			if (this.te != null) {
				new ItemRailUpdatePacket(te.getPos(), settings.immutable()).sendToServer();
			} else {
				new ItemRailUpdatePacket(settings.immutable()).sendToServer();
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
					null, SwitchState.NONE, SwitchState.NONE, 0, true);

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
					null, SwitchState.NONE, SwitchState.NONE, 0, true);

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
							? (frame / 2.0) % settings.transfertableEntrySpacing * (settings.transfertableEntryCount - 1)
							: 0;
		RailInfo info = new RailInfo(
				settings.immutable().with(b -> {
					int length = b.length;
					if (length < 5) {
						length = 5;
					}
					if (settings.type == TrackItems.TURNTABLE) {
						length = Math.min(25, Math.max(10, length));
					}
					b.length = length;
				}),
				new PlacementInfo(new Vec3d(0.5, 0, 0.5), settings.direction, 0, null),
				null, SwitchState.NONE, SwitchState.NONE, tablePos, true);

		int length = info.settings.length;
		double scale = (GUIHelpers.getScreenWidth() / (length * 2.25)) * zoom;
		if (settings.type == TrackItems.TURNTABLE) {
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
