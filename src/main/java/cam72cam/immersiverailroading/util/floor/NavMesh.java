package cam72cam.immersiverailroading.util.floor;

import cam72cam.immersiverailroading.model.StockModel;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.util.MathUtil;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.model.obj.FaceAccessor;
import cam72cam.mod.model.obj.OBJFace;
import cam72cam.mod.model.obj.Vec2f;
import cam72cam.mod.util.Axis;

import java.util.*;

/**
 * Helper class used for tracking FLOOR meshes using BVH
 * <p>
 * Here we use the model coordinate (unscaled, -X forward) to store data
 */
public class NavMesh {
    public static final float RANGE = 0.5f;

    public final BVHNode root;
    //Edges that are only connected to 1 face, useful when checking holes
    private final List<Edge> floorBoundaryEdges;
    // Theoretically this could be much lower. IR floor meshes probably won't use the whole depth, but who knows
    private static final int MAX_DEPTH = 20;
    private static final int LEAF_SIZE = 8;

    public NavMesh(EntityRollingStockDefinition definition) {
        StockModel<?, ?> model = definition.getModel();
        List<OBJFace> floorFaces;
        if (model.floor != null) {
            floorFaces = collectFloorFaces(model);
            root = buildBVH(new ArrayList<>(floorFaces), 0);
            //Overwrite legacy definition
            Vec3d bounds = model.floor.max.subtract(model.floor.min);
            definition.passengerCompartmentLength = bounds.x/2;
            definition.passengerCompartmentWidth = bounds.z/2;
            definition.passengerCenter = model.floor.center;
        } else {
            floorFaces = legacyFloorFaces(definition);
            root = buildBVH(new ArrayList<>(floorFaces), 0);
        }

        floorBoundaryEdges = computeBoundaryEdges(floorFaces);
    }

    private List<OBJFace> collectFloorFaces(StockModel<?, ?> model) {
        FaceAccessor accessor = model.getFaceAccessor();
        List<OBJFace> floor = new ArrayList<>();
        model.floor.modelIDs.forEach(group -> {
            FaceAccessor sub = accessor.getSubByGroup(group);
            sub.forEach(a -> floor.add(a.asOBJFace()));
        });
        return floor;
    }

    private List<OBJFace> legacyFloorFaces(EntityRollingStockDefinition def) {
        Vec3d center = def.passengerCenter;
        Double length = def.passengerCompartmentLength;
        Double width = def.passengerCompartmentWidth;

        if (length == null || width == null) {
            throw new RuntimeException(String.format("Rolling stock %s needs to have either a FLOOR object or have \"length\" and \"width\" defined in the \"passenger\" section of the stocks json", def.name()));
        }
        if (center == null) {
            center = Vec3d.ZERO;
        }

        OBJFace face1 = new OBJFace();
        OBJFace face2 = new OBJFace();

        Vec2f uv = new Vec2f(0, 0);
        Vec3d normal = new Vec3d(0, 1, 0);

        Vec3d vertex1 = center.add(-length, 0, width / 2);
        Vec3d vertex2 = center.add(length, 0, width / 2);
        Vec3d vertex3 = center.add(length,  0, -width / 2);
        Vec3d vertex4 = center.add(-length, 0, -width / 2);

        face1.vertex0 = new OBJFace.Vertex(vertex1, uv);
        face1.vertex1 = new OBJFace.Vertex(vertex2, uv);
        face1.vertex2 = new OBJFace.Vertex(vertex3, uv);

        face2.vertex0 = new OBJFace.Vertex(vertex1, uv);
        face2.vertex1 = new OBJFace.Vertex(vertex3, uv);
        face2.vertex2 = new OBJFace.Vertex(vertex4, uv);

        face1.normal = normal;
        face2.normal = normal;

        return Arrays.asList(face1, face2);
    }

    public static class Edge {
        public final Vec3d start;
        public final Vec3d end;
        Edge(Vec3d start, Vec3d end) {
            this.start = start;
            this.end = end;
        }

        String key() {
            String startKey = vecKey(start);
            String endKey = vecKey(end);
            return startKey.compareTo(endKey) <= 0 ? startKey + "|" + endKey : endKey + "|" + startKey;
        }

        //Use %.4f, allowing minor errors
        private static String vecKey(Vec3d vec) {
            return String.format("%.4f,%.4f,%.4f", vec.x, vec.y, vec.z);
        }
    }

    private List<Edge> computeBoundaryEdges(List<OBJFace> triangles) {
        Map<String, Edge> edgeByKey = new HashMap<>();
        Map<String, Integer> edgeCount = new HashMap<>();

        for (OBJFace tri : triangles) {
            addEdge(tri.vertex0.pos, tri.vertex1.pos, edgeByKey, edgeCount);
            addEdge(tri.vertex1.pos, tri.vertex2.pos, edgeByKey, edgeCount);
            addEdge(tri.vertex2.pos, tri.vertex0.pos, edgeByKey, edgeCount);
        }

        List<Edge> boundary = new ArrayList<>();
        for (Map.Entry<String, Integer> e : edgeCount.entrySet()) {
            if (e.getValue() == 1) {
                boundary.add(edgeByKey.get(e.getKey()));
            }
        }
        return boundary;
    }

    private void addEdge(Vec3d a, Vec3d b, Map<String, Edge> edgeByKey, Map<String, Integer> edgeCount) {
        Edge newEdge = new Edge(a, b);
        String key = newEdge.key();
        edgeByKey.putIfAbsent(key, newEdge);
        edgeCount.merge(key, 1, Integer::sum);
    }

    public Edge closestBoundaryEdge(Vec3d point) {
        Edge closest = null;
        double closestDistSq = Double.MAX_VALUE;
        for (Edge edge : floorBoundaryEdges) {
            Vec3d onSeg = MathUtil.closestPointOnSegmentXZ(point, edge.start, edge.end);
            double distSq = point.distanceToSquared(onSeg);
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closest = edge;
            }
        }
        return closest;
    }

    public boolean isPointOnFloor(Vec3d point, double scale) {
        IBoundingBox box = IBoundingBox.from(
                point.subtract(RANGE * 0.1 * scale, RANGE * scale, RANGE * 0.1 * scale),
                point.add(RANGE * 0.1 * scale, RANGE * scale, RANGE * 0.1 * scale)
        );
        List<OBJFace> nearby = new ArrayList<>();
        queryBVH(root, box, nearby, scale);
        for (OBJFace tri : nearby) {
            if (pointInTriangleXZ(point, tri)) {
                return true;
            }
        }
        return false;
    }

    private static boolean pointInTriangleXZ(Vec3d p, OBJFace tri) {
        double d1 = signXZ(p, tri.vertex0.pos, tri.vertex1.pos);
        double d2 = signXZ(p, tri.vertex1.pos, tri.vertex2.pos);
        double d3 = signXZ(p, tri.vertex2.pos, tri.vertex0.pos);

        boolean hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0);
        boolean hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0);

        return !(hasNeg && hasPos);
    }

    private static double signXZ(Vec3d p, Vec3d a, Vec3d b) {
        return (p.x - b.x) * (a.z - b.z) - (a.x - b.x) * (p.z - b.z);
    }

    public static class BVHNode {
        IBoundingBox bounds;
        BVHNode left;
        BVHNode right;
        List<OBJFace> triangles;

        boolean isLeaf() {
            return triangles != null;
        }
    }

    public BVHNode buildBVH(List<OBJFace> triangles, int depth) {
        if (triangles.size() <= LEAF_SIZE || depth > MAX_DEPTH) {
            IBoundingBox bounds = IBoundingBox.from(Vec3i.ZERO);
            for (OBJFace face : triangles) {
                bounds = bounds.expandToFit(face.getBoundingBox());
            }
            BVHNode node = new BVHNode();
            node.bounds = bounds;
            node.triangles = triangles;
            return node;
        }

        IBoundingBox bounds = IBoundingBox.from(Vec3i.ZERO);
        for (OBJFace face : triangles) {
            bounds = bounds.expandToFit(face.getBoundingBox());
        }

        Vec3d size = bounds.max().subtract(bounds.min());
        Axis axis = (size.x > size.y && size.x > size.z) ? Axis.X : (size.y > size.z ? Axis.Y : Axis.Z);

        triangles.sort((a, b) -> Double.compare(getCentroid(a, axis), getCentroid(b, axis)));
        int mid = triangles.size() / 2;

        BVHNode node = new BVHNode();
        node.left = buildBVH(triangles.subList(0, mid), depth + 1);
        node.right = buildBVH(triangles.subList(mid, triangles.size()), depth + 1);
        node.bounds = bounds;
        return node;
    }

    public void queryBVH(BVHNode node, IBoundingBox query, List<OBJFace> result, double scale) {
        query = unscaleBoxUniform(query, scale);
        queryBVHInternal(node, query, result, scale);
    }



    public void queryBVHInternal(BVHNode node, IBoundingBox query, List<OBJFace> result, double scale) {
        if (node == null) return;
        if (!node.bounds.intersects(query)) return;

//        query = unscaleBoxUniform(query, scale);

        if (node.isLeaf()) {
            for (OBJFace tri : node.triangles) {
                if (tri.getBoundingBox().intersects(query)) {
                    result.add(tri.scale(scale));
                }
            }
        } else {
            queryBVHInternal(node.left, query, result, scale);
            queryBVHInternal(node.right, query, result, scale);
        }
    }

    private static IBoundingBox unscaleBoxUniform(IBoundingBox box, double s) {
        Vec3d a = box.min().scale(1.0 / s);
        Vec3d b = box.max().scale(1.0 / s);

        return IBoundingBox.from(
                new Vec3d(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z)),
                new Vec3d(Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z))
        );
    }

    private double getCentroid(OBJFace tri, Axis axis) {
        return (VecUtil.getByAxis(tri.vertex0.pos, axis) + VecUtil.getByAxis(tri.vertex1.pos, axis) + VecUtil.getByAxis(tri.vertex2.pos, axis)) / 3f;
    }
}