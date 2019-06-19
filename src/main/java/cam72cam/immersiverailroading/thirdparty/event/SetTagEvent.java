package cam72cam.immersiverailroading.thirdparty.event;

import java.util.UUID;

public class SetTagEvent extends TagEvent {

	public SetTagEvent(UUID stockID, String tag) {
		super(stockID);
		this.tag = tag;
	}
}
