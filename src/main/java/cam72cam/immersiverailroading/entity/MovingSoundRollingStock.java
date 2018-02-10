package cam72cam.immersiverailroading.entity;

import net.minecraft.client.audio.ISound;
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
		this.attenuationType = ISound.AttenuationType.NONE;
	}
	
	public void setVolume(float volume) {
		this.volume = volume;
	}
	
	public void donePlaying() {
		this.repeatDelay = 0;
		this.repeat = false;
		this.donePlaying = true;
	}
	
	public int getRepeatDelay()
    {
		System.out.println("CHECK");
        return this.repeatDelay;
    }
	
	public void setDynamicRate() {
		this.repeat = true;
		this.dyamicRate = true;
		this.update();
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
	    	double maxSpeed = Math.abs(loco.getDefinition().getMaxSpeed(loco.gauge).minecraft());
        	//this.pitch = (float) (1+speed/maxSpeed)/2;
        }
	}
	
}
