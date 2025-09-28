package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.library.TrackModelPart;
import cam72cam.immersiverailroading.track.BuilderBase;
import cam72cam.immersiverailroading.track.BuilderBase.VecYawPitch;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.immersiverailroading.util.MathUtil;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.render.opengl.VBO;
import cam72cam.mod.resource.Identifier;
import trackapi.lib.Gauges;
import util.Matrix4;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TrackModel extends OBJModel{
    private TrackOrder order;
    //Get randomized idents' real ident
    private Map<String, Supplier<String>> randomMap;
    //Map primitive idents to OBJModel
    private final Map<String, Map<TrackModelPart, List<String>>> groupNamesMapper;
    private final String compare;
    private final double size;
    private double height;
    public final double spacing;

    public TrackModel(String condition, Identifier resource, double modelGaugeM, double spacing, boolean isSingle) throws Exception {
        super(resource, 0, Gauges.STANDARD / modelGaugeM);
        this.compare = condition.substring(0, 1);
        this.groupNamesMapper = new HashMap<>();
        if(isSingle) {
            Map<TrackModelPart, List<String>> groups = new HashMap<>();
            for(TrackModelPart part : TrackModelPart.values()){
                List<String> parts = this.groups().stream().filter(part::is).collect(Collectors.toList());
                groups.put(part, parts);
            }
            groupNamesMapper.put("single", groups);
            randomMap = Collections.singletonMap("single", () -> "single");
            this.order = new TrackOrder(Collections.singletonList("single"));
            List<String> rails = this.groups().stream()
                                     .filter(group -> TrackModelPart.RAIL_LEFT.is(group) || TrackModelPart.RAIL_RIGHT.is(group))
                                     .collect(Collectors.toList());
            this.height = maxOfGroup(rails).y;
        }
        this.size = Double.parseDouble(condition.substring(1));
        this.spacing = spacing * (Gauges.STANDARD / modelGaugeM);
    }

    public static TrackModel parse(String condition, DataBlock block, double modelGaugeM, double spacing) throws Exception{
        Map<String, Supplier<String>> mapper = new HashMap<>();
        Identifier identifier = block.getValue("model").asIdentifier();
        TrackModel model = new TrackModel(condition, identifier, modelGaugeM, spacing, false);
        Function<List<DataBlock.Value>, List<String>> toString = s -> s.stream()
                                                                       .map(DataBlock.Value::asString)
                                                                       .collect(Collectors.toList());

        double[] height = {Double.MIN_VALUE};
        List<DataBlock> groupsList = block.getBlocks("groups");
        if (groupsList == null || groupsList.isEmpty()){
            throw new IllegalArgumentException("You must have at least 1 entry in \"groups\", or you should use legacy format!");
        } else {
            groupsList.forEach(groupBlock -> {
                String ident = groupBlock.getValue("ident").asString();
                DataBlock parts = groupBlock.getBlock("parts");

                Map<TrackModelPart, List<String>> groups = Arrays.stream(TrackModelPart.values())
                                                                 .collect(Collectors.toMap(
                                                                         part -> part,
                                                                         part -> Optional.ofNullable(parts.getValues(part.name()))
                                                                                         .map(toString)
                                                                                         .orElse(Collections.emptyList())
                                                                 ));

                List<String> rails = new ArrayList<>();
                groups.entrySet().stream()
                                 .filter(entry -> entry.getKey() == TrackModelPart.RAIL_LEFT
                                 || entry.getKey() == TrackModelPart.RAIL_RIGHT)
                                 .map(Map.Entry::getValue)
                                 .forEach(rails::addAll);

                height[0] = Math.max(height[0], model.maxOfGroup(rails).y);
                model.groupNamesMapper.put(ident, groups);
                mapper.put(ident, () -> ident);
            });
        }

        if (height[0] != Double.MIN_VALUE) {
            model.height = height[0];
        } else {
            throw new IllegalArgumentException("Unable to get any rail in model groups definition!");
        }

        List<DataBlock> randomized = block.getBlocks("randomized");
            if(randomized != null){randomized.forEach(b -> {
                String ident = b.getValue("ident").asString();
                Random rand = new Random(ident.hashCode());
                Map<String, Integer> partWeights = b.getBlock("part_weights").getValueMap().entrySet()
                                                    .stream()
                                                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().asInteger()));

                int gcd = partWeights.values().stream().reduce(0, MathUtil::gcd);
                int totalWeight = partWeights.values().stream().mapToInt(i -> i / gcd).sum();

                String[] strArray = new String[totalWeight];
                int index = 0;

                for (Map.Entry<String, Integer> entry : partWeights.entrySet()) {
                    int weight = entry.getValue() / gcd;
                    for (int j = 0; j < weight; j++) {
                        strArray[index++] = entry.getKey();
                    }
                }

                mapper.put(ident, () -> strArray[rand.nextInt(totalWeight)]);
            });
        }

        //Parse order
        DataBlock orderBlock = block.getBlock("order");
        List<DataBlock.Value> orderArray = block.getValues("order");
        TrackOrder trackOrder;
        if(orderBlock != null){
            List<String> mid = toString.apply(orderBlock.getValues("mid"));
            trackOrder = new TrackOrder(mid);
            Optional.ofNullable(orderBlock.getValues("near"))
                    .ifPresent(arr -> trackOrder.setNear(arr.stream()
                                                            .map(DataBlock.Value::asString)
                                                            .collect(Collectors.toList())));
            Optional.ofNullable(orderBlock.getValues("far"))
                    .ifPresent(arr -> trackOrder.setFar(arr.stream()
                                                            .map(DataBlock.Value::asString)
                                                            .collect(Collectors.toList())));
        } else if(orderArray != null) {
            //A fallback for "order" in array format, take it as "mid"
            List<String> mid = toString.apply(block.getValues("order"));
            trackOrder = new TrackOrder(mid);
        } else {
            throw new IllegalArgumentException("Must contains \"order\" field with advanced track definition");
        }

        model.order = trackOrder;
        model.randomMap = mapper;
        return model;
    }

    public boolean canRender(double gauge) {
        switch (compare) {
            case ">": return gauge > size;
            case "<": return gauge < size;
            case "=": return gauge == size;
            default: return true;
        }
    }

    public VBO getModel(RailInfo info, List<BuilderBase.VecYawPitch> data) {
        if(info.settings.type.isTable()){
            return renderTable(info, data);
        }

        //Otherwise use generated order to build
        OBJRender.Builder builder = this.binder().builder();
        List<String> names = order.getRenderOrder(data.size());
        for (int i = 0; i < names.size(); i++) {
            String modelKey = randomMap.get(names.get(i)).get();
            Map<TrackModelPart, List<String>> groups = this.groupNamesMapper.get(modelKey);

            renderPiece(info, data.get(i), builder, groups);
        }

        return builder.build();
    }

    private VBO renderTable(RailInfo info, List<BuilderBase.VecYawPitch> data) {
        OBJRender.Builder builder = this.binder().builder();
        Map<TrackModelPart, List<String>> groupNames = this.groupNamesMapper.values().stream().findFirst().get();

        for (BuilderBase.VecYawPitch piece : data) {
            renderPiece(info, piece, builder, groupNames);
        }

        return builder.build();
    }

    private void renderPiece(RailInfo info, VecYawPitch piece,
                             OBJRender.Builder builder, Map<TrackModelPart, List<String>> groupNames) {
        Matrix4 matrix = new Matrix4();
        matrix.translate(piece.x, piece.y, piece.z);
        matrix.rotate(Math.toRadians(piece.getYaw()), 0, 1, 0);
        matrix.rotate(Math.toRadians(piece.getPitch()), 1, 0, 0);
        matrix.rotate(Math.toRadians(-90), 0, 1, 0);

        double scale = info.settings.gauge.scale();
        matrix.scale(scale, scale, scale);

        List<String> tables = groupNames.get(TrackModelPart.TABLE);

        if(piece.getParts().contains(TrackModelPart.TABLE)){
            builder.draw(tables, matrix.copy());
        }

        if (piece.getLength() != -1) {
            matrix.scale(piece.getLength() / info.settings.gauge.scale(), 1, 1);
        }

        List<String> groups = new ArrayList<>();
        if (!piece.getParts().isEmpty()) {
            groupNames.keySet().stream()
                      .filter(part -> part != TrackModelPart.TABLE)
                      .filter(part -> piece.getParts().contains(part))
                      .map(groupNames::get).forEach(groups::addAll);
        } else {
            groupNames.keySet().stream()
                      .filter(part -> part != TrackModelPart.TABLE)
                      .map(groupNames::get).forEach(groups::addAll);
        }
        builder.draw(groups, matrix);
        if(!piece.children.isEmpty()){
            for(VecYawPitch vec : piece.children){
                renderPiece(info, vec, builder, groupNames);
            }
        }
    }

    public double getHeight() {
        return height;
    }

    public static class TrackOrder{
        // All directions are Near -> Far
        protected final List<String> near = new ArrayList<>();
        protected final List<String> mid = new ArrayList<>();
        protected final List<String> far = new ArrayList<>();

        public TrackOrder(List<String> mid) {
            this.mid.addAll(parseCounts(mid));
        }

        public void setNear(List<String> near) {
            this.near.addAll(parseCounts(near));
        }

        public void setFar(List<String> far) {
            this.far.addAll(parseCounts(far));
        }

        public List<String> getRenderOrder(int length) {
            List<String> value = new ArrayList<>();

            if(length < near.size() + far.size()){
                int n = 0, f = 0;
                while (n + f < length){
                    if(n <= near.size()){
                        n++;
                    }

                    if(n + f < length && f <= far.size()){
                        f++;
                    }
                }

                for(int i = 0; i < n; i++){
                    value.add(near.get(i));
                }
                for(int i = far.size() - f; i < far.size(); i++){
                    value.add(far.get(i));
                }
            } else {
                value.addAll(near);
                value.addAll(far);
                for(int i = 0, j = near.size(); j < length - far.size(); i = (i+1) % mid.size(), j++){
                    value.add(j, mid.get(i));
                }
            }
            return value;
        }

        private static List<String> parseCounts(List<String> orig) {
            // ["Str*3"] will be ["Str","Str","Str"]
            List<String> result = new ArrayList<>();
            for(String s : orig){
                String[] str = s.split("\\*");
                result.add(str[0]);
                if(str.length == 2) {
                    int count = Integer.parseInt(str[1]);
                    for (int i = 1; i < count; i++) {
                        result.add(str[0]);
                    }
                }
            }
            return result;
        }
    }
}