package cam72cam.mod.entity;

public class EntitySettings {
    public boolean canBePushed;
    boolean immuneToFire;
    float entityCollisionReduction;
    boolean attachedToPlayer;

    public EntitySettings() {
        this.immuneToFire = false;
        this.entityCollisionReduction = 0;
        this.attachedToPlayer = true;
        this.canBePushed = false;
    }

    public EntitySettings setImmuneToFire(boolean immuneToFire) {
        this.immuneToFire = immuneToFire;
        return this;
    }

    public EntitySettings setCollisionReduction(float reduction) {
        this.entityCollisionReduction = reduction;
        return this;
    }

    public EntitySettings setAttachedToPlayer(boolean attachedToPlayer) {
        this.attachedToPlayer = attachedToPlayer;
        return this;
    }

    public EntitySettings setCanBePushed(boolean canBePushed) {
        this.canBePushed = canBePushed;
        return this;
    }
}
