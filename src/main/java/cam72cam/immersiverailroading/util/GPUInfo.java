package cam72cam.immersiverailroading.util;

import org.lwjgl.opengl.*;

public class GPUInfo {
    public static boolean hasGPUInfo() {
        ContextCapabilities capabilities = GLContext.getCapabilities();
        //TODO https://www.khronos.org/registry/OpenGL/extensions/MESA/GLX_MESA_query_renderer.txt
        return capabilities.GL_NVX_gpu_memory_info || capabilities.GL_ATI_meminfo;
    }
    public static int memFreeMB() {
        ContextCapabilities capabilities = GLContext.getCapabilities();
        if (capabilities.GL_NVX_gpu_memory_info) {
            return GL11.glGetInteger(NVXGpuMemoryInfo.GL_GPU_MEMORY_INFO_CURRENT_AVAILABLE_VIDMEM_NVX)/1024;
        }
        if (capabilities.GL_ATI_meminfo) {
            return GL11.glGetInteger(ATIMeminfo.GL_TEXTURE_FREE_MEMORY_ATI);
        }
        return 0;
    }
    public static int memTotalMB() {
        ContextCapabilities capabilities = GLContext.getCapabilities();
        if (capabilities.GL_NVX_gpu_memory_info) {
            return GL11.glGetInteger(NVXGpuMemoryInfo.GL_GPU_MEMORY_INFO_DEDICATED_VIDMEM_NVX)/1024;
        }
        return 1024;
    }

    public static String debug() {
        int free = GPUInfo.memFreeMB();
        int total = GPUInfo.memTotalMB();
        int used = total - free;
        int pct = (used*100 / total*100)/100;
        return String.format("GPU Memory: %d%% %d/%dMB", pct, used, total);
    }
}
