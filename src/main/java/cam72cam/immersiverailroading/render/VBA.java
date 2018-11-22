package cam72cam.immersiverailroading.render;

import cam72cam.immersiverailroading.model.obj.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

public class VBA {
    private FloatBuffer vertexBuffer;
    private FloatBuffer normalBuffer;
    private FloatBuffer colorBuffer;
    private FloatBuffer texBuffer;
    private boolean has_vn;

    public VBA(int size) {
        vertexBuffer = BufferUtils.createFloatBuffer(size * 3 * 3);
        normalBuffer = BufferUtils.createFloatBuffer(size * 3 * 3);
        colorBuffer = BufferUtils.createFloatBuffer(size * 3 * 4);
        texBuffer = BufferUtils.createFloatBuffer(size * 3 * 2);
        has_vn = true;
    }
    public void addPoint(Vec3d v, Vec3d vn, Vec2f vt, float r, float g, float b, float a) {
        vertexBuffer.put((float) (v.x));
        vertexBuffer.put((float) (v.y));
        vertexBuffer.put((float) (v.z));
        if (vn != null) {
            normalBuffer.put((float) (vn.x));
            normalBuffer.put((float) (vn.y));
            normalBuffer.put((float) (vn.z));
        } else {
            has_vn = false;
        }
        texBuffer.put(vt.x);
        texBuffer.put(vt.y);
        colorBuffer.put(r);
        colorBuffer.put(g);
        colorBuffer.put(b);
        colorBuffer.put(a);
    }
    public void draw() {
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
        if (has_vn) {
            GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
        }

        vertexBuffer.flip();
        colorBuffer.flip();
        normalBuffer.flip();
        texBuffer.flip();
        GL11.glTexCoordPointer(2, 2 << 2, texBuffer);
        GL11.glColorPointer(4, 4 << 2, colorBuffer);
        if (has_vn) {
            GL11.glNormalPointer(3 << 2, normalBuffer);
        }
        GL11.glVertexPointer(3, 3 << 2, vertexBuffer);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertexBuffer.capacity() / 3);

        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
        if (has_vn) {
            GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
        }

        // Reset draw color (IMPORTANT)
        GL11.glColor4f(1, 1, 1, 1);
    }
}
