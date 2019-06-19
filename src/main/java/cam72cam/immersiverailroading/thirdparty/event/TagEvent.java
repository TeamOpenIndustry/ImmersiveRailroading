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
}
