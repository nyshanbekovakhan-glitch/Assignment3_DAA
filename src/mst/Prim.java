package mst;
import graph.Edge;
import graph.Graph;

import java.util.PriorityQueue;
public final class Prim {
    private Prim() {}

    public static MSTResult run(Graph g) {
        if (g.isDirected()) throw new IllegalArgumentException("MST needs an undirected graph");
        int n = g.vertexCount();
        MSTResult res = new MSTResult();
        long[] cmp = {0};
        PriorityQueue<Edge> pq = new PriorityQueue<>((a, b) -> { cmp[0]++; return Integer.compare(a.weight, b.weight); });
        boolean[] inTree = new boolean[n];

        for (int start = 0; start < n; start++) {
            if (inTree[start]) continue;
            res.components++;
            inTree[start] = true;
            expand(g, start, inTree, pq, res);
            while (!pq.isEmpty() && res.edges.size() < n - 1) {
                Edge e = pq.poll();
                res.pqPops++;
                if (inTree[e.to]) continue;
                inTree[e.to] = true;
                res.edges.add(e);
                res.totalWeight += e.weight;
                expand(g, e.to, inTree, pq, res);
            }
            pq.clear();
        }
        res.comparisons = cmp[0];
        return res;
    }
    private static void expand(Graph g, int v, boolean[] inTree, PriorityQueue<Edge> pq, MSTResult res) {
        for (Edge e : g.neighbors(v)) {
            res.edgeExaminations++;
            if (!inTree[e.to]) {
                pq.add(e);
                res.pqPushes++;
            }
        }
    }
}
