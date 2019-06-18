package cam72cam.immersiverailroading.thirdparty.event;

import java.util.UUID;

import net.minecraftforge.fml.common.eventhandler.Event;

public class TagEvent extends Event {
	private EventType eventType;
	private UUID stockID;
	public TagEvent(EventType eventType, UUID stockID)
	{
		super();
		this.eventType = eventType;
		this.stockID = stockID;
	}
	
	public String tag;
	
	public EventType getEventType()
	{
		return eventType;
	}
	
	public UUID getStockID()
	{
		return stockID;
	}
	
	public enum EventType
	{
		GetTag,
		SetTag
	}
}
