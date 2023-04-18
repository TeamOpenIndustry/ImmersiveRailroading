package cam72cam.immersiverailroading.physics;

import cam72cam.immersiverailroading.Config.ConfigPerformance;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.world.World;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
     * The last job submitted to the physics thread by dimension id.
     */
    private static final HashMap<Integer, Future<?>> lastJobs = new HashMap<Integer, Future<?>>();

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
}
