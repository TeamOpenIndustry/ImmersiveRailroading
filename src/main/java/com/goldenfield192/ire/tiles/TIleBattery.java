package com.goldenfield192.ire.tiles;

import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.energy.Energy;
import cam72cam.mod.energy.IEnergy;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.util.Facing;

public class TIleBattery extends BlockEntity {
    @TagField("energy")
    private Energy energy = new Energy(0,1000);

    @Override
    public ItemStack onPick() {
        return null;
    }

    @Override
    public boolean onClick(Player player, Player.Hand hand, Facing facing, Vec3d hit) {
        if(player.getWorld().isServer)
            player.sendMessage(PlayerMessage.direct(String.valueOf(energy.getCurrent())));
        return true;
    }

    @Override
    public IEnergy getEnergy(Facing side) {
        return energy;
    }
}
