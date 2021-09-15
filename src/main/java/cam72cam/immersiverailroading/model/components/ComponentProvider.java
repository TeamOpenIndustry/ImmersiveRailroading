package cam72cam.immersiverailroading.model.components;

import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.mod.model.obj.OBJModel;

import java.util.*;
import java.util.regex.Matcher;
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
        Pattern regex = Pattern.compile(pattern);
        Set<String> modelIDs = groups.stream().filter(group -> regex.matcher(group).matches()).collect(Collectors.toSet());
        groups.removeAll(modelIDs);

        return modelIDs;
    }

    private Map<String, Set<String>> modelIDMap(String pattern) {
        Pattern rgx = Pattern.compile(pattern);
        Map<String, Set<String>> modelIDs = groups.stream()
                .map(rgx::matcher)
                .filter(Matcher::matches)
                .collect(
                        Collectors.groupingBy(
                                m -> m.group(m.groupCount()),
                                Collectors.mapping(m -> m.group(0), Collectors.toSet())
                        )
                );
        modelIDs.forEach((k, v) -> groups.removeAll(v));
        return modelIDs;
    }

    public ModelComponent parse(ModelComponentType type) {
        Set<String> ids = modelIDs(type.regex);
        if (!ids.isEmpty()) {
            ModelComponent component = new ModelComponent(type, null, null, model, ids);
            this.components.add(component);
            return component;
        }
        return null;
    }

    public List<ModelComponent> parse(ModelComponentType... types) {
        return Arrays.stream(types).map(this::parse).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public ModelComponent parse(ModelComponentType type, String pos) {
        Set<String> ids = modelIDs(type.regex.replace("#POS#", pos).replace("#SIDE#", pos));
        if (!ids.isEmpty()) {
            ModelComponent component = new ModelComponent(type, pos, null, model, ids);
            this.components.add(component);
            return component;
        }
        return null;
    }

    public List<ModelComponent> parse(String pos, ModelComponentType... types) {
        return Arrays.stream(types).map(type -> parse(type, pos)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<ModelComponent> parseAll(ModelComponentType type) {
        return modelIDMap(
                type.regex.replace("#ID#", "([\\d]+)")
        ).entrySet().stream().map(e -> {
            ModelComponent component = new ModelComponent(type, null, Integer.parseInt(e.getKey()), model, e.getValue());
            this.components.add(component);
            return component;
        }).collect(Collectors.toList());
    }

    public List<ModelComponent> parseAll(ModelComponentType... types) {
        return Arrays.stream(types).flatMap(type -> parseAll(type).stream()).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<ModelComponent> parseAll(ModelComponentType type, String pos) {
        return modelIDMap(
                type.regex
                        .replace("#ID#", "([\\d]+)")
                        .replace("#POS#", pos)
                        .replace("#SIDE#", pos)
        ).entrySet().stream().map(e -> {
            ModelComponent component = new ModelComponent(type, pos, Integer.parseInt(e.getKey()), model, e.getValue());
            this.components.add(component);
            return component;
        }).collect(Collectors.toList());
    }

    public List<ModelComponent> components() {
        return components;
    }
}
