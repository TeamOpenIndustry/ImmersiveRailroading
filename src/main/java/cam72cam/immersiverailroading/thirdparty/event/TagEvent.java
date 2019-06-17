package cam72cam.immersiverailroading.thirdparty.event;

import net.minecraftforge.fml.common.eventhandler.Event;

public class TagEvent extends Event {
	private EventType eventType;
	public TagEvent(EventType eventType)
	{
		super();
		this.eventType = eventType;
	}
	
	public String tag;
	public EventType getEventType()
	{
		return eventType;
	}
	
	public enum EventType
	{
		GetTag,
		SetTag
	}
}
