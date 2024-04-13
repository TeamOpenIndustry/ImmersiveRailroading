package com.goldenfield192.ire.tiles;

import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.mod.ModCore;
import cam72cam.mod.block.BlockEntityTickable;
import cam72cam.mod.energy.IEnergy;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.*;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.util.Facing;
import com.goldenfield192.ire.init.ItemsInit;
import com.goldenfield192.ire.serializer.MapVec3iBooleanMapper;
import com.goldenfield192.ire.util.ConnectionStorage;
import com.goldenfield192.ire.util.graph.DimGraph;
import com.goldenfield192.ire.util.graph.GraphHandler;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.util.*;

import static com.goldenfield192.ire.init.ItemsInit.WIRE_ITEM;
import static com.goldenfield192.ire.util.BlockUtil.vecToBlocks;
import static com.goldenfield192.ire.util.MathUtil.toVec3d;

//邻接表
public class TileConnector extends BlockEntityTickable {
    @TagField("facing")
    private int rotation;

    @TagField("offset")
    public Vec3d inBlockOffset;

    @TagField("subGraphID")
    private UUID subGraphID;

    @TagField(value = "connect", mapper = MapVec3iBooleanMapper.class)//存相对方块位置
    @TagSync
    private HashMap<Vec3i, ConnectionStorage> connection = new HashMap<>();

    private transient boolean lastTryBreakByCreativePlayer = false;

    public UUID getSubGraphID() {
        return subGraphID;
    }

    public int getRotation() {
        return rotation;
    }

    public HashMap<Vec3i, ConnectionStorage> getConnection() {
        return connection;
    }

    public void setSubGraphID(UUID subGraphID) {
        this.subGraphID = subGraphID;
    }

    public TileConnector(String facing) {
        super();
        switch (facing) {
            case "NORTH":
                rotation = 180;
                break;
            case "WEST":
                rotation = -90;
                break;
            case "EAST":
                rotation = 90;
        }
        inBlockOffset = new Vec3d(0, 0.74, 1.13);
        subGraphID = UUID.randomUUID();
        try {
            TagCompound tc = new TagCompound()
                    .setInteger("rotation", rotation)
                    .setVec3d("offset", inBlockOffset)
                    .setUUID("subGraphID", subGraphID);
            save(tc);
        } catch (SerializationException ignore) {
        }
    }


    @Override
    public boolean onClick(Player player, Player.Hand hand, Facing facing, Vec3d hit) {
        if (getWorld().isServer) {
            ModCore.info("------------For testing------------");
            ModCore.info(GraphHandler.getDimGraphByWorld(getWorld()).getGraphInDim().values().toString());
            ModCore.info(String.valueOf(this.subGraphID));
            ModCore.info(String.valueOf(
                    GraphHandler.getDimGraphByWorld(getWorld()).getSubGraphByUUID(subGraphID).energy.getCurrentInput()));
            ModCore.info(String.valueOf(
                    GraphHandler.getDimGraphByWorld(getWorld()).getSubGraphByUUID(subGraphID).energy.getCurrentOutput()));
            player.sendMessage(PlayerMessage.direct(String.valueOf(
                    GraphHandler.getDimGraphByWorld(getWorld()).getSubGraphByUUID(subGraphID).energy.getStatus())));
            //Moved from WireItem#onClickBlock since the code doesn't work there
            if (player.getHeldItem(Player.Hand.PRIMARY).is(WIRE_ITEM)) {

                attemptToAddWire(player);
            }
        }
        return true;
    }

    private void attemptToAddWire(Player player) {
        //物品的tag集
        TagCompound compound = player.getHeldItem(Player.Hand.PRIMARY).getTagCompound();
        if (compound != null && compound.getVec3i("pos") == null) {
            compound.setVec3i("pos", this.getPos());
            ItemStack is = new ItemStack(WIRE_ITEM, 1);
            is.setTagCompound(compound);
            player.setHeldItem(Player.Hand.PRIMARY, is);
            player.sendMessage(PlayerMessage.direct("First point set: " + this.getPos()));
        } else if (compound != null && getWorld().getBlockEntity(
                compound.getVec3i("pos"), TileConnector.class) != null) {
            TileConnector tc = getWorld().getBlockEntity(compound.getVec3i("pos"), TileConnector.class);
            if (tc != null && !tc.getPos().equals(this.getPos())) {
                if(tc.getPos().y != getPos().y)
                    player.sendMessage(PlayerMessage.direct("Slope isn't fully supported, you are using it at your own risk!"));
                player.sendMessage(PlayerMessage.direct("Linked"));
                tc.addWire(true, this.getPos().subtract(tc.getPos()));
                this.addWire(false, tc.getPos().subtract(this.getPos()));
                compound.setVec3i("pos", null);
                ItemStack is = new ItemStack(WIRE_ITEM, 1);
                is.setTagCompound(compound);
                player.setHeldItem(Player.Hand.PRIMARY, is);
            }
        }
    }

    @Override
    public void onBreak() {
        if (!lastTryBreakByCreativePlayer) {
            getWorld().dropItem(onPick(), getPos());
        }
        if (getWorld().isServer) {
            GraphHandler.getDimGraphByWorld(getWorld()).split(this.subGraphID, this, getWorld());
            GraphHandler.getDimGraphByWorld(getWorld()).remove(this.subGraphID, this.getPos());
        }
        removeWire();
    }

    @Override
    public void update() {
        if (getWorld().isServer) {
            DimGraph dimGraph = GraphHandler.getDimGraphByWorld(getWorld());
            if (!dimGraph.getGraphInDim().containsKey(this.subGraphID)) {
                dimGraph.addSubGraph(this.getPos(), this.subGraphID);
            }
        }
    }


    public void addWire(boolean isFirst, Vec3i relativeTarget) {
        TileConnector tc = getWorld()
                .getBlockEntity(getPos().add(relativeTarget), TileConnector.class);
        Vec3d offset2 = tc.inBlockOffset.rotateYaw(tc.getRotation());
        Vec3d rotated = toVec3d(relativeTarget).add(offset2)
                .subtract(this.inBlockOffset);
        if (isFirst) {
            this.connection.put(relativeTarget, new ConnectionStorage(true,rotated));
            vecToBlocks(this,getWorld().getBlockEntity(this.getPos().add(relativeTarget), TileConnector.class),new TileRail());
            UUID target = getWorld().getBlockEntity(this.getPos().add(relativeTarget), TileConnector.class).getSubGraphID();
            if (target != this.subGraphID && getWorld().isServer) {//IMPORTANT：逻辑相关加isServer
                GraphHandler.getDimGraphByWorld(getWorld()).merge(this.subGraphID, target, getWorld());
            }
        }else{
            this.connection.put(relativeTarget, new ConnectionStorage(false,new Vec3d(0,0,0)));
        }
        markDirty();
    }

    public void removeWire() {
        this.connection.forEach(((vec3i, bool) -> {
            TileConnector tc1 = getWorld().getBlockEntity(vec3i.add(this.getPos()), TileConnector.class);
            if (tc1 != null) {
                tc1.removeConnection(Vec3i.ZERO.subtract(vec3i));
            }
        }));
    }

    public void removeConnection(Vec3i relativePose) {
        if (this.connection.containsKey(relativePose)) {
            connection.remove(relativePose);
        }
        markDirty();
    }

    @Override
    public boolean tryBreak(Player player) {
        if (player.isCreative())
            lastTryBreakByCreativePlayer = true;
        return true;
    }

    @Override
    public ItemStack onPick() {
        return new ItemStack(ItemsInit.CONNECTOR_ITEM, 1);
    }

    @Override
    public double getRenderDistance() {
        return 1024;
    }

    @Override
    public IBoundingBox getRenderBoundingBox() {
        return IBoundingBox.INFINITE;
    }

    @Override
    public IEnergy getEnergy(Facing side) {
        return GraphHandler.getDimGraphByWorld(getWorld()).getSubGraphByUUID(subGraphID).energy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        TileConnector that = (TileConnector) o;

        return new EqualsBuilder()
                .append(this.getPos().x, that.getPos().x)
                .append(this.getPos().y, that.getPos().y)
                .append(this.getPos().z, that.getPos().z)
                .isEquals();
    }
}
