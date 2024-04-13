package com.goldenfield192.ire.items;

import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.CreativeTab;
import cam72cam.mod.item.CustomItem;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.world.World;
import com.goldenfield192.ire.init.TabsInit;

import java.util.Collections;
import java.util.List;

public class WireItem extends CustomItem {
//    private TileConnector tc;

//    public Vec3i posStorage = new Vec3i(0,0,0);

    public WireItem(String modID, String name) {
        super(modID, name);
    }

    @Override
    public List<CreativeTab> getCreativeTabs() {
        return Collections.singletonList(TabsInit.IRE_MAIN);
    }

    //I don't know why it doesn't work after a change. See TileConnector#onClick
    @Deprecated
    @Override
    public ClickResult onClickBlock(Player player, World world, Vec3i pos, Player.Hand hand, Facing facing, Vec3d inBlockPos) {
        //获得当前对象
//        TileConnector storage = world.getBlockEntity(pos, TileConnector.class);
//        if(storage == null){
//            return ClickResult.REJECTED;
//        }
//        if (tc != null && !tc.getPos().equals(storage.getPos())) {
//            if (tc.getPos().y != storage.getPos().y) {
//                player.sendMessage(PlayerMessage.direct("Don't support slope for now!"));
//                return ClickResult.PASS;
//            }
//            player.sendMessage(PlayerMessage.direct("Linked"));
//            tc.addWire(true, storage.getPos().subtract(tc.getPos()));
//            storage.addWire(false, tc.getPos().subtract(storage.getPos()));
//            tc = null;
//        } else {
//            tc = storage;
//            player.sendMessage(PlayerMessage.direct("First point set: " + tc.getPos()));
//        }
        return ClickResult.ACCEPTED;
    }

    @Override
    public int getStackSize() {
        return 1;
    }
}
