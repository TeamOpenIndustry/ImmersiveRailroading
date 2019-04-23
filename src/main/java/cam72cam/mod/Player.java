package cam72cam.mod;

import cam72cam.mod.util.Hand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ITextComponent;

public class Player {
    public final EntityPlayer internal;

    public Player(EntityPlayer player) {
        this.internal = player;
    }

    public ItemStack getHeldItem(Hand hand) {
        return new ItemStack(internal.getHeldItem(hand.internal));
    }

    public void sendMessage(ITextComponent o) {
        internal.sendMessage(o);
    }

    public boolean isCrouching() {
        return internal.isSneaking();
    }
}
