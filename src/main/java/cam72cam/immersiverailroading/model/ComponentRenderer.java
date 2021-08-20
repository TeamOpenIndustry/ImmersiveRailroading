package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.mod.render.OpenGL;
import cam72cam.mod.render.obj.OBJVBO.BoundOBJVBO;
import org.lwjgl.opengl.GL11;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ComponentRenderer implements Closeable {
    private final BoundOBJVBO vbo;
    private final List<ModelComponent> buffer = new ArrayList<>();
    private final List<ModelComponentType> available;
    private final OpenGL.With matrix;
    private final Float interiorLight;
    private final Float skyLight;
    private final boolean fullbright;
    private final boolean useInteriorFallback;

    public ComponentRenderer(BoundOBJVBO vbo, List<ModelComponentType> available, boolean useInteriorFallback) {
        this(vbo, available, false, useInteriorFallback, false, null, null);
    }

    public ComponentRenderer(BoundOBJVBO vbo, List<ModelComponentType> available, boolean newMatrix, boolean useInteriorFallback, boolean fullbright, Float interiorLight, Float skyLight) {
        this.vbo = vbo;
        this.available = available;
        this.fullbright = fullbright;
        this.useInteriorFallback = useInteriorFallback;
        this.interiorLight = interiorLight;
        this.skyLight = skyLight;
        matrix = newMatrix ? OpenGL.matrix() : () -> {};
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
        return new ComponentRenderer(vbo, available, false, useInteriorFallback, fullbright, interiorLight, skyLight);
    }

    public ComponentRenderer withInteriorLight(EntityRollingStock stock) {
        float interiorLight = 6 / 15f;
        float blockLight = stock.getWorld().getBlockLightLevel(stock.getBlockPosition());
        float skyLight = stock.getWorld().getSkyLightLevel(stock.getBlockPosition());
        return blockLight < interiorLight ? new ComponentRenderer(vbo, available, false, useInteriorFallback, fullbright, interiorLight, skyLight) : this;
    }

    public ComponentRenderer push() {
        return new ComponentRenderer(vbo, available, true, useInteriorFallback, fullbright, interiorLight, skyLight);
    }

    private void draw(Collection<String> groups, boolean fullbright) {
        if (fullbright) {
            // Is partitioning faster?
            List<String> std = groups.stream().filter(x -> !x.contains("FULLBRIGHT")).collect(Collectors.toList());
            List<String> bright = groups.stream().filter(x -> x.contains("FULLBRIGHT")).collect(Collectors.toList());
            if (!std.isEmpty()) {
                if (interiorLight != null) {
                    List<String> exterior = groups.stream().filter(x -> !x.contains("INTERIOR")).collect(Collectors.toList());
                    List<String> interior = groups.stream().filter(x -> x.contains("INTERIOR")).collect(Collectors.toList());
                    if (!interior.isEmpty()) {
                        try (OpenGL.With lm = OpenGL.lightmap(interiorLight, skyLight)) {
                            vbo.draw(interior);
                        }
                    }
                    if (!exterior.isEmpty()) {
                        try (OpenGL.With lm = interior.isEmpty() && useInteriorFallback ? OpenGL.lightmap(interiorLight, skyLight) : () -> {}) {
                            vbo.draw(exterior);
                        }
                    }
                } else {
                    vbo.draw(std);
                }
            }
            if (!bright.isEmpty()) {
                try (
                        OpenGL.With light = OpenGL.shaderActive() ?
                                OpenGL.lightmap(1, 1) :
                                OpenGL.bool(GL11.GL_LIGHTING, false).and(OpenGL.lightmap(false));
                ) {
                    vbo.draw(bright);
                }
            }
        } else {
            vbo.draw(groups);
        }
    }

    @Override
    public void close() {
        if (buffer.size() == 1) {
            draw(buffer.get(0).modelIDs, fullbright);
        } else if (buffer.size() > 1) {
            draw(buffer.stream().flatMap(x -> x.modelIDs.stream()).collect(Collectors.toList()), fullbright);
        }
        matrix.restore();
    }
}
