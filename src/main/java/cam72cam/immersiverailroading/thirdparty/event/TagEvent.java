package cam72cam.immersiverailroading.thirdparty.event;

import java.util.UUID;

import net.minecraftforge.fml.common.eventhandler.Event;

public abstract class TagEvent extends Event {
	public final UUID stockID;
	public String tag;
	public TagEvent(UUID stockID)
	{
		super();
		this.stockID = stockID;
	}
	
	public static class SetTagEvent extends TagEvent {

		public SetTagEvent(UUID stockID, String tag) {
			super(stockID);
			this.tag = tag;
		}
	}
	
	public static class GetTagEvent extends TagEvent {

		public GetTagEvent(UUID stockID) {
			super(stockID);
		}
	}
}
