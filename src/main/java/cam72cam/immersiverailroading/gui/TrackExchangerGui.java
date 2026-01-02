package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.items.ItemTrackExchanger;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.*;
import cam72cam.immersiverailroading.net.ItemTrackExchangerUpdatePacket;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.util.IRFuzzy;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui.helpers.ItemPickerGUI;
import cam72cam.mod.gui.screen.Button;
import cam72cam.mod.gui.screen.IScreen;
import cam72cam.mod.gui.screen.IScreenBuilder;
import cam72cam.mod.input.Keyboard;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.opengl.RenderState;

import java.util.ArrayList;
import java.util.List;

import static cam72cam.immersiverailroading.gui.ClickListHelper.next;
import static cam72cam.immersiverailroading.gui.TrackGui.getStackName;

public class TrackExchangerGui implements IScreen {
	private Button trackSelector;
	private Button bedTypeButton;
	private Button gaugeButton;

	private String track;
	private ItemStack railBed;
	private Gauge gauge;

	List<ItemStack> oreDict;

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
		trackSelector = new Button(screen, -100, 1 * 22, GuiText.SELECTOR_TRACK.toString(DefinitionManager.getTrack(this.track).name),
                                   (hand, button) -> {
                                       track = next(DefinitionManager.getTrackIDs(), track, hand);
                                       button.setText(GuiText.SELECTOR_TRACK.toString(DefinitionManager.getTrack(track).name));
                                   });
		bedTypeButton = new Button(screen, -100, 2 * 22, GuiText.SELECTOR_RAIL_BED.toString(getStackName(railBed)),
                                   ((hand, button) -> {
                                       ItemPickerGUI ip = new ItemPickerGUI(oreDict, (ItemStack bed) -> {
                                           if (bed != null) {
                                               TrackExchangerGui.this.railBed = bed;
                                               button.setText(GuiText.SELECTOR_RAIL_BED.toString(getStackName(bed)));
                                           }
                                           screen.show();
                                       });
                                       ip.choosenItem = railBed;
                                       ip.show();
                                   }));
		gaugeButton = new Button(screen, -100, 3 * 22, GuiText.SELECTOR_GAUGE.toString(gauge),
                                 (hand, button) -> {
                                     gauge = next(Gauge.values(), gauge, hand);
                                     button.setText(GuiText.SELECTOR_GAUGE.toString(gauge));
                                 });
	}

    @Override
    public void onKeyType(IScreenBuilder builder, Keyboard.KeyCode keyCode) {
        if (keyCode == Keyboard.KeyCode.NUMPADENTER || keyCode == Keyboard.KeyCode.RETURN) {
            builder.close();
        }
    }

	@Override
	public void onClose() {
		new ItemTrackExchangerUpdatePacket(this.track, this.railBed, this.gauge).sendToServer();
	}

	@Override
	public void draw(IScreenBuilder builder, RenderState state) {
		int scale = 8;
		// This could be more efficient...
		RailSettings settings = new RailSettings(gauge,
				track,
				TrackItems.STRAIGHT,
				10,
				0,
				1, TrackPositionType.FIXED,
				TrackSmoothing.BOTH,
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

		state.translate(GUIHelpers.getScreenWidth() / 2 + builder.getWidth() / 4, builder.getHeight() / 4, 0);
		state.scale(scale, scale, 1);
		GUIHelpers.drawItem(stack, 0, 0, state.model_view());

		state.model_view().setIdentity();
		state.translate(GUIHelpers.getScreenWidth() / 2 - builder.getWidth() / 4, builder.getHeight() / 4, 0);
		state.scale(-scale, scale, 1);
		GUIHelpers.drawItem(stack, 0, 0, state.model_view());
	}
}
