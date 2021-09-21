package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.mod.render.OpenGL;
import cam72cam.mod.render.obj.OBJVBO.BoundOBJVBO;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import util.Matrix4;

import java.io.Closeable;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ComponentRenderer implements Closeable {
    private static FloatBuffer fbm = null;

    private final BoundOBJVBO vbo;
    private final List<ModelComponent> buffer = new ArrayList<>();
    private final List<ModelComponentType> available;
    private final Matrix4 matrix;
    private final Float interiorLight;
    private final Float skyLight;
    private final boolean fullbright;
    private final boolean hasInterior;
    private final EntityRollingStock stock;

    public ComponentRenderer(EntityRollingStock stock, BoundOBJVBO vbo, List<ModelComponentType> available, boolean hasInterior) {
        this(stock, new Matrix4(), vbo, available, hasInterior, false, null, null);
    }

    public ComponentRenderer(EntityRollingStock stock, Matrix4 matrix, BoundOBJVBO vbo, List<ModelComponentType> available, boolean hasInterior, boolean fullbright, Float interiorLight, Float skyLight) {
        this.vbo = vbo;
        this.available = available;
        this.fullbright = fullbright;
        this.hasInterior = hasInterior;
        this.interiorLight = interiorLight;
        this.skyLight = skyLight;
        this.matrix = matrix;
        this.stock = stock;
    }

    public void render(ModelComponent component) {
        if (component != null && (available == null || available.remove(component.type))) {
            buffer.add(component);
        }
    }

    public void render(ModelComponent ... components) {
        for (ModelComponent component : components) {
            render(component);
        }
    }

    public void render(Collection<ModelComponent> components) {
        for (ModelComponent component : components) {
            render(component);
        }
    }

    public ComponentRenderer withBrightGroups(boolean fullbright) {
        return new ComponentRenderer(stock, matrix.copy(), vbo, available, hasInterior, fullbright, interiorLight, skyLight);
    }

    public ComponentRenderer withInteriorLight(EntityRollingStock stock) {
        float interiorLight = stock.getDefinition().interiorLightLevel();
        float blockLight = stock.getWorld().getBlockLightLevel(stock.getBlockPosition());
        float skyLight = stock.getWorld().getSkyLightLevel(stock.getBlockPosition());
        return blockLight < interiorLight ? new ComponentRenderer(stock, matrix.copy(), vbo, available, hasInterior, fullbright, interiorLight, skyLight) : this;
    }

    public ComponentRenderer push() {
        return new ComponentRenderer(stock, matrix.copy(), vbo, available, hasInterior, fullbright, interiorLight, skyLight);
    }

    public void translate(double x, double y, double z) {
        matrix.translate(x, y, z);
    }
    public void rotate(double degrees, double x, double y, double z) {
        matrix.rotate(Math.toRadians(degrees), x, y, z);
    }
    public void scale(double x, double y, double z) {
        matrix.scale(x, y, z);
    }

    public static final Pattern lcgPattern = Pattern.compile("_LCG_([^_]+)");
    private void draw(Collection<String> groups) {
        try (OpenGL.With mtx = OpenGL.matrix()) {
            matrix.transpose();
            if (fbm == null) {
                // Can't static init since class is loaded server side
                fbm = BufferUtils.createFloatBuffer(16);
            }
            fbm.position(0);
            fbm.put(new float[]{
                    (float) matrix.m00, (float) matrix.m01, (float) matrix.m02, (float) matrix.m03,
                    (float) matrix.m10, (float) matrix.m11, (float) matrix.m12, (float) matrix.m13,
                    (float) matrix.m20, (float) matrix.m21, (float) matrix.m22, (float) matrix.m23,
                    (float) matrix.m30, (float) matrix.m31, (float) matrix.m32, (float) matrix.m33
            });
            fbm.flip();
            OpenGL.multMatrix(fbm);
            

            if (interiorLight == null && !fullbright) {
                // Skip any sort of lighting logic
                vbo.draw(groups);
                return;
            }

            List<String> noop = Collections.EMPTY_LIST;
            List<String> dark = new ArrayList<>();
            List<String> interiorNormal = hasInterior ? new ArrayList<>() : noop;
            List<String> interiorFullbright = hasInterior && interiorLight != null && fullbright ? new ArrayList<>() : noop;
            List<String> exteriorNormal = new ArrayList<>();
            List<String> exteriorFullbright = fullbright ? new ArrayList<>() : noop;

            for (String group : groups) {
                Matcher matcher = lcgPattern.matcher(group);
                if (matcher.find()) {
                    boolean invert = group.contains("_LINVERT_") || group.startsWith("LINVERT_") || group.endsWith("_LINVERT");
                    if (stock.getControlPosition(matcher.group(1)) == (invert ? 1 : 0)) {
                        dark.add(group);
                        continue;
                    }
                }
                if (hasInterior && group.contains("INTERIOR")) {
                    (fullbright && interiorLight != null && group.contains("FULLBRIGHT") ? interiorFullbright : interiorNormal).add(group);
                } else {
                    (fullbright && group.contains("FULLBRIGHT") ? exteriorFullbright : exteriorNormal).add(group);
                }
            }

            vbo.draw(dark);

            if (!interiorFullbright.isEmpty() || !exteriorFullbright.isEmpty()) {
                try (
                        OpenGL.With light = OpenGL.shaderActive() ?
                                OpenGL.lightmap(1, 1) :
                                OpenGL.bool(GL11.GL_LIGHTING, false).and(OpenGL.lightmap(false));
                ) {
                    vbo.draw(exteriorFullbright);
                    vbo.draw(interiorFullbright);
                }
            }

            if (!interiorNormal.isEmpty()) {
                try (OpenGL.With lm = interiorLight != null ? OpenGL.lightmap(interiorLight, skyLight) : () -> {
                }) {
                    vbo.draw(interiorNormal);
                }
            }

            if (!exteriorNormal.isEmpty()) {
                try (OpenGL.With lm = interiorLight != null && !hasInterior ? OpenGL.lightmap(interiorLight, skyLight) : () -> {
                }) {
                    vbo.draw(exteriorNormal);
                }
            }
        }
    }

    @Override
    public void close() {
        if (buffer.size() == 1) {
            draw(buffer.get(0).modelIDs);
        } else if (buffer.size() > 1) {
            draw(buffer.stream().flatMap(x -> x.modelIDs.stream()).collect(Collectors.toList()));
        }
    }
}
