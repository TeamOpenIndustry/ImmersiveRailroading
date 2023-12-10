package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.mod.render.obj.OBJRender;
import org.apache.commons.lang3.tuple.Pair;
import util.Matrix4;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ModelState {
    private final Animator animator;
    private final GroupAnimator groupAnimator;
    private final GroupVisibility groupVisibility;
    private final Lighter lighter;
    private final List<ModelComponent> components;

    private final List<ModelState> children = new ArrayList<>();

    private ModelState(
            Animator animator,
            GroupAnimator groupAnimator,
            GroupVisibility groupVisibility,
            Lighter lighter
    ) {
        this.components = new ArrayList<>();
        this.animator = animator;
        this.groupAnimator = groupAnimator;
        this.groupVisibility = groupVisibility;
        this.lighter = lighter;
    }


    public ModelState push(Consumer<Builder> fn) {
        Builder builder = new Builder(this);
        fn.accept(builder);
        ModelState created = builder.build();
        children.add(created);
        return created;
    }

    public Matrix4 getMatrix(EntityMoveableRollingStock stock, float partialTicks) {
        return animator != null ? animator.getMatrix(stock, partialTicks) : null;
    }
    public Matrix4 getGroupMatrix(EntityMoveableRollingStock stock, String group, float partialTicks) {
        Matrix4 groupMatrix = groupAnimator != null ? groupAnimator.getMatrix(stock, group, partialTicks) : null;
        Matrix4 baseMatrix = getMatrix(stock, partialTicks);
        if (groupMatrix == null) {
            return baseMatrix;
        }
        if (baseMatrix == null) {
            return groupMatrix;
        }
        return baseMatrix.copy().multiply(groupMatrix);
    }

    public void include(ModelComponent component) {
        if (component != null) {
            this.components.add(component);
        }
    }

    public void include(Collection<ModelComponent> components) {
        this.components.addAll(components);
    }

    @FunctionalInterface
    public interface Animator {
        Matrix4 getMatrix(EntityMoveableRollingStock stock, float partialTicks);
        default Animator merge(Animator other) {
            return (EntityMoveableRollingStock stock, float partialTicks) -> {
                Matrix4 ourMatrix = this.getMatrix(stock, partialTicks);
                Matrix4 newMatrix = other.getMatrix(stock, partialTicks);
                if (ourMatrix == null) {
                    return newMatrix;
                }
                if (newMatrix == null) {
                    return ourMatrix;
                }
                return ourMatrix.copy().multiply(newMatrix);
            };
        }
    }
    @FunctionalInterface
    public interface GroupAnimator {
        Matrix4 getMatrix(EntityMoveableRollingStock stock, String group, float partialTicks);
        default GroupAnimator merge(GroupAnimator other) {
            return (stock, g, partialTicks) -> {
                Matrix4 ourMatrix = this.getMatrix(stock, g, partialTicks);
                Matrix4 newMatrix = other.getMatrix(stock, g, partialTicks);
                if (ourMatrix == null) {
                    return newMatrix;
                }
                if (newMatrix == null) {
                    return ourMatrix;
                }
                return ourMatrix.copy().multiply(newMatrix);
            };
        }
    }

    @FunctionalInterface
    public interface GroupVisibility {
        Boolean visible(EntityMoveableRollingStock stock, String group);

        default GroupVisibility merge(GroupVisibility other) {
            return (stock, group) -> {
                Boolean ourVisible = this.visible(stock, group);
                Boolean otherVisible = other.visible(stock, group);
                if (ourVisible == null) {
                    return otherVisible;
                }
                if (otherVisible == null) {
                    return ourVisible;
                }
                return ourVisible && otherVisible;// TODO || or && ??
            };
        }
    }

    @FunctionalInterface
    public interface Lighter {
        LightState get(EntityMoveableRollingStock stock);
        default Lighter merge(Lighter lighter) {
            return stock -> this.get(stock).merge(lighter.get(stock));
        }
    }

    public static class LightState {
        public static final LightState FULLBRIGHT = new LightState(null, null, true, null);
        private final Float interiorLight;
        private final Float skyLight;
        private final Boolean fullBright;
        private final Boolean hasInterior;

        public LightState(Float interiorLight, Float skyLight, Boolean fullBright, Boolean hasInterior) {
            this.interiorLight = interiorLight;
            this.skyLight = skyLight;
            this.fullBright = fullBright;
            this.hasInterior = hasInterior;
        }

        public LightState merge(LightState other) {
            return new LightState(
                    other.interiorLight != null ? other.interiorLight : this.interiorLight,
                    other.skyLight != null ? other.skyLight : this.skyLight,
                    other.fullBright != null ? other.fullBright : this.fullBright,
                    other.hasInterior != null ? other.hasInterior : this.hasInterior
            );
        }
    }

    public static ModelState construct(Consumer<Builder> settings) {
        Builder builder = new Builder();
        settings.accept(builder);
        return builder.build();
    }


    public static class Builder {
        public static Consumer<Builder> FULLBRIGHT = builder -> builder.add((Lighter) stock -> LightState.FULLBRIGHT);
        private Animator animator;
        private GroupAnimator groupAnimator;
        private GroupVisibility groupVisibility;
        private Lighter lighter;

        private Builder() {
            this.animator = null;
            this.groupAnimator = null;
            this.groupVisibility = null;
            this.lighter = null;
        }

        private Builder(ModelState parent) {
            this.animator = parent.animator;
            this.groupAnimator = parent.groupAnimator;
            this.groupVisibility = parent.groupVisibility;
            this.lighter = parent.lighter;
        }

        public Builder add(Animator animator) {
            this.animator = this.animator != null ? this.animator.merge(animator) : animator;
            return this;
        }
        public Builder add(GroupAnimator groupAnimator) {
            this.groupAnimator = this.groupAnimator != null ? this.groupAnimator.merge(groupAnimator) : groupAnimator;
            return this;
        }
        public Builder add(GroupVisibility groupVisibility) {
            this.groupVisibility = this.groupVisibility != null ? this.groupVisibility.merge(groupVisibility) : groupVisibility;
            return this;
        }
        public Builder add(Lighter lighter) {
            this.lighter = this.lighter != null ? this.lighter.merge(lighter) : lighter;
            return this;
        }

        private ModelState build() {
            return new ModelState(
                    animator,
                    groupAnimator,
                    groupVisibility,
                    lighter
            );
        }
   }

    // TODO Lighting rework...
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

    // TODO check performance impact of streams
    public void render(OBJRender.Binding vbo, EntityMoveableRollingStock stock, List<ModelComponentType> available, float partialTicks) {
        // Get all groups that we can render from components that are available
        List<String> groups = new ArrayList<>();
        for (ModelComponent component : components) {
            if (available == null) {
                groups.addAll(component.modelIDs);
            } else if (available.contains(component.type)) {
                available.remove(component.type);
                groups.addAll(component.modelIDs);
            }
        }

        // Filter out groups that aren't currently visible
        if (groupVisibility != null) {
            groups = groups.stream().filter(group -> {
                Boolean visible = groupVisibility.visible(stock, group);
                return visible == null || visible;
            }).collect(Collectors.toList());
        }

        Matrix4 matrix = animator != null ? animator.getMatrix(stock, partialTicks) : null;

        Map<String, Matrix4> animatedGroups = new HashMap<>();
        if (groupAnimator != null) {
            for (String group : groups) {
                Matrix4 m = groupAnimator.getMatrix(stock, group, partialTicks);
                if (m != null) {
                    animatedGroups.put(group, m);
                }
            }
        }

        // Required, TODO upstream checking or optional
        LightState lighting = lighter.get(stock);
        boolean fullBright = lighting.fullBright != null && lighting.fullBright;
        boolean hasInterior = lighting.hasInterior != null && lighting.hasInterior;


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

            // TODO additional null checks around lighting fields
            if (lcgValue == null || lcgValue > 0) {
                if (fullBright && fullbrightGroup) {
                    key = Pair.of(1f, 1f);
                } else if (lighting.interiorLight != null) {
                    if (!hasInterior || interiorGroup) {
                        if (lcgValue != null) {
                            key = Pair.of(lighting.interiorLight * lcgValue, lighting.skyLight);
                        } else {
                            key = Pair.of(lighting.interiorLight, lighting.skyLight);
                        }
                    }
                }
            }

            levels.computeIfAbsent(key, p -> new ArrayList<>()).add(group);
        }

        levels.forEach((level, litGroups) -> {
            List<String> animated = animatedGroups.isEmpty() ? Collections.emptyList() :
                    litGroups.stream().filter(g -> animatedGroups.containsKey(g)).collect(Collectors.toList());
            List<String> notAnimated = animatedGroups.isEmpty() ? litGroups :
                    litGroups.stream().filter(g -> !animatedGroups.containsKey(g)).collect(Collectors.toList());

            if (!notAnimated.isEmpty()) {
                vbo.draw(notAnimated, state -> {
                    if (matrix != null) {
                        state.model_view().multiply(matrix);
                    }
                    if (level != null) {
                        state.lightmap(level.getKey(), level.getValue());
                    }
                });
            }
            if (!animated.isEmpty()) {
                animated.forEach(group -> {
                    vbo.draw(Collections.singletonList(group), state -> {
                        if (matrix != null) {
                            state.model_view().multiply(matrix);
                        }
                        state.model_view().multiply(animatedGroups.get(group));
                        if (level != null) {
                            state.lightmap(level.getKey(), level.getValue());
                        }
                    });
                });
            }
        });

        for (ModelState child : children) {
            child.render(vbo, stock, available, partialTicks);
        }
    }
}
