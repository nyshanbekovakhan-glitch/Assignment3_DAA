package connectivity;

import graph.Edge;
import graph.Graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class TopologicalSort {
    private TopologicalSort() {}

    public static final class Result {
        public final List<Integer> order = new ArrayList<>();
        public boolean isDag;

        public int unorderedVertices;
        public long edgeExaminations;
        public long vertexPops;
    }

    public static Result run(Graph g) {
        if (!g.isDirected()) throw new IllegalArgumentException("topological order needs a directed graph");
        int n = g.vertexCount();
        Result res = new Result();
        int[] indeg = new int[n];
        for (Edge e : g.edges()) { indeg[e.to]++; res.edgeExaminations++; }

        Deque<Integer> queue = new ArrayDeque<>();
        for (int v = 0; v < n; v++) if (indeg[v] == 0) queue.add(v);

        while (!queue.isEmpty()) {
            int v = queue.poll();
            res.vertexPops++;
            res.order.add(v);
            for (Edge e : g.neighbors(v)) {
                res.edgeExaminations++;
                if (--indeg[e.to] == 0) queue.add(e.to);
            }
        }
        res.unorderedVertices = n - res.order.size();
        res.isDag = res.unorderedVertices == 0;
        return res;
    }

    public static boolean isValidOrder(Graph g, List<Integer> order) {
        int n = g.vertexCount();
        if (order.size() != n) return false;
        int[] pos = new int[n];
        java.util.Arrays.fill(pos, -1);
        for (int i = 0; i < n; i++) {
            int v = order.get(i);
            if (v < 0 || v >= n || pos[v] != -1) return false;
            pos[v] = i;
        }
        for (Edge e : g.edges()) if (pos[e.from] >= pos[e.to]) return false;
        return true;
    }
}
