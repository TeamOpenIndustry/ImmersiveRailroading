package cam72cam.immersiverailroading.remotecontrol;

import cam72cam.immersiverailroading.util.Speed;
import cam72cam.mod.serialization.TagField;

public class RemoteControlData {
	@TagField("speed")
    public Speed speed;
	@TagField("throttle")
	public float throttle;
	@TagField("brakePressure")
	public float brakePressure;
	@TagField("indBrake")
	public float indBrake;
	@TagField("reverser")
	public float reverser;
	@TagField("emergency")
	public boolean emergency;
	@TagField("horn")
	public float horn;
	@TagField("sanding")
	public boolean sanding;
	@TagField("tractiveEffort")
	public float tractiveEffort;
	@TagField("brakeCylPressure")
	public float brakeCylPressure;
	@TagField("engine")
	public boolean engine;
	
	public RemoteControlData() {
	}
}
