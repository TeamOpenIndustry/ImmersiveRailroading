package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.Config.ConfigPerformance;
import cam72cam.immersiverailroading.ImmersiveRailroading;

import javax.annotation.Nullable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class for the physics thread.
 */
public class PhysicsThread {
    /**
     * The physics thread to run physics on.
     * Null if disabled.
     */
    @Nullable
    private static ExecutorService executor;

    /**
     * Whether a job is currently running on the physics thread.
     */
    private static boolean jobRunning = false;

    /**
     * Starts the physics thread if enabled.
     */
    public static void start() {
        if (ConfigPerformance.multithreadedPhysics) {
            if (executor == null) {
                ImmersiveRailroading.info("Starting physics thread");
                executor = Executors.newSingleThreadExecutor((t) -> new Thread(t, "ImmersiveRailroading Physics Thread"));
            } else {
                ImmersiveRailroading.info("Physics thread already running");
            }
        } else {
            ImmersiveRailroading.info("Physics thread disabled");
        }
    }

    /**
     * Runs a task on the physics thread if enabled, or on the current thread if disabled.
     */
    public static void run(Runnable task) {
        if (executor == null) {
            task.run();
        } else {
            executor.submit(() -> {
                if (jobRunning) {
                    ImmersiveRailroading.warn("Physics thread can't keep up. Skipping tick.");
                } else {
                    try {
                        jobRunning = true;
                        task.run();
                    } catch (Exception e) {
                        ImmersiveRailroading.error("Error in physics thread. Skipping tick.");
                        ImmersiveRailroading.catching(e);
                    } finally {
                        jobRunning = false;
                    }
                }
            });
        }
    }
}
