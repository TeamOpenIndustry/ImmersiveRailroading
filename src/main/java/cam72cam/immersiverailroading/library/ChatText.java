package cam72cam.immersiverailroading.library;

import cam72cam.mod.text.TextUtil;
import cam72cam.mod.text.PlayerMessage;

public enum ChatText {
	STOCK_BUILT("stock.built"), 
	STOCK_DISSASEMBLED("stock.dissasembled"), 
	STOCK_MISSING("stock.missing"),
	STOCK_INVALID("stock.invalid"),
	STOCK_WRONG_GAUGE("stock.wrong_gauge"),
	DEADMANS_SWITCH_ENABLED("stock.deadmans_switch_enabled"),
	DEADMANS_SWITCH_DISABLED("stock.deadmans_switch_disabled"),
	COUPLER_FRONT("coupler.front"),
	COUPLER_BACK("coupler.back"),
	COUPLER_ENGAGED("coupler.engaged"),
	COUPLER_DISENGAGED("coupler.disengaged"),
	COUPLER_STATUS_COUPLED("coupler.status.coupled"),
	COUPLER_STATUS_DECOUPLED_ENGAGED("coupler.status.decoupled.engaged"),
	COUPLER_STATUS_DECOUPLED_DISENGAGED("coupler.status.decoupled.disengaged"),
	BUILD_MISSING("build.missing"),
	SET_AUGMENT_FILTER("augment.set"),
	RESET_AUGMENT_FILTER("augment.reset"),
	WOOD_PLANKS("misc.wood_planks"),
	INVALID_BLOCK("build.invalid_block"),
	RADIO_LINK("radio.link"),
	RADIO_RELINK("radio.relink"),
	RADIO_UNLINK("radio.unlink"),
	RADIO_NOLINK("radio.nolink"),
	RADIO_CANT_LINK("radio.cant_link"),
	BRUSH_NO_VARIANTS("brush.no.variants"),
	BRUSH_NEXT("brush.next"),
	BRUSH_MODE("brush.mode"),
	SWITCH_UNLOCKED("switch_state.unlocked"),
	SWITCH_LOCKED("switch_state.locked"),
	;
	
	private String value;
	ChatText(String value) {
		this.value = value;
	}
	
	private String getRaw() {
		return "chat.immersiverailroading:" + value;
	}
	
	public PlayerMessage getMessage(Object... objects) {
		return PlayerMessage.translate(getRaw(), objects);
	}

	@Override
	public String toString() {
		return TextUtil.translate(getRaw());
	}
}
