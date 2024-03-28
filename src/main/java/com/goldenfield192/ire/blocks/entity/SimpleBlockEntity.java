//package com.goldenfield192.ire.blocks.entity;
//
//import cam72cam.mod.block.BlockEntity;
//import cam72cam.mod.entity.Player;
//import cam72cam.mod.item.ItemStack;
//import cam72cam.mod.math.Vec3d;
//import cam72cam.mod.math.Vec3i;
//import cam72cam.mod.serialization.*;
//import cam72cam.mod.util.Facing;
//import com.goldenfield192.ire.register.ItemsInit;
//import com.goldenfield192.ire.serializer.ConnectionStorageSerializer;
//import com.goldenfield192.ire.util.ConnectionStorage;
//import org.apache.commons.lang3.builder.EqualsBuilder;
//
//import java.util.LinkedList;
//
//class ConnectorBlockEntityBackup extends BlockEntity {
//
//    @TagField("facing")
//    private String facing = "";
//    public boolean isConnecting = false;
//
//    @TagField(value = "connect",mapper = ConnectionStorageSerializer.class)
//    public LinkedList<ConnectionStorage> connectGraph = new LinkedList<>();
//
//    private boolean lastTryBreakByCreativePlayer = false;
//
//    public void setFacing(String facing) {
//        this.facing = facing;
//    }
//
//    public String getFacing() {
//        return facing;
//    }
//
//    public ConnectorBlockEntityBackup(String facing) {
//        super();
//        this.facing = facing;
//        try {
//            TagCompound tc = new TagCompound()
//                    .setString("facing", facing);
//            save(tc);
//        }catch (SerializationException ignore){
//
//        }
//    }
//
//    @Override
//    public ItemStack onPick() {
//        return new ItemStack(ItemsInit.CONNECTOR_ITEM,1);
//    }
//
//    @Override
//    public boolean onClick(Player player, Player.Hand hand, Facing facing, Vec3d hit) {
//        System.out.println(this.connectGraph);
//        System.out.println(this.facing);
//        return super.onClick(player, hand, facing, hit);
//    }
//
//    @Override
//    public boolean tryBreak(Player player) {
//        if(player.isCreative())
//            lastTryBreakByCreativePlayer = true;
//        return true;
//    }
//
//    @Override
//    public void onBreak() {
//        if (!lastTryBreakByCreativePlayer) {
//            getWorld().dropItem(onPick(), getPos());
//        }
//        this.removeWire();
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//
//        if (o == null || getClass() != o.getClass()) return false;
//
//        TileConnector that = (TileConnector) o;
//
//        return new EqualsBuilder()
////                .append(facing, that.facing)
//                .append(this.getPos().x,that.getPos().x)
//                .append(this.getPos().y,that.getPos().y)
//                .append(this.getPos().z,that.getPos().z)
//                .isEquals();
//    }
//
//    public void addWire(boolean isFirst, Vec3i connectedTo){
//        this.connectGraph.add(new ConnectionStorage(isFirst,connectedTo));
//        System.out.println(this+" "+this.connectGraph);
////        markDirty();
//    }
//
//    public void removeWire(){
//        this.connectGraph.forEach(storage -> {
//            if(getWorld().isServer){
//                getWorld().getBlockEntity(storage.getTarget(),TileConnector.class)
//                        .connectGraph.remove();
//                System.out.println(getWorld().getBlockEntity(storage.getTarget(),TileConnector.class)
//                        .connectGraph);
//            }
//        });
////        markDirty();
//    }
//}
