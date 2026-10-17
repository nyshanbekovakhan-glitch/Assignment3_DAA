package shortestpath;
import graph.Edge;
import graph.Graph;

import java.util.PriorityQueue;
public final class Dijkstra {
    private Dijkstra() {}
    private record Entry(long dist, int vertex) {}
    public static DijkstraResult run(Graph g, int source) {
        if (g.hasNegativeWeight())
            throw new IllegalArgumentException("Dijkstra requires non-negative edge weights");
        return compute(g, source);
    }
    public static DijkstraResult runUnchecked(Graph g, int source) { return compute(g, source); }

    private static DijkstraResult compute(Graph g, int source) {
        int n = g.vertexCount();
        if (source < 0 || source >= n) throw new IllegalArgumentException("source out of range");
        DijkstraResult res = new DijkstraResult(source, n);
        long[] cmp = {0};
        PriorityQueue<Entry> pq = new PriorityQueue<>((a, b) -> { cmp[0]++; return Long.compare(a.dist(), b.dist()); });
        boolean[] done = new boolean[n];

        res.dist[source] = 0;
        pq.add(new Entry(0, source));
        res.pqPushes++;

        while (!pq.isEmpty()) {
            Entry cur = pq.poll();
            res.pqPops++;
            int u = cur.vertex();
            if (done[u]) continue;
            done[u] = true;
            for (Edge e : g.neighbors(u)) {
                res.edgeExaminations++;
                int v = e.to;
                if (done[v]) continue;
                long nd = res.dist[u] + e.weight;
                if (nd < res.dist[v]) {
                    res.dist[v] = nd;
                    res.parent[v] = u;
                    res.relaxations++;
                    pq.add(new Entry(nd, v));
                    res.pqPushes++;
                }
            }
        }
        res.comparisons = cmp[0];
        return res;
    }
}
