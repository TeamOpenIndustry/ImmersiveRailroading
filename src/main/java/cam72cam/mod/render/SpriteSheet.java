package cam72cam.mod.render;

import net.minecraft.client.renderer.texture.TextureUtil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpriteSheet {
    private class SpriteInfo {
        final float uMin;
        final float uMax;
        final int uPx;
        final float vMin;
        final float vMax;
        final int vPx;
        final int texID;

        private SpriteInfo(float u, float uMax, int uPx, float v, float vMax, int vPx, int texID) {
            this.uMin = u;
            this.uMax = uMax;
            this.uPx = uPx;
            this.vMin = v;
            this.vMax = vMax;
            this.vPx = vPx;
            this.texID = texID;
        }
    }

    private final Map<String, SpriteInfo> sprites = new HashMap<>();
    private final List<SpriteInfo> unallocated = new ArrayList<>();
    public final int spriteSize;

    private void allocateSheet() {
        int textureID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        int sheetSize = Math.min(1024, GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE));
        TextureUtil.allocateTexture(textureID, sheetSize, sheetSize);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        for (int uPx = 0; uPx < sheetSize; uPx += spriteSize) {
            for (int vPx = 0; vPx < sheetSize; vPx += spriteSize) {
                float u = uPx / (float)sheetSize;
                float uMax = (uPx+spriteSize) / (float)sheetSize;
                float v = vPx / (float)sheetSize;
                float vMax = (vPx+spriteSize) / (float)sheetSize;
                unallocated.add(new SpriteInfo(u, uMax, uPx, v, vMax, vPx, textureID));
            }
        }
    }

    public SpriteSheet(int spriteSize) {
        this.spriteSize = spriteSize;
    }

    public void setSprite(String id, ByteBuffer pixels) {
        if (!sprites.containsKey(id)) {
            if (unallocated.size() == 0) {
                allocateSheet();
            }
            sprites.put(id, unallocated.remove(0));
        }
        SpriteInfo sprite = sprites.get(id);

        int prevTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, sprite.texID);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, sprite.uPx, sprite.vPx, spriteSize, spriteSize, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, pixels);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTexture);
    }

    public void renderSprite(String id) {
        SpriteInfo sprite = sprites.get(id);
        if (sprite == null) {
            return;
        }
        int prevTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, sprite.texID);
        GL11.glPushMatrix();
        {
            GL11.glRotated(180, 1, 0, 0);
            GL11.glTranslated(0, -1, 0);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glColor4f(1, 1, 1, 1);
            GL11.glTexCoord2f(sprite.uMin, sprite.vMin);
            GL11.glVertex3f(0, 0, 0);
            GL11.glTexCoord2f(sprite.uMin, sprite.vMax);
            GL11.glVertex3f(0, 1, 0);
            GL11.glTexCoord2f(sprite.uMax, sprite.vMax);
            GL11.glVertex3f(1, 1, 0);
            GL11.glTexCoord2f(sprite.uMax, sprite.vMin);
            GL11.glVertex3f(1, 0, 0);
            GL11.glEnd();
        }
        GL11.glPopMatrix();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTexture);
    }

    public void freeSprite(String id) {
        unallocated.add(sprites.remove(id));
        // TODO shrink number of sheets?
    }
}
