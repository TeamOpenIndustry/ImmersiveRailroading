package cam72cam.mod.net;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.world.World;
import cam72cam.mod.entity.Player;
import cam72cam.mod.util.TagCompound;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public abstract class Packet implements IMessage {
    public static final SimpleNetworkWrapper net = NetworkRegistry.INSTANCE.newSimpleChannel("cam72cam.mod");
    private static int pktCount = 0;

    public static void register(Class<? extends Packet> cls, PacketDirection dir) {
        net.registerMessage(new Packet.Handler<>(), cls, pktCount++, dir == PacketDirection.ServerToClient ? Side.CLIENT : Side.SERVER);
    }

    protected TagCompound data = new TagCompound();
    MessageContext ctx;

    public abstract void handle();

    @Override
    public final void fromBytes(ByteBuf buf) {
        this.data = new TagCompound(ByteBufUtils.readTag(buf));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, data.internal);
    }

    protected final World getWorld() {
        return getPlayer().getWorld();
    }

    protected final Player getPlayer() {
        switch (ctx.side) {
            case CLIENT:
                return new Player(Minecraft.getMinecraft().player);
            case SERVER:
                return new Player(ctx.getServerHandler().player);
            default:
                return null;
        }
    }

    public void sendToAllAround(World world, Vec3d pos, double distance) {
        net.sendToAllAround(this,
                new NetworkRegistry.TargetPoint(world.internal.provider.getDimension(), pos.x, pos.y, pos.z, distance));
    }

    public void sendToServer() {
        net.sendToServer(this);
    }

    public void sendToAll() {
        net.sendToAll(this);
    }

    public static class Handler<T extends Packet> implements IMessageHandler<T, IMessage> {
        @Override
        public IMessage onMessage(T message, MessageContext ctx) {
            FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> handle(message, ctx));
            return null;
        }

        private void handle(T message, MessageContext ctx) {
            message.ctx = ctx;
            message.handle();
        }
    }
}
