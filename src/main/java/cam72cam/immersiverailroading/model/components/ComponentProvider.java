package cam72cam.immersiverailroading.model.components;

import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ModelComponentType.ModelPosition;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.model.obj.OBJModel;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ComponentProvider {
    public final OBJModel model;
    private final Set<String> groups;
    public Map<String, DataBlock> widgetConfig;
    private final List<ModelComponent> components;
    public double internal_model_scale;

    public ComponentProvider(OBJModel model, double internal_model_scale, Map<String, DataBlock> widgetConfig) {
        this.model = model;
        this.groups = new HashSet<>(model.groups());
        this.widgetConfig = widgetConfig;
        this.components = new ArrayList<>();
        this.internal_model_scale = internal_model_scale;
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

    public ModelComponent parse(ModelComponentType type, ModelPosition pos) {
        Set<String> ids = modelIDs(type.regex.replace("#POS#", pos.toString()).replace("#SIDE#", pos.toString()));
        if (!ids.isEmpty()) {
            ModelComponent component = new ModelComponent(type, pos, null, model, ids);
            this.components.add(component);
            return component;
        }
        return null;
    }

    public List<ModelComponent> parse(ModelPosition pos, ModelComponentType... types) {
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

    public List<ModelComponent> parseAll(ModelComponentType type, ModelPosition pos) {
        String re = type.regex;
        re = re.replace("#POS#", pos.toString()).replace("#SIDE#", pos.toString());
        if (!re.equals(type.regex)) {
            // POS or SIDE found
            re = re.replace("#ID#", "([\\d]+)");
        } else {
            // Hack pos into #ID# slot
            re = re.replace("#ID#", pos + "_([\\d]+)");
        }
        return modelIDMap(re).entrySet().stream().map(e -> {
            ModelComponent component = new ModelComponent(type, pos, Integer.parseInt(e.getKey()), model, e.getValue());
            this.components.add(component);
            return component;
        }).collect(Collectors.toList());
    }

    public List<ModelComponent> components() {
        return components;
    }

}
