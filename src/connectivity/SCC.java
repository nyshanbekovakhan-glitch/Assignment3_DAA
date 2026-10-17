package connectivity;
import graph.Edge;
import graph.Graph;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
public final class SCC {
    private SCC() {}
    public static final class Result {
        public final List<List<Integer>> components = new ArrayList<>();
        public int[] componentOf;
        public long edgeExaminations;
        public long vertexVisits;
        public int count() { return components.size(); }
        public int largestComponentSize() {
            int best = 0;
            for (List<Integer> c : components) best = Math.max(best, c.size());
            return best;
        }
    }
    public static Result run(Graph g) {
        if (!g.isDirected()) throw new IllegalArgumentException("SCC needs a directed graph");
        int n = g.vertexCount();
        Result res = new Result();
        res.componentOf = new int[n];
        int[] index = new int[n], low = new int[n], edgePos = new int[n];
        java.util.Arrays.fill(index, -1);
        boolean[] onStack = new boolean[n];
        Deque<Integer> stack = new ArrayDeque<>();
        Deque<Integer> calls = new ArrayDeque<>();
        int counter = 0;
        for (int s = 0; s < n; s++) {
            if (index[s] != -1) continue;
            index[s] = low[s] = counter++;
            res.vertexVisits++;
            stack.push(s); onStack[s] = true; calls.push(s);
            while (!calls.isEmpty()) {
                int v = calls.peek();
                List<Edge> adj = g.neighbors(v);
                if (edgePos[v] < adj.size()) {
                    int w = adj.get(edgePos[v]++).to;
                    res.edgeExaminations++;
                    if (index[w] == -1) {
                        index[w] = low[w] = counter++;
                        res.vertexVisits++;
                        stack.push(w); onStack[w] = true; calls.push(w);
                    } else if (onStack[w]) {
                        low[v] = Math.min(low[v], index[w]);
                    }
                } else {
                    calls.pop();
                    if (low[v] == index[v]) {
                        List<Integer> comp = new ArrayList<>();
                        int w;
                        do {
                            w = stack.pop();
                            onStack[w] = false;
                            res.componentOf[w] = res.components.size();
                            comp.add(w);
                        } while (w != v);
                        Collections.sort(comp);
                        res.components.add(comp);
                    }
                    if (!calls.isEmpty()) {
                        int parent = calls.peek();
                        low[parent] = Math.min(low[parent], low[v]);
                    }
                }
            }
        }
        return res;
    }
    public static Graph condensation(Graph g, Result scc) {
        Graph dag = new Graph(scc.count(), true);
        Set<Long> seen = new HashSet<>();
        for (Edge e : g.edges()) {
            int a = scc.componentOf[e.from], b = scc.componentOf[e.to];
            if (a != b && seen.add((long) a * scc.count() + b)) dag.addEdge(a, b, 1);
        }
        return dag;
    }
}
