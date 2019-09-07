package cam72cam.mod.render;

import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Mod.EventBusSubscriber(Side.CLIENT)
public class GLTexture {
    private static LinkedBlockingQueue queue = new LinkedBlockingQueue<>(1);
    private static ExecutorService saveImage = new ThreadPoolExecutor(5, 5, 60, TimeUnit.SECONDS, queue);
    private static ExecutorService prioritySaveImage = Executors.newFixedThreadPool(1);
    private static ExecutorService readImage = Executors.newFixedThreadPool(1);

    private final File texLoc;
    private final int cacheSeconds;
    private int glTexID;
    private long lastUsed;

    private final int width;
    private final int height;
    private IntBuffer pixels;
    private boolean loading;

    public GLTexture(String name, BufferedImage image, int cacheSeconds, boolean isSmallEnoughToUpload) {
        File cacheDir = Paths.get(Loader.instance().getConfigDir().getParentFile().getPath(), "cache", "modcore").toFile();
        cacheDir.mkdirs();

        this.texLoc = new File(cacheDir, name);
        this.glTexID = -1;
        this.cacheSeconds = cacheSeconds;
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.loading = false;


        if (isSmallEnoughToUpload) {
            this.pixels = imageToPixels(image);
            tryUpload();
        }

        if (!isSmallEnoughToUpload) {
            while (queue.size() != 0) {
                try {
                    Thread.sleep(1000);
                    System.out.println("Waiting for free write slot...");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        (isSmallEnoughToUpload ? prioritySaveImage : saveImage).submit(() -> {
            try {
                ImageIO.write(image, "png", texLoc);
            } catch (IOException e) {
                //TODO throw?
                e.printStackTrace();
            }
        });

        textures.add(this);
    }

    private IntBuffer imageToPixels(BufferedImage image) {
        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        IntBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4).asIntBuffer();
        buffer.put(pixels);
        buffer.flip();
        return buffer;
    }


    private int uploadTexture() {
        System.out.println("ALLOC " + this.texLoc);
        int textureID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        TextureUtil.allocateTexture(textureID, width, height);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, width, height, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, pixels);
        return textureID;
    }

    public boolean isLoaded() {
        return this.glTexID != -1;
    }

    public boolean tryUpload() {
        if (this.glTexID != -1) {
            return true;
        }
        if (pixels != null) {
            this.glTexID = uploadTexture();
            pixels = null;
        } else {
            if (loading) {
                return false;
            }
            loading = true;
            readImage.submit(() -> {
                try {
                    this.pixels = imageToPixels(ImageIO.read(texLoc));
                    loading = false;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            return false; //PENDING
        }
        return true;
    }

    public int bind() {
        lastUsed = System.currentTimeMillis();
        int currentTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        if (!tryUpload()) {
            return -1;
        }
        if (glTexID == currentTexture) {
            return -1; //NOP
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, glTexID);
        return currentTexture;
    }

    public void freeGL() {
        if (glTexID != -1) {
            GL11.glDeleteTextures(glTexID);
        }
        textures.remove(this);
    }

    public void dealloc() {
        if (this.glTexID != -1) {
            System.out.println("DEALLOC " + this.texLoc);
            GL11.glDeleteTextures(this.glTexID);
            this.glTexID = -1;
        }
    }

    private static List<GLTexture> textures = new ArrayList<>();
    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        for (GLTexture texture : textures) {
            if (texture.glTexID == -1) {
                continue;
            }
            if (System.currentTimeMillis() - texture.lastUsed > texture.cacheSeconds * 1000) {
                texture.dealloc();
            }
        }
    }
}
