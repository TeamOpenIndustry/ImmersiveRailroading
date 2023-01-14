package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.mod.render.obj.OBJRender;
import org.apache.commons.lang3.tuple.Pair;
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

    private final List<Animator> animators;

    public ComponentRenderer(EntityRollingStock stock, OBJRender.Binding vbo, List<ModelComponentType> available, boolean hasInterior, List<Animator> animators) {
        this(stock, new Matrix4(), vbo, available, hasInterior, false, null, null, animators);
    }

    public ComponentRenderer(EntityRollingStock stock, Matrix4 matrix, OBJRender.Binding vbo, List<ModelComponentType> available, boolean hasInterior, boolean fullbright, Float interiorLight, Float skyLight, List<Animator> animators) {
        this.vbo = vbo;
        this.available = available;
        this.fullbright = fullbright;
        this.hasInterior = hasInterior;
        this.interiorLight = interiorLight;
        this.skyLight = skyLight;
        this.matrix = matrix;
        this.stock = stock;
        this.animators = animators;
    }

    @FunctionalInterface
    public interface Animator {
        Matrix4 getMatrix(String group);
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
        return new ComponentRenderer(stock, matrix.copy(), vbo, available, hasInterior, fullbright, interiorLight, skyLight, animators);
    }

    public ComponentRenderer withInteriorLight(EntityRollingStock stock) {
        float interiorLight = stock.getDefinition().interiorLightLevel();
        float blockLight = stock.getWorld().getBlockLightLevel(stock.getBlockPosition());
        float skyLight = stock.getWorld().getSkyLightLevel(stock.getBlockPosition());
        return blockLight < interiorLight ? new ComponentRenderer(stock, matrix.copy(), vbo, available, hasInterior, fullbright, interiorLight, skyLight, animators) : this;
    }

    public ComponentRenderer push() {
        return new ComponentRenderer(stock, matrix.copy(), vbo, available, hasInterior, fullbright, interiorLight, skyLight, animators);
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
    private static final Map<String, Boolean> interiorCache = new HashMap<>();
    private static final Map<String, Boolean> fullbrightCache = new HashMap<>();

    private boolean hasGroupFlag(String group, String filter) {
        for (String x : group.split("_")) {
            if (x.equals(filter)) {
                return true;
            }
        }
        return false;
    }

    private void draw(Collection<String> groups) {
        Map<String, Matrix4> animatedGroups = new HashMap<>();
        for (Animator animator : animators) {
            for (String group : groups) {
                Matrix4 m = animator.getMatrix(group);
                if (m != null) {
                    if (!animatedGroups.containsKey(group)) {
                        animatedGroups.put(group, matrix.copy());
                    }
                    animatedGroups.get(group).multiply(m);
                }
            }
        }

        if (interiorLight == null && !fullbright) {
            // Skip any sort of lighting logic
            if (animatedGroups.isEmpty()) {
                vbo.draw(groups, s -> s.model_view().multiply(matrix));
            } else {
                List<String> animated = groups.stream().filter(g -> animatedGroups.containsKey(g)).collect(Collectors.toList());
                List<String> notAnimated = groups.stream().filter(g -> !animatedGroups.containsKey(g)).collect(Collectors.toList());
                if (!notAnimated.isEmpty()) {
                    vbo.draw(notAnimated, state -> state.model_view().multiply(matrix));
                }
                if (!animated.isEmpty()) {
                    animated.forEach(group -> {
                        vbo.draw(Collections.singletonList(group), state -> state.model_view().multiply(animatedGroups.getOrDefault(group, matrix)));
                    });
                }
            }
            return;
        }

        Map<Pair<Float, Float>, List<String>> levels = new HashMap<>();
        for (String group : groups) {
            if (!lcgCache.containsKey(group)) {
                Matcher matcher = lcgPattern.matcher(group);
                lcgCache.put(group, matcher.find() ? matcher.group(1) : null);
            }
            String lcg = lcgCache.get(group);

            boolean invertGroup = linvertCache.computeIfAbsent(group, g -> hasGroupFlag(g, "LINVERT"));
            boolean interiorGroup = interiorCache.computeIfAbsent(group, g -> hasGroupFlag(g, "INTERIOR"));
            boolean fullbrightGroup = fullbrightCache.computeIfAbsent(group, g -> hasGroupFlag(g, "FULLBRIGHT"));

            Float lcgValue = lcg != null ? stock.getControlPosition(lcg) : null;
            lcgValue = lcgValue == null ? null : invertGroup ? 1 - lcgValue : lcgValue;
            Pair<Float, Float> key = null;

            if (lcgValue == null || lcgValue > 0) {
                if (fullbright && fullbrightGroup) {
                    key = Pair.of(1f, 1f);
                } else if (interiorLight != null) {
                    if (!hasInterior || interiorGroup) {
                        if (lcgValue != null) {
                            key = Pair.of(interiorLight * lcgValue, skyLight);
                        } else {
                            key = Pair.of(interiorLight, skyLight);
                        }
                    }
                }
            }

            levels.computeIfAbsent(key, p -> new ArrayList<>()).add(group);
        }


        if (animatedGroups.isEmpty()) {
            levels.forEach((level, litGroups) -> {
                vbo.draw(litGroups, state -> {
                    state.model_view().multiply(matrix);
                    if (level != null) {
                        state.lightmap(level.getKey(), level.getValue());
                    }
                });
            });
        } else {
            levels.forEach((level, litGroups) -> {
                List<String> animated = litGroups.stream().filter(g -> animatedGroups.containsKey(g)).collect(Collectors.toList());
                List<String> notAnimated = litGroups.stream().filter(g -> !animatedGroups.containsKey(g)).collect(Collectors.toList());
                if (!notAnimated.isEmpty()) {
                    vbo.draw(notAnimated, state -> {
                        state.model_view().multiply(matrix);
                        if (level != null) {
                            state.lightmap(level.getKey(), level.getValue());
                        }
                    });
                }
                if (!animated.isEmpty()) {
                    animated.forEach(group -> {
                        vbo.draw(Collections.singletonList(group), state -> {
                            state.model_view().multiply(animatedGroups.getOrDefault(group, matrix));
                            if (level != null) {
                                state.lightmap(level.getKey(), level.getValue());
                            }
                        });
                    });
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
