package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.umc.api.render.obj.OBJRender;
import cam72cam.umc.api.render.opengl.BlendMode;
import cam72cam.umc.api.render.opengl.RenderState;
import cam72cam.umc.api.util.Matrix4;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

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

    private static class Opacity {
        final List<String> opaque = new ArrayList<>();
        final List<String> transparent = new ArrayList<>();

        void add(boolean isTransparent, String group) {
            if (isTransparent) {
                transparent.add(group);
            } else {
                opaque.add(group);
            }
        }
    }

    private static final BlendMode ALPHA_BLEND = new BlendMode(BlendMode.GL_SRC_ALPHA, BlendMode.GL_ONE_MINUS_SRC_ALPHA);

    private static class GroupState {
        Matrix4 matrix = null;
        Float blockLight = null;
        Float skyLight = null;
        boolean transparent = false;

        boolean equals(GroupState other) {
            return Objects.equals(this.matrix, other.matrix) &&
                    Objects.equals(this.blockLight, other.blockLight) &&
                    Objects.equals(this.skyLight, other.skyLight) &&
                    this.transparent == other.transparent;
        }

        void copy(GroupState other) {
            this.matrix = other.matrix;
            this.blockLight = other.blockLight;
            this.skyLight = other.skyLight;
            this.transparent = other.transparent;
        }

        void reset() {
            this.matrix = null;
            this.blockLight = null;
            this.skyLight = null;
            this.transparent = false;
        }
    }

    public void render(OBJRender.Binding vbo, EntityMoveableRollingStock stock, List<ModelComponentType> available, float partialTicks) {
        // Get all groups that we can render from components that are available
        List<ModelComponent.ModelGroup> groups = new ArrayList<>();

        for (ModelComponent component : components) {
            if (available == null || available.remove(component.type)) {
                // Filter out groups that aren't currently visible
                if (groupVisibility == null) {
                    groups.addAll(component.groups);
                } else {
                    for (ModelComponent.ModelGroup g : component.groups) {
                        Boolean visible = groupVisibility.visible(stock, g.modelID);
                        if (visible == null || visible) {
                            groups.add(g);
                        }
                    }
                }
            }
        }

        // Minimize draw calls by using the same comparison as OBJRender
        groups.sort(Comparator.comparing(a -> a.modelID));

        // General matrix to apply
        Matrix4 matrix = animator != null ? animator.getMatrix(stock, partialTicks) : null;

        // Required, TODO upstream checking or optional
        LightState lighting = lighter.get(stock);
        boolean fullBright = lighting.fullBright != null && lighting.fullBright;
        boolean hasInterior = lighting.hasInterior != null && lighting.hasInterior;

        GroupState current = new GroupState();
        GroupState next = new GroupState();
        List<String> currentGroups = new ArrayList<>(groups.size());

        Consumer<RenderState> currentModifier = state -> {
            if (matrix != null) {
                state.model_view().multiply(matrix);
            }
            if (current.matrix != null) {
                state.model_view().multiply(current.matrix);
            }
            if (current.blockLight != null && current.skyLight != null) {
                state.lightmap(current.blockLight, current.skyLight);
            }
            if (current.transparent) {
                state.blend(ALPHA_BLEND).depth_mask(false);
            }
        };

        for (ModelComponent.ModelGroup group : groups) {
            if (group.transparent && !ConfigGraphics.RenderSemiTransparentParts) {
                // Don't render the group
                continue;
            }

            next.reset();

            if (groupAnimator != null) {
                next.matrix = groupAnimator.getMatrix(stock, group.modelID, partialTicks);
            }

            Float lcgValue = group.LCG != null ? stock.getControlPosition(group.LCG) : null;
            lcgValue = lcgValue == null ? null : group.linvert ? 1 - lcgValue : lcgValue;

            // TODO additional null checks around lighting fields
            if (lcgValue == null || lcgValue > 0) {
                if (fullBright && group.fullbright) {
                    next.blockLight = 1F;
                    next.skyLight = 1F;
                } else if (lighting.interiorLight != null) {
                    if (!hasInterior || group.interior) {
                        next.blockLight = lighting.interiorLight;
                        next.skyLight = lighting.skyLight;
                        if (lcgValue != null) {
                            next.blockLight *= lcgValue;
                        }
                    }
                }
            }

            next.transparent = group.transparent;

            // See if we are still part of the current render group
            if (current.equals(next)) {
                // If so, add this group to the pile
                currentGroups.add(group.modelID);
                continue;
            }

            // Flush
            if (!currentGroups.isEmpty()) {
                vbo.draw(currentGroups, currentModifier);
            }

            // Start tracking the next set of groups
            currentGroups.clear();
            currentGroups.add(group.modelID);
            current.copy(next);
        }

        // Flush
        if (!currentGroups.isEmpty()) {
            vbo.draw(currentGroups, currentModifier);
        }

        for (ModelState child : children) {
            child.render(vbo, stock, available, partialTicks);
        }
    }
}
