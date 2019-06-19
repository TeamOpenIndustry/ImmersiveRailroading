package cam72cam.immersiverailroading.thirdparty.event;

import java.util.UUID;

public class GetTagEvent extends TagEvent {

	public GetTagEvent(UUID stockID) {
		super(stockID);
	}
}
