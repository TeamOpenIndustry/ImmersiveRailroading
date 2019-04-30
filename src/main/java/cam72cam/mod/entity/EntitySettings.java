package cam72cam.mod.entity;

public class EntitySettings {
    public boolean canBePushed;
    boolean immuneToFire;
    float entityCollisionReduction;
    boolean attachedToPlayer;
    boolean defaultMovement;

    public EntitySettings() {
        this.immuneToFire = false;
        this.entityCollisionReduction = 0;
        this.attachedToPlayer = true;
        this.canBePushed = false;
        this.defaultMovement = true;
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
    public EntitySettings setDefaultMovement(boolean defaultMovement) {
        this.defaultMovement = defaultMovement;
        return this;
    }
}
