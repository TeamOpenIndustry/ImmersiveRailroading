package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.ImmersiveRailroading;

import java.util.concurrent.*;

public class ConcurrencyUtil {

    public static int calcThreadPoolSize() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static ExecutorService makeThreadPool(int threadPoolSize) {
        return Executors.newFixedThreadPool(threadPoolSize);
    }

    public static CompletionService<Object> makeCompletionService(ExecutorService service) {
        return new ExecutorCompletionService<>(service);
    }

    public static void shutdownService(ExecutorService service) {
        service.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!service.awaitTermination(5, TimeUnit.SECONDS)) {
                service.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!service.awaitTermination(5, TimeUnit.SECONDS)) {
                    ImmersiveRailroading.error("Thread pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            service.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

}
