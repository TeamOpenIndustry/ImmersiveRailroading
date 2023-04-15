package cam72cam.immersiverailroading.util;

import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PhysicsThread {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Runs a task on the physics thread and returns a future.
     */
    public static Future<?> run(Runnable task) {
        return executor.submit(task);
    }
}
