package cam72cam.immersiverailroading.render;

import cam72cam.immersiverailroading.model.obj.Vec2f;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.Map;

public class VBA {
    private Map<String, Pair<Integer, Integer>> groupIdx;
    private boolean isVBO;
    private int size;
    private FloatBuffer vertexBuffer;
    private FloatBuffer normalBuffer;
    private FloatBuffer colorBuffer;
    private FloatBuffer texBuffer;
    private int vbo = -1;
    private int vnbo = -1;
    private int vtbo = -1;
    private int vcbo = -1;
    private boolean has_vn = true;

    public VBA(int size) {
        this.size = size;
        vertexBuffer = BufferUtils.createFloatBuffer(size * 3 * 3);
        normalBuffer = BufferUtils.createFloatBuffer(size * 3 * 3);
        colorBuffer = BufferUtils.createFloatBuffer(size * 3 * 4);
        texBuffer = BufferUtils.createFloatBuffer(size * 3 * 2);
        isVBO = OpenGlHelper.useVbo();
    }

    public VBA(int size, Map<String, Pair<Integer, Integer>> groupIdx) {
        this(size);
        this.groupIdx = groupIdx;
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
        if (isVBO) {
            drawVBO(null);
        } else {
            drawVBA(null);
        }
    }
    public void draw(Iterable<String> groups) {
        if (isVBO) {
            drawVBO(groups);
        } else {
            drawVBA(groups);
        }
    }
    private void drawVBO(Iterable<String> groups) {
        int prev = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);

        if (vbo == -1) {
            vertexBuffer.flip();
            colorBuffer.flip();
            normalBuffer.flip();
            texBuffer.flip();

            vbo = GL15.glGenBuffers();
            vnbo = GL15.glGenBuffers();
            vtbo = GL15.glGenBuffers();
            vcbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);
            if (has_vn) {
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vnbo);
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, normalBuffer, GL15.GL_STATIC_DRAW);
            }
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vtbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, texBuffer, GL15.GL_STATIC_DRAW);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vcbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, colorBuffer, GL15.GL_STATIC_DRAW);

            vertexBuffer = null;
            normalBuffer = null;
            texBuffer = null;
            colorBuffer = null;
        }

        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
        if (has_vn) {
            GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
        }

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vtbo);
        GL11.glTexCoordPointer(2, GL11.GL_FLOAT, 0, 0);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vcbo);
        GL11.glColorPointer(4, GL11.GL_FLOAT, 0, 0);

        if (has_vn) {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vnbo);
            GL11.glNormalPointer(GL11.GL_FLOAT, 0, 0);
        }

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL11.glVertexPointer(3, GL11.GL_FLOAT, 0, 0);
        if (groups == null) {
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, size * 3);
        } else {
            for (String group : groups) {
                Pair<Integer, Integer> info = groupIdx.get(group);
                GL11.glDrawArrays(GL11.GL_TRIANGLES, info.getKey() * 3, info.getValue() * 3);
            }
        }

        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
        if (has_vn) {
            GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
        }

        // Reset draw color (IMPORTANT)
        GL11.glColor4f(1, 1, 1, 1);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, prev);
    }
    private void drawVBA(Iterable<String> groups) {
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
        if (groups == null) {
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, size * 3);
        } else {
            for (String group : groups) {
                Pair<Integer, Integer> info = groupIdx.get(group);
                GL11.glDrawArrays(GL11.GL_TRIANGLES, info.getKey() * 3, info.getValue() * 3);
            }
        }

        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
        if (has_vn) {
            GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
        }

        // Reset draw color (IMPORTANT)
        GL11.glColor4f(1, 1, 1, 1);
    }

    public void free() {
        vertexBuffer = null;
        normalBuffer = null;
        texBuffer = null;
        colorBuffer = null;

        if (isVBO) {
            GL15.glDeleteBuffers(vbo);
            GL15.glDeleteBuffers(vnbo);
            GL15.glDeleteBuffers(vtbo);
            GL15.glDeleteBuffers(vcbo);
        }
    }
}
