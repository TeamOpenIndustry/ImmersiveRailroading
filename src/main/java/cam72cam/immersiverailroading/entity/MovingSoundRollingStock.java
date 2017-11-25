package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.library.Gauge;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;

public class MovingSoundRollingStock extends MovingSound {

	private EntityRollingStock train;
	private boolean dyamicRate = false;
	private boolean dynamicPitch;

	public MovingSoundRollingStock(EntityRollingStock train, SoundEvent soundIn, SoundCategory categoryIn) {
		super(soundIn, categoryIn);
		this.train = train;
	}
	
	public void setVolume(float volume) {
		this.volume = volume;
	}
	
	public void donePlaying() {
		this.repeatDelay = 0;
		this.repeat = false;
		this.donePlaying = true;
	}
	
	public void setDynamicRate() {
		this.repeat = true;
		this.dyamicRate = true;
	}
	
	public void setDynamicPitch() {
		this.repeat = true;
		this.dynamicPitch = true;
	}

	@Override
	public void update() {
		if (this.train.isDead) {
			this.donePlaying = true;
			return;
		}

        this.xPosF = (float)this.train.posX;
        this.yPosF = (float)this.train.posY;
        this.zPosF = (float)this.train.posZ;
        
        if (train instanceof Locomotive) {
        	Locomotive loco = (Locomotive) train;
	    	double speed = Math.abs(loco.getCurrentSpeed().minecraft());
	    	double maxSpeed = Math.abs(loco.getDefinition().getMaxSpeed(Gauge.STANDARD.get()).minecraft());
	        if (this.dyamicRate) {
	    		// Set repeat delay to speed
	            this.repeatDelay = (int) (1 - speed/maxSpeed);
	        }
	        
	        if (this.dynamicPitch) {
	        	// Set pitch to speed
	        	this.pitch = (int) (1 + speed/maxSpeed);
	        }
        }
	}
}
