package cam72cam.mod.entity;

import net.minecraft.entity.EntityLiving;

public class Living extends Entity {
    private final EntityLiving living;

    public Living(net.minecraft.entity.EntityLiving entity) {
        super(entity);
        this.living = entity;
    }

    public boolean isLeashedTo(Player player) {
        return living.getLeashed() && living.getLeashHolder().getUniqueID().equals(player.getUUID());
    }

    public void unleash(Player player) {
        living.clearLeashed(true, !player.isCreative());
    }

    public void setLeashHolder(Player player) {
        living.setLeashHolder(player.internal, true);
    }

    public boolean canBeLeashedTo(Player player) {
        return living.canBeLeashedTo(player.internal);
    }
}
