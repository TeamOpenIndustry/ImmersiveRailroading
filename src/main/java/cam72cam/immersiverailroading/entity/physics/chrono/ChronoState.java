package cam72cam.immersiverailroading.entity.physics.chrono;

import cam72cam.mod.world.World;

public interface ChronoState {
    static ChronoState getState(World world) {
        if (world.isClient) {
            return ClientChronoState.getState(world);
        } else {
            return ServerChronoState.getState(world);
        }
    }

    double getTickID();
}
