package cam72cam.immersiverailroading.track.graph;

// todo 暂时采用单向边，如果有需要可以考虑单边加方向标记
public class TrackEdge {
    public final TrackNodeId from;
    public final TrackNodeId to;
    public final EdgeType edgeType;

    public TrackEdge(TrackNodeId from, TrackNodeId to, EdgeType edgeType) {
        this.from = from;
        this.to = to;
        this.edgeType = edgeType;
    }

    public enum EdgeType {
        NORMAL,  // single内部相邻点
        GAP      // 连接2个single的节点
    }

    @Override
    public int hashCode() {
        int h = from.hashCode();
        h = 31 * h + to.hashCode();
        h = 31 * h + edgeType.hashCode();
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TrackEdge)) return false;
        TrackEdge o = (TrackEdge) obj;
        return from.equals(o.from) && to.equals(o.to) && edgeType == o.edgeType;
    }
}