package cam72cam.immersiverailroading.model.components;

import cam72cam.immersiverailroading.library.RenderComponentType;
import cam72cam.mod.model.obj.OBJModel;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ComponentProvider {
    private final OBJModel model;
    private final Set<String> groups;
    private final List<ModelComponent> components;

    public ComponentProvider(OBJModel model) {
        this.model = model;
        this.groups = new HashSet<>(model.groups());
        this.components = new ArrayList<>();
    }

    private Set<String> modelIDs(String pattern) {
        Set<String> modelIDs;

        Pattern regex = Pattern.compile(pattern);
        modelIDs = groups.stream().filter(group -> regex.matcher(group).matches()).collect(Collectors.toSet());
        groups.removeAll(modelIDs);

        return modelIDs;
    }

    public ModelComponent parse(RenderComponentType type) {
        Set<String> ids = modelIDs(type.regex);
        if (!ids.isEmpty()) {
            ModelComponent component = new ModelComponent(type, null, null, model, ids);
            this.components.add(component);
            return component;
        }
        return null;
    }

    public List<ModelComponent> parse(RenderComponentType... types) {
        return Arrays.stream(types).map(this::parse).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public ModelComponent parse(RenderComponentType type, String pos) {
        Set<String> ids = modelIDs(type.regex.replace("#POS#", pos).replace("#SIDE#", pos));
        if (!ids.isEmpty()) {
            ModelComponent component = new ModelComponent(type, pos, null, model, ids);
            this.components.add(component);
            return component;
        }
        return null;
    }

    public List<ModelComponent> parse(String pos, RenderComponentType... types) {
        return Arrays.stream(types).map(type -> parse(type, pos)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<ModelComponent> parseAll(RenderComponentType type) {
        List<ModelComponent> components = new ArrayList<>();
        for (int i = 100; i >= 0; i--) {
            Set<String> ids = modelIDs(type.regex.replace("#ID#", i + ""));
            if (!ids.isEmpty()) {
                ModelComponent component = new ModelComponent(type, null, i, model, ids);
                this.components.add(component);
                components.add(component);
            }
        }
        return components;
    }

    public List<ModelComponent> parseAll(RenderComponentType... types) {
        return Arrays.stream(types).flatMap(type -> parseAll(type).stream()).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<ModelComponent> parseAll(RenderComponentType type, String pos) {
        List<ModelComponent> components = new ArrayList<>();
        for (int i = 100; i >= 0; i--) {
            Set<String> ids = modelIDs(type.regex.replace("#ID#", i + "").replace("#POS#", pos).replace("#SIDE#", pos));
            if (!ids.isEmpty()) {
                ModelComponent component = new ModelComponent(type, pos, i, model, ids);
                this.components.add(component);
                components.add(component);
            }
        }
        return components;
    }

    public List<ModelComponent> components() {
        return components;
    }
}
