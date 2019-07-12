package cam72cam.mod.entity;

import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.IInventory;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import net.minecraft.entity.player.EntityPlayer;

public class Player extends Entity {
    public final EntityPlayer internal;

    public Player(EntityPlayer player) {
        super(player);
        this.internal = player;
    }

    public ItemStack getHeldItem(Hand hand) {
        return new ItemStack(internal.getHeldItem(hand.internal));
    }

    public void sendMessage(PlayerMessage o) {
        internal.sendMessage(o.internal);
    }

    public boolean isCrouching() {
        return internal.isSneaking();
    }

    public boolean isCreative() {
        return internal.isCreative();
    }

    public float getYawHead() {
        return internal.rotationYawHead;
    }

    public void setHeldItem(Hand hand, ItemStack stack) {
        internal.setHeldItem(hand.internal, stack.internal);
    }

    public int getFoodLevel() {
        return internal.getFoodStats().getFoodLevel();
    }

    public void setFoodLevel(int i) {
        internal.getFoodStats().setFoodLevel(i);
    }

    public boolean isLeftKeyDown() {
        return (internal instanceof net.minecraft.client.entity.EntityPlayerSP) && ((net.minecraft.client.entity.EntityPlayerSP) internal).movementInput.leftKeyDown;
    }
    public boolean isRightKeyDown() {
        return (internal instanceof net.minecraft.client.entity.EntityPlayerSP) && ((net.minecraft.client.entity.EntityPlayerSP) internal).movementInput.rightKeyDown;
    }
    public boolean isForwardKeyDown() {
        return (internal instanceof net.minecraft.client.entity.EntityPlayerSP) && ((net.minecraft.client.entity.EntityPlayerSP) internal).movementInput.forwardKeyDown;
    }
    public boolean isBackKeyDown() {
        return (internal instanceof net.minecraft.client.entity.EntityPlayerSP) && ((net.minecraft.client.entity.EntityPlayerSP) internal).movementInput.backKeyDown;
    }

    public IInventory getInventory() {
        return IInventory.from(internal.inventory);
    }

    public ClickResult clickBlock(Hand hand, Vec3i pos, Vec3d hit) {
        return ClickResult.from(getHeldItem(hand).internal.onItemUse(internal, getWorld().internal, pos.internal, hand.internal, Facing.DOWN.internal, (float)hit.x, (float)hit.y, (float)hit.z));
    }
}
