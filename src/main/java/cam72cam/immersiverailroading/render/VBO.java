package cam72cam.immersiverailroading.render;

import cam72cam.immersiverailroading.proxy.ClientProxy;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.util.List;

public class VBO extends VBA {
    private int vbo = -1;
    private int vnbo = -1;
    private int vtbo = -1;
    private int vcbo = -1;

    public VBO(int size) {
        super(size);
    }
    public VBO(List<VBA> subVBAs) {
        super(subVBAs);
    }
    /*
    public void draw() {
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
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, size * 3);

        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
        if (has_vn) {
            GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
        }

        // Reset draw color (IMPORTANT)
        GL11.glColor4f(1, 1, 1, 1);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, prev);
    }*/
}
