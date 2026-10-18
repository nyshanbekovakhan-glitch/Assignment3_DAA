package mst;

import dsu.DSU;
import graph.Edge;
import graph.Graph;

import java.util.ArrayList;
import java.util.List;

public final class Kruskal {
    private Kruskal() {}

    public static MSTResult run(Graph g) { return run(g, true, true); }

    public static MSTResult run(Graph g, boolean pathCompression, boolean unionByRank) {
        if (g.isDirected()) throw new IllegalArgumentException("MST needs an undirected graph");
        int n = g.vertexCount();
        MSTResult res = new MSTResult();

        List<Edge> sorted = new ArrayList<>(g.edges());
        long[] cmp = {0};
        sorted.sort((a, b) -> { cmp[0]++; return Integer.compare(a.weight, b.weight); });
        res.comparisons = cmp[0];

        DSU dsu = new DSU(n, pathCompression, unionByRank);
        for (Edge e : sorted) {
            if (n > 0 && res.edges.size() == n - 1) break;
            res.edgeExaminations++;
            if (dsu.union(e.from, e.to)) {
                res.edges.add(e);
                res.totalWeight += e.weight;
            }
        }
        res.components = dsu.components();
        res.dsuFinds = dsu.findCalls;
        res.dsuUnions = dsu.unionCalls;
        res.dsuParentSteps = dsu.parentSteps;
        return res;
    }
}
