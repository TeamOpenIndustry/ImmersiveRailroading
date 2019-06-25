package cam72cam.mod.world;

import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.RealBB;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.tile.TileEntity;
import cam72cam.mod.util.Facing;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber
public class World {

    /* Static access to loaded worlds */
    private static Map<net.minecraft.world.World, World> worlds = new HashMap<>();
    private static Map<Integer, World> worldsByID = new HashMap<>();

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        net.minecraft.world.World world = event.getWorld();
        World worldWrap = new World(world);
        worlds.put(world, worldWrap);
        worldsByID.put(world.provider.getDimension(), worldWrap);

        world.addEventListener(new WorldEventListener(worldWrap));
    }
    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        net.minecraft.world.World world = event.getWorld();
        worlds.remove(world);
        worldsByID.remove(world.provider.getDimension());
    }
    public static World get(net.minecraft.world.World world) {
        return worlds.get(world);
    }
    public static World get(int dimID) {
        return worldsByID.get(dimID);
    }

    public boolean doesBlockCollideWith(Vec3i bp, RealBB bb) {
        IBoundingBox bbb = IBoundingBox.from(internal.getBlockState(bp.internal).getCollisionBoundingBox(internal, bp.internal));
        return bbb != null && bb.intersects(bbb);
    }

    /* World Initialization */

    public final net.minecraft.world.World internal;
    public final boolean isClient;
    public final boolean isServer;
    private final List<Entity> entities;
    private final Map<Integer, Entity> entityByID;
    private final Map<UUID, Entity> entityByUUID;

    private World(net.minecraft.world.World world) {
        internal = world;
        isClient = world.isRemote;
        isServer = !world.isRemote;
        entities = new ArrayList<>();
        entityByID = new HashMap<>();
        entityByUUID = new HashMap<>();
    }

    /* Event Methods */

    void onEntityAdded(net.minecraft.entity.Entity entityIn) {
        Entity entity;
        if (entityIn instanceof ModdedEntity) {
            entity = ((ModdedEntity) entityIn).getSelf();
        } else if (entityIn instanceof EntityPlayer) {
            entity = new Player((EntityPlayer) entityIn);
        } else {
            entity = new Entity(entityIn);
        }
        entities.add(entity);
        entityByID.put(entityIn.getEntityId(), entity);
        entityByUUID.put(entity.getUUID(), entity);
    }
    void onEntityRemoved(net.minecraft.entity.Entity entity) {
        if (entityByUUID.containsKey(entity.getUniqueID())) {
            entities.remove(entityByUUID.get(entity.getUniqueID()));
            entityByID.remove(entity.getEntityId());
            entityByUUID.remove(entity.getUniqueID());
        }
    }

    /* Entity Methods */

    public <T extends Entity> T getEntity(int id, Class<T> type) {
        Entity ent = entityByID.get(id);
        if (ent == null) {
            return null;
        }
        if (!type.isInstance(ent)) {
            // TODO Warning???
            return null;
        }
        return (T)ent;
    }

    public <T extends Entity> T getEntity(UUID id, Class<T> type) {
        Entity ent = entityByUUID.get(id);
        if (ent == null) {
            return null;
        }
        if (!type.isInstance(ent)) {
            // TODO Warning???
            return null;
        }
        return (T)ent;
    }

    public <T extends Entity> List<T> getEntities(Class<T> type) {
        return getEntities((T val) -> true, type);
    }

    public <T extends Entity> List<T> getEntities(Predicate<T> filter, Class<T> type) {
        return entities.stream().map(entity -> entity.as(type)).filter(Objects::nonNull).filter(filter).collect(Collectors.toList());
    }

    public boolean spawnEntity(Entity ent) {
        return internal.spawnEntity(ent.internal);
    }
























    public <T extends net.minecraft.tileentity.TileEntity> T getTileEntity(Vec3i pos, Class<T> cls) {
        return getTileEntity(pos, cls, true);
    }
    public <T extends net.minecraft.tileentity.TileEntity> T getTileEntity(Vec3i pos, Class<T> cls, boolean create) {
        net.minecraft.tileentity.TileEntity ent = internal.getChunkFromBlockCoords(pos.internal).getTileEntity(pos.internal, create ? Chunk.EnumCreateEntityType.IMMEDIATE : Chunk.EnumCreateEntityType.CHECK);
        if (ent != null && cls.isInstance(ent)) {
            return (T) ent;
        }
        if (ent != null) {
            System.out.println("WHAAAA");
            System.out.println(ent.getClass());
            System.out.println(cls);
        }
        return null;
    }
    public void setTileEntity(Vec3i pos, TileEntity tile) {
        internal.setTileEntity(pos.internal, tile);
    }

    public void setToAir(Vec3i pos) {
        internal.setBlockToAir(pos.internal);
    }

    public Block getBlock(Vec3i pos) {
        return internal.getBlockState(pos.internal).getBlock();
    }

    public long getTime() {
        return internal.getWorldTime();
    }

    public double getTPS(int sampleSize) {
        if (internal.getMinecraftServer() == null) {
            return 20;
        }

        long[] ttl = internal.getMinecraftServer().tickTimeArray;

        sampleSize = Math.min(sampleSize, ttl.length);
        double ttus = 0;
        for (int i = 0; i < sampleSize; i++) {
            ttus += ttl[ttl.length - 1 - i] / (double)sampleSize;
        }

        if (ttus == 0) {
            ttus = 0.01;
        }

        double ttms = ttus * 1.0E-6D;
        return Math.min(1000.0 / ttms, 20);
    }

    public Vec3i getPrecipitationHeight(Vec3i offset) {
        return new Vec3i(internal.getPrecipitationHeight(offset.internal));
    }

    public boolean isAir(Vec3i ph) {
        return internal.isAirBlock(ph.internal);
    }

    public void setSnowLevel(Vec3i ph, int snowDown) {
        internal.setBlockState(ph.internal, Blocks.SNOW_LAYER.getDefaultState().withProperty(BlockSnow.LAYERS, snowDown));
    }

    public int getSnowLevel(Vec3i ph) {
        IBlockState state = internal.getBlockState(ph.internal);
        if (state.getBlock() == Blocks.SNOW_LAYER) {
            return state.getValue(BlockSnow.LAYERS);
        }
        return 0;
    }

    public boolean isSnow(Vec3i ph) {
        Block block = internal.getBlockState(ph.internal).getBlock();
        return block == Blocks.SNOW || block == Blocks.SNOW_LAYER;
    }

    public boolean isSnowBlock(Vec3i ph) {
        return internal.getBlockState(ph.internal).getBlock() == Blocks.SNOW;
    }

    public boolean isPrecipitating() {
        return internal.isRaining();
    }

    public boolean isBlockLoaded(Vec3i parent) {
        return internal.isBlockLoaded(parent.internal);
    }

    public void breakBlock(Vec3i pos) {
        this.breakBlock(pos, true);
    }
    public void breakBlock(Vec3i pos, boolean drop) {
        internal.destroyBlock(pos.internal, drop);
    }

    public void dropItem(ItemStack stack, Vec3i pos) {
        dropItem(stack, new Vec3d(pos));
    }
    public void dropItem(ItemStack stack, Vec3d pos) {
        internal.spawnEntity(new EntityItem(internal, pos.x, pos.y, pos.z, stack.internal));
    }

    public void setBlock(Vec3i pos, Block block) {
        internal.setBlockState(pos.internal, block.getDefaultState());
    }
    public void setBlock(Vec3i pos, ItemStack item) {
        internal.setBlockState(pos.internal, BlockUtil.itemToBlockState(item));
    }

    public boolean isTopSolid(Vec3i pos) {
        return internal.getBlockState(pos.internal).isTopSolid();
    }

    public int getRedstone(Vec3i pos) {
        int power = 0;
        for (Facing facing : Facing.values()) {
            power = Math.max(power, internal.getRedstonePower(pos.offset(facing).internal, facing.internal));
        }
        return power;
    }

    public void removeEntity(cam72cam.mod.entity.Entity entity) {
        internal.removeEntity(entity.internal);
    }

    public boolean canSeeSky(Vec3i position) {
        return internal.canSeeSky(position.internal);
    }

    public boolean isRaining(Vec3i position) {
        return internal.getBiome(position.internal).canRain();
    }
    public boolean isSnowing(Vec3i position) {
        return internal.getBiome(position.internal).isSnowyBiome();
    }

    public float getTemperature(Vec3i pos) {
        float mctemp = internal.getBiome(pos.internal).getTemperature(pos.internal);
        //https://www.reddit.com/r/Minecraft/comments/3eh7yu/the_rl_temperature_of_minecraft_biomes_revealed/ctex050/
        return (13.6484805403f*mctemp)+7.0879687222f;
    }
}
