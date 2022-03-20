package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.mod.render.obj.OBJRender;
import util.Matrix4;

import java.io.Closeable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ComponentRenderer implements Closeable {
    private final OBJRender.Binding vbo;
    private final List<ModelComponent> buffer = new ArrayList<>();
    private final List<ModelComponentType> available;
    private final Matrix4 matrix;
    private final Float interiorLight;
    private final Float skyLight;
    private final boolean fullbright;
    private final boolean hasInterior;
    private final EntityRollingStock stock;

    public ComponentRenderer(EntityRollingStock stock, OBJRender.Binding vbo, List<ModelComponentType> available, boolean hasInterior) {
        this(stock, new Matrix4(), vbo, available, hasInterior, false, null, null);
    }

    public ComponentRenderer(EntityRollingStock stock, Matrix4 matrix, OBJRender.Binding vbo, List<ModelComponentType> available, boolean hasInterior, boolean fullbright, Float interiorLight, Float skyLight) {
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

    public void mult(Matrix4 transform) {
        this.matrix.multiply(transform);
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
    private static final Map<String, String> lcgCache = new HashMap<>();
    private static final Map<String, Boolean> linvertCache = new HashMap<>();

    private void draw(Collection<String> groups) {

        if (interiorLight == null && !fullbright) {
            // Skip any sort of lighting logic
            vbo.draw(groups, s -> s.model_view().multiply(matrix));
            return;
        }

        List<String> noop = Collections.emptyList();
        List<String> dark = new ArrayList<>();
        List<String> exteriorNormal = new ArrayList<>();
        List<String> interiorNormal = hasInterior ? new ArrayList<>() : noop;
        List<String> fullBright = fullbright ? new ArrayList<>() : noop;

        for (String group : groups) {
            if (!lcgCache.containsKey(group)) {
                Matcher matcher = lcgPattern.matcher(group);
                if (matcher.find()) {
                    lcgCache.put(group, matcher.group(1));
                } else {
                    lcgCache.put(group, null);
                }
            }
            String lcg = lcgCache.get(group);
            if (lcg != null) {
                Boolean invert = linvertCache.getOrDefault(group, null);
                if (invert == null) {
                    invert = group.contains("_LINVERT_") || group.startsWith("LINVERT_") || group.endsWith("_LINVERT");
                    linvertCache.put(group, invert);
                }
                if (stock.getControlPosition(lcg) == (invert ? 1 : 0)) {
                    dark.add(group);
                    continue;
                }
            }
            if (hasInterior && group.contains("INTERIOR")) {
                (fullbright && interiorLight != null && group.contains("FULLBRIGHT") ? fullBright : interiorNormal).add(group);
            } else {
                (fullbright && group.contains("FULLBRIGHT") ? fullBright : exteriorNormal).add(group);
            }
        }

        if (!dark.isEmpty()) {
            vbo.draw(dark, state -> state.model_view().multiply(matrix));
        }

        if (!fullBright.isEmpty()) {
            vbo.draw(fullBright, state -> {
                state.model_view().multiply(matrix);
                state.lightmap(1, 1).lighting(false);
            });
        }

        if (!interiorNormal.isEmpty()) {
            vbo.draw(interiorNormal, state -> {
                state.model_view().multiply(matrix);
                if (interiorLight != null) {
                    state.lightmap(interiorLight, skyLight);
                }
            });
        }

        if (!exteriorNormal.isEmpty()) {
            vbo.draw(exteriorNormal, state -> {
                state.model_view().multiply(matrix);
                if (interiorLight != null && !hasInterior) {
                    state.lightmap(interiorLight, skyLight);
                }
            });
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
