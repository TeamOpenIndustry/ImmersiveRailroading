package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.gui.components.ListSelector;
import cam72cam.immersiverailroading.items.ItemTrackExchanger;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.*;
import cam72cam.immersiverailroading.net.ItemTrackExchangerUpdatePacket;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.TrackDefinition;
import cam72cam.immersiverailroading.render.rail.RailRender;
import cam72cam.immersiverailroading.track.TrackBase;
import cam72cam.immersiverailroading.util.EndPointData;
import cam72cam.immersiverailroading.util.IRFuzzy;
import cam72cam.immersiverailroading.util.PlacementInfo;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui.screen.Button;
import cam72cam.mod.gui.screen.IScreen;
import cam72cam.mod.gui.screen.IScreenBuilder;
import cam72cam.mod.gui.screen.Slider;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.opengl.RenderState;
import util.Matrix4;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static cam72cam.immersiverailroading.gui.TrackGui.getStackName;

public class TrackExchangerGui implements IScreen {
	long frame;

	private String track;
	private ItemStack railBed;
	private Gauge gauge;

	List<ItemStack> oreDict;

	private Button trackButton;
	private ListSelector trackSelector;

	private Button bedTypeButton;
	private ListSelector railBedSelector;

	private Button gaugeButton;
	private ListSelector gaugeSelector;

	private double zoom = 1;

	public TrackExchangerGui () {
		Player player = MinecraftClient.getPlayer();

		ItemTrackExchanger.Data data = new ItemTrackExchanger.Data(player.getHeldItem(Player.Hand.PRIMARY));
		this.track = data.track;
		this.railBed = data.railBed;
		this.gauge = data.gauge;

		oreDict = new ArrayList<>();
		oreDict.add(ItemStack.EMPTY);
		oreDict.addAll(IRFuzzy.IR_RAIL_BED.enumerate());
	}

	@Override
	public void init(IScreenBuilder screen) {
		int width = 200;
		int height = 20;
		int xtop = -GUIHelpers.getScreenWidth() / 2;
		int ytop = 0;

		trackButton = new Button(screen, xtop, ytop, width, height, GuiText.SELECTOR_TRACK.toString(DefinitionManager.getTrack(this.track).name)) {
			@Override
			public void onClick(Player.Hand hand) {
				showSelector(trackSelector);
			}
		};

		trackSelector = new ListSelector<TrackDefinition>(screen, width, 250, height,
				DefinitionManager.getTrack(track),
				DefinitionManager.getTracks().stream().collect(Collectors.toMap(t -> t.name, g -> g, (u, v) -> u, LinkedHashMap::new))) {
			@Override
			public void onClick(TrackDefinition newTrack) {
				track = newTrack.trackID;
				trackButton.setText(GuiText.SELECTOR_TRACK.toString(DefinitionManager.getTrack(track).name));
			}
		};

		ytop += height;

		bedTypeButton = new Button(screen, xtop, ytop, width, height, GuiText.SELECTOR_RAIL_BED.toString(getStackName(railBed))) {
			@Override
			public void onClick(Player.Hand hand) {
				showSelector(railBedSelector);
			}
		};

		railBedSelector = new ListSelector<ItemStack>(screen, width, 250, height, railBed,
				oreDict.stream().collect(Collectors.toMap(TrackGui::getStackName, g -> g, (u, v) -> u, LinkedHashMap::new))
		) {
			@Override
			public void onClick(ItemStack option) {
				railBed = option;
				bedTypeButton.setText(GuiText.SELECTOR_RAIL_BED.toString(getStackName(railBed)));
			}
		};

		ytop += height;

		gaugeButton = new Button(screen, xtop, ytop, width, height, GuiText.SELECTOR_GAUGE.toString(gauge)) {
			@Override
			public void onClick(Player.Hand hand) {
				showSelector(gaugeSelector);
			}
		};

		gaugeSelector = new ListSelector<Gauge>(screen, width, width / 2, height, gauge,
				Gauge.values().stream().collect(Collectors.toMap(Gauge::toString, g -> g, (u, v) -> u, LinkedHashMap::new))
		) {
			@Override
			public void onClick(Gauge newGauge) {
				gauge = newGauge;
				gaugeButton.setText(GuiText.SELECTOR_GAUGE.toString(newGauge));
			}
		};

		Slider zoom_slider = new Slider(screen, GUIHelpers.getScreenWidth() / 2 - 150, (int) (GUIHelpers.getScreenHeight()*0.75 - height),
				GuiText.SLIDER_ZOOM.toString(), 0.1, 2, 1, true) {
			@Override
			public void onSlider() {
				zoom = this.getValue();
			}
		};
	}

	@Override
	public void onEnterKey(IScreenBuilder builder) {
		builder.close();
	}

	@Override
	public void onClose() {
		new ItemTrackExchangerUpdatePacket(this.track, this.railBed, this.gauge).sendToServer();
	}

	@Override
	public void draw(IScreenBuilder builder, RenderState state) {
		frame++;

		GUIHelpers.drawRect(200, 0, GUIHelpers.getScreenWidth() - 200, GUIHelpers.getScreenHeight(), 0xCC000000);
		GUIHelpers.drawRect(0, 0, 200, GUIHelpers.getScreenHeight(), 0xEE000000);

		int baseScale = 8;
		// This could be more efficient...
		RailSettings settings = new RailSettings(gauge,
				track,
				TrackItems.STRAIGHT, TrackItems.STRAIGHT,
				10,
				0,
				1, TrackPositionType.FIXED,
				TrackSmoothing.BOTH,
				new EndPointData(0), new EndPointData(10),
                null,null,
				TrackDirection.NONE,
				railBed,
				ItemStack.EMPTY,
				false,
				false,
				1,
				1
		);
		ItemStack stack = new ItemStack(IRItems.ITEM_TRACK_BLUEPRINT, 1);
		settings.write(stack);

		if (trackSelector.isVisible() || railBedSelector.isVisible()) {
			ListSelector.ButtonRenderer<ItemStack> icons = (button, x, y, value) -> {
				Matrix4 zMatrix = new Matrix4();
				zMatrix.translate(0, 0, 100);

				GUIHelpers.drawItem(value, x+2, y+2, zMatrix);
			};

			railBedSelector.render(icons);

			double textScale = 1.5;
			String str = trackSelector.isVisible() ?
					GuiText.SELECTOR_TRACK.toString(DefinitionManager.getTrack(settings.track).name) :
					GuiText.SELECTOR_RAIL_BED.toString(getStackName(settings.railBed));

			GUIHelpers.drawCenteredString(str, (int) ((450 + (GUIHelpers.getScreenWidth()-450) / 2) / textScale), (int) (10 / textScale), 0xFFFFFF, new Matrix4().scale(textScale, textScale, textScale));

			RailInfo info = new RailInfo(
					settings.with(rendered -> {
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

		Matrix4 matrix = new Matrix4();
		matrix.translate(GUIHelpers.getScreenWidth() / 2 + builder.getWidth() / 4, builder.getHeight() / 4, 0);
		baseScale *= zoom;
		matrix.scale(baseScale, baseScale, 1);
		GUIHelpers.drawItem(stack, 0, 0, matrix);

//		matrix.setIdentity();
//		matrix.translate(GUIHelpers.getScreenWidth() / 2 - builder.getWidth() / 4, builder.getHeight() / 4, 0);
//		matrix.scale(-baseScale, BaseScale, 1);
//		GUIHelpers.drawItem(stack, 0, 0, matrix);
	}

	private void showSelector(ListSelector<?> selector) {
		boolean isVisible = selector.isVisible();

		gaugeSelector.setVisible(false);
		railBedSelector.setVisible(false);
		trackSelector.setVisible(false);

		selector.setVisible(!isVisible);
	}
}
