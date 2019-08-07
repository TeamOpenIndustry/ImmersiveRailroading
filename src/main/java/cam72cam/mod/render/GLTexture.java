package cam72cam.mod.render;

import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mod.EventBusSubscriber
public class GLTexture {
    private static ExecutorService saveImage = Executors.newFixedThreadPool(1);
    private static ExecutorService prioritySaveImage = Executors.newFixedThreadPool(1);
    private static ExecutorService readImage = Executors.newFixedThreadPool(1);

    private final File texLoc;
    private final int cacheSeconds;
    private int glTexID;
    private long lastUsed;

    private BufferedImage image;
    private boolean loading = false;

    public GLTexture(String name, BufferedImage image, int cacheSeconds, boolean upload) {
        File cacheDir = Paths.get(Loader.instance().getConfigDir().getParentFile().getPath(), "cache", "modcore").toFile();
        cacheDir.mkdirs();

        this.texLoc = new File(cacheDir, name);
        this.glTexID = -1;
        this.cacheSeconds = cacheSeconds;
        this.image = image;

        if (upload) {
            tryUpload();
        }

        (upload ? prioritySaveImage : saveImage).submit(() -> {
            try {
                ImageIO.write(GLTexture.this.image, "png", texLoc);
                synchronized (this) {
                    GLTexture.this.image = null;
                }
            } catch (IOException e) {
                //TODO throw?
                e.printStackTrace();
            }
        });

        textures.add(this);
    }


    private int uploadTexture(BufferedImage image) {
        System.out.println("ALLOC " + this.texLoc);
        int textureID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        TextureUtil.allocateTexture(textureID, image.getWidth(), image.getHeight());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        IntBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4).asIntBuffer();
        buffer.put(pixels);
        buffer.flip();
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, image.getWidth(), image.getHeight(), GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, buffer);
        return textureID;
    }

    public boolean isLoaded() {
        return this.glTexID != -1;
    }

    public boolean tryUpload() {
        if (this.glTexID != -1) {
            return true;
        }
        if (image != null) {
            synchronized (this) {
                this.glTexID = uploadTexture(image);
                loading = false;
            }
        } else {
            if (loading) {
                return false;
            }
            loading = true;
            readImage.submit(() -> {
                try {
                    this.image = ImageIO.read(texLoc);
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
                System.out.println("DEALLOC " + texture.texLoc);
                GL11.glDeleteTextures(texture.glTexID);
                texture.glTexID = -1;
            }
        }
    }
}
