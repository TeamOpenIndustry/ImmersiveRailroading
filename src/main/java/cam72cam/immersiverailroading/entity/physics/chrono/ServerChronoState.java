package cam72cam.immersiverailroading.entity.physics.chrono;

import cam72cam.mod.net.Packet;
import cam72cam.mod.net.PacketDirection;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.world.World;

import java.util.HashMap;
import java.util.Map;

public class ServerChronoState extends Packet implements ChronoState {
    private final static Map<World, ServerChronoState> states = new HashMap<>();

    @TagField
    protected World world;
    // int -> two years of ticks, good enough
    @TagField
    protected int tickID;
    @TagField
    protected double ticksPerSecond;

    private ServerChronoState() {
        // only used by networking layer
    }

    private ServerChronoState(World world) {
        this.world = world;
        this.tickID = 0;
        this.ticksPerSecond = 20;
    }

    private void tick() {
        tickID += 1;
        ticksPerSecond = world.getTPS(20);
        if (tickID % 5 == 0) {
            sendToAll();
        }
    }

    @Override
    protected void handle() {
        ClientChronoState.updated(this);
    }

    @Override
    public double getTickID() {
        return tickID;
    }

    @Override
    public double getTickSkew() {
        return 1;
    }

    public int getServerTickID() {
        return tickID;
    }

    public static ServerChronoState getState(World world) {
        return states.computeIfAbsent(world, ServerChronoState::new);
    }

    static {
        World.onTick(w -> {
            ServerChronoState state = getState(w);
            if (state != null) {
                state.tick();
            }
        });
    }

    public static void register() {
        register(ServerChronoState::new, PacketDirection.ServerToClient);
    }
}
