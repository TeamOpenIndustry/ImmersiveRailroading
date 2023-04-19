package cam72cam.immersiverailroading.entity.physics;

import cam72cam.immersiverailroading.Config.ConfigPerformance;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.concurrent.*;

/**
 * Utility class for the physics thread.
 */
public class PhysicsThread {
    /**
     * The active physics thread, or null if none is active.
     */
    @Nullable
    private static ExecutorService executor;

    /**
     * The minecraft server instance.
     */
    // TODO: Get this using UniversalModCore
    private static final MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();

    /**
     * The last job submitted to the physics thread by dimension id.
     */
    private static final HashMap<Object, Future<?>> lastJobs = new HashMap<>();

    /**
     * Gets or creates the physics thread, if enabled, or null if disabled.
     * @return The physics thread, or null if disabled.
     */
    @Nullable
    private static ExecutorService getExecutor() {
        if (ConfigPerformance.physicsThreads > 0) {
            if (executor == null) {
                ImmersiveRailroading.info("Starting physics %d thread(s)", ConfigPerformance.physicsThreads);
                executor = Executors.newFixedThreadPool(ConfigPerformance.physicsThreads, (t) -> new Thread(t, "ImmersiveRailroading Physics Thread"));
            }
            return executor;
        } else {
            return null;
        }
    }

    /**
     * Runs a task on the physics thread if enabled, or on the current thread if disabled.
     * @param world The world to run the task in.
     * @param task The task to run.
     */
    public static void run(World world, Runnable task) {
        ExecutorService executor = getExecutor();
        if (executor == null) {
            task.run();
        } else {
            Future<?> lastJob = lastJobs.get(world.getId());
            if (lastJob != null && !lastJob.isDone()) {
                ImmersiveRailroading.warn("Physics thread can't keep up. Skipping tick...");
            } else {
                lastJobs.put(world.getId(), executor.submit(task));
            }
        }
    }

    /**
     * Assures that the given block is loaded.
     * If the position is not loaded, the position is loaded on the server thread and the method waits for the result.
     * @param world The world to load the block in.
     * @param pos The position of the block to load.
     */
    public static void assureLoaded(World world, Vec3i pos) throws ExecutionException, InterruptedException, TimeoutException {
        if (!server.isCallingFromMinecraftThread() && !world.isBlockLoaded(pos)) {
            server.addScheduledTask(() -> world.keepLoaded(pos)).get(1, TimeUnit.SECONDS);
        }
    }
}
