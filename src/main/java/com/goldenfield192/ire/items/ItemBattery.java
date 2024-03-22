package com.goldenfield192.ire.items;

import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.CreativeTab;
import cam72cam.mod.item.CustomItem;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.world.World;
import com.goldenfield192.ire.init.BlocksInit;
import com.goldenfield192.ire.init.TabsInit;

import java.util.Collections;
import java.util.List;

public class ItemBattery extends CustomItem{
    public ItemBattery(String modID, String name) {
        super(modID, name);
    }

    @Override
    public List<CreativeTab> getCreativeTabs() {
        return Collections.singletonList(TabsInit.IRE_MAIN);
    }

    @Override
    public ClickResult onClickBlock(Player player, World world, Vec3i pos, Player.Hand hand, Facing facing, Vec3d inBlockPos) {
        world.setBlock(pos.offset(facing), BlocksInit.BATTERY_BLOCK);
        return ClickResult.ACCEPTED;
    }
}
