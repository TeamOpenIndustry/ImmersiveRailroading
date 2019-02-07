package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.registry.LocomotiveCabcarDefinition;
import cam72cam.immersiverailroading.sound.ISound;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.world.World;

public class LocomotiveCabcar extends LocomotiveDiesel {

	private ISound horn;
	private ISound idle;
	private int turnOnOffDelay = 0;
	
	private static DataParameter<Float> ENGINE_TEMPERATURE = EntityDataManager.createKey(LocomotiveDiesel.class, DataSerializers.FLOAT);
	private static DataParameter<Boolean> TURNED_ON = EntityDataManager.createKey(LocomotiveDiesel.class, DataSerializers.BOOLEAN);
	private static DataParameter<Boolean> ENGINE_OVERHEATED = EntityDataManager.createKey(LocomotiveDiesel.class, DataSerializers.BOOLEAN);

	public LocomotiveCabcar(World world) {
		this(world, null);
	}

	public LocomotiveCabcar(World world, String defID) {
		super(world, defID);
		this.getDataManager().register(ENGINE_TEMPERATURE, ambientTemperature());
		this.getDataManager().register(TURNED_ON, false);
		this.getDataManager().register(ENGINE_OVERHEATED, false);
	}
	
	public float getEngineTemperature() {
		return this.dataManager.get(ENGINE_TEMPERATURE);
	}
	
	private void setEngineTemperature(float temp) {
		this.dataManager.set(ENGINE_TEMPERATURE, temp);
	}
	
	public void setTurnedOn(boolean value) {
		this.dataManager.set(TURNED_ON, value);
	}
	
	public boolean isTurnedOn() {
		return this.dataManager.get(TURNED_ON);
	}
	
	public boolean isRunning() {
		return isTurnedOn();
	}
	
	@Override
	public LocomotiveCabcarDefinition getDefinition() {
		return super.getDefinition(LocomotiveCabcarDefinition.class);
	}
	
	@Override
	public GuiTypes guiType() {
		return GuiTypes.CABCAR_LOCOMOTIVE;
	}
	
	@Override
	protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
		super.writeEntityToNBT(nbttagcompound);
		nbttagcompound.setFloat("engine_temperature", getEngineTemperature());
		nbttagcompound.setBoolean("turned_on", isTurnedOn());
		nbttagcompound.setBoolean("engine_overheated", isEngineOverheated());
	}
	
	@Override
	protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
		setEngineTemperature(nbttagcompound.getFloat("engine_temperature"));
		setTurnedOn(nbttagcompound.getBoolean("turned_on"));
		setEngineOverheated(nbttagcompound.getBoolean("engine_overheated"));
		super.readEntityFromNBT(nbttagcompound);
	}
	
	/*
	 * Sets the throttle or brake on all connected diesel locomotives if the throttle or brake has been changed
	 */
	@Override
	public void handleKeyPress(Entity source, KeyTypes key, boolean sprinting) {
		switch(key) {
			case START_STOP_ENGINE:
				if (turnOnOffDelay == 0) {
					turnOnOffDelay = 10;
					setTurnedOn(!isTurnedOn());
				}
				break;
			default:
				super.handleKeyPress(source, key, sprinting);
		}
	}
	
	@Override
	protected int getAvailableHP() {
		return 0;
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		
		if (world.isRemote) {
			if (ConfigSound.soundEnabled) {
				if (this.horn == null) {
					this.horn = ImmersiveRailroading.proxy.newSound(this.getDefinition().horn, false, 100, this.soundGauge());
					this.idle = ImmersiveRailroading.proxy.newSound(this.getDefinition().idle, true, 80, this.soundGauge());
				}
				
				if (this.getDataManager().get(HORN) != 0 && !horn.isPlaying() && isRunning()) {
					horn.play(getPositionVector());
				}
				
				if (isRunning()) {
					if (!idle.isPlaying()) {
						this.idle.play(getPositionVector());
					}
				} else {
					if (idle.isPlaying()) {
						idle.stop();
					}
				}
				
				if (horn.isPlaying()) {
					horn.setPosition(getPositionVector());
					horn.setVelocity(getVelocity());
					horn.update();
				}
				if (idle.isPlaying()) {
					idle.setPosition(getPositionVector());
					idle.setVelocity(getVelocity());
					idle.update();
				}
			}
			
			if (!ConfigGraphics.particlesEnabled) {
				return;
			}
			
			return;
		}
		
		float engineTemperature = isRunning() ? 100f : ambientTemperature();
		
		if (turnOnOffDelay > 0) {
			turnOnOffDelay -= 1;
		}
		
		setEngineTemperature(engineTemperature);
	}
}