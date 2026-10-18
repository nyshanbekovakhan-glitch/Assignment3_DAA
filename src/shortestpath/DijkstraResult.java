package shortestpath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DijkstraResult {
    public static final long INF = Long.MAX_VALUE;
    public final int source;
    public final long[] dist;
    public final int[] parent;
    public long edgeExaminations;
    public long relaxations;
    public long pqPushes;
    public long pqPops;
    public long comparisons;

    DijkstraResult(int source, int n) {
        this.source = source;
        this.dist = new long[n];
        this.parent = new int[n];
        java.util.Arrays.fill(dist, INF);
        java.util.Arrays.fill(parent, -1);
    }

    public boolean isReachable(int v) { return dist[v] != INF; }

    public int reachableCount() {
        int c = 0;
        for (long d : dist) if (d != INF) c++;
        return c;
    }

    public List<Integer> pathTo(int target) {
        if (!isReachable(target)) return Collections.emptyList();
        List<Integer> path = new ArrayList<>();
        for (int v = target; v != -1; v = parent[v]) path.add(v);
        Collections.reverse(path);
        return path;
    }
}
