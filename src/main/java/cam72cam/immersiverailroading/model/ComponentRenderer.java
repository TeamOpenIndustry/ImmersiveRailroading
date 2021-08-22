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
import java.util.Collections;
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
    private final boolean hasInterior;

    public ComponentRenderer(BoundOBJVBO vbo, List<ModelComponentType> available, boolean hasInterior) {
        this(vbo, available, false, hasInterior, false, null, null);
    }

    public ComponentRenderer(BoundOBJVBO vbo, List<ModelComponentType> available, boolean newMatrix, boolean hasInterior, boolean fullbright, Float interiorLight, Float skyLight) {
        this.vbo = vbo;
        this.available = available;
        this.fullbright = fullbright;
        this.hasInterior = hasInterior;
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
        return new ComponentRenderer(vbo, available, false, hasInterior, fullbright, interiorLight, skyLight);
    }

    public ComponentRenderer withInteriorLight(EntityRollingStock stock) {
        float interiorLight = 6 / 15f;
        float blockLight = stock.getWorld().getBlockLightLevel(stock.getBlockPosition());
        float skyLight = stock.getWorld().getSkyLightLevel(stock.getBlockPosition());
        return blockLight < interiorLight ? new ComponentRenderer(vbo, available, false, hasInterior, fullbright, interiorLight, skyLight) : this;
    }

    public ComponentRenderer push() {
        return new ComponentRenderer(vbo, available, true, hasInterior, fullbright, interiorLight, skyLight);
    }

    private void draw(Collection<String> groups) {
        if (interiorLight == null && !fullbright) {
            // Skip any sort of lighting logic
            vbo.draw(groups);
            return;
        }


        List<String> noop = Collections.EMPTY_LIST;
        List<String> interiorNormal = hasInterior ? new ArrayList<>() : noop;
        List<String> interiorFullbright = hasInterior && interiorLight != null && fullbright ? new ArrayList<>() : noop;
        List<String> exteriorNormal = new ArrayList<>();
        List<String> exteriorFullbright = fullbright ? new ArrayList<>() : noop;

        for (String group : groups) {
            if (hasInterior && group.contains("INTERIOR")) {
                (fullbright && interiorLight != null && group.contains("FULLBRIGHT") ? interiorFullbright : interiorNormal).add(group);
            } else {
                (fullbright && group.contains("FULLBRIGHT") ? exteriorFullbright : exteriorNormal).add(group);
            }
        }

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
            try (OpenGL.With lm = interiorLight != null ? OpenGL.lightmap(interiorLight, skyLight) : () -> {}) {
                vbo.draw(interiorNormal);
            }
        }

        if (!exteriorNormal.isEmpty()) {
            try (OpenGL.With lm = interiorLight != null && interiorNormal.isEmpty() ? OpenGL.lightmap(interiorLight, skyLight) : () -> {}) {
                vbo.draw(exteriorNormal);
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
        matrix.restore();
    }
}
