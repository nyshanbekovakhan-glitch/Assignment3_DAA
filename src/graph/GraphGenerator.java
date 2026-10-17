package graph;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
public final class GraphGenerator {
    private GraphGenerator() {}
    public static Graph connectedUndirected(int n, int m, long seed, int maxWeight) {
        if (n < 1) throw new IllegalArgumentException("n must be >= 1");
        long maxEdges = (long) n * (n - 1) / 2;
        if (m < n - 1 || m > maxEdges)
            throw new IllegalArgumentException("m must be in [" + (n - 1) + ", " + maxEdges + "] but was " + m);
        Random rnd = new Random(seed);
        Graph g = new Graph(n, false);
        Set<Long> used = new HashSet<>();

        int[] perm = new int[n];
        for (int i = 0; i < n; i++) perm[i] = i;
        for (int i = n - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = perm[i]; perm[i] = perm[j]; perm[j] = t;
        }
        for (int i = 1; i < n; i++) {
            int u = perm[i];
            int v = perm[rnd.nextInt(i)];
            used.add(undirectedKey(u, v, n));
            g.addEdge(u, v, weight(rnd, maxWeight));
        }
        while (g.edgeCount() < m) {
            int u = rnd.nextInt(n), v = rnd.nextInt(n);
            if (u == v) continue;
            if (used.add(undirectedKey(u, v, n))) g.addEdge(u, v, weight(rnd, maxWeight));
        }
        return g;
    }
    public static Graph randomDirected(int n, int m, long seed, int maxWeight) {
        long maxEdges = (long) n * (n - 1);
        if (n < 1 || m < 0 || m > maxEdges) throw new IllegalArgumentException("invalid n/m");
        Random rnd = new Random(seed);
        Graph g = new Graph(n, true);
        Set<Long> used = new HashSet<>();
        while (g.edgeCount() < m) {
            int u = rnd.nextInt(n), v = rnd.nextInt(n);
            if (u == v) continue;
            if (used.add((long) u * n + v)) g.addEdge(u, v, weight(rnd, maxWeight));
        }
        return g;
    }
    public static Graph randomDag(int n, int m, long seed, int maxWeight) {
        long maxEdges = (long) n * (n - 1) / 2;
        if (n < 1 || m < 0 || m > maxEdges) throw new IllegalArgumentException("invalid n/m");
        Random rnd = new Random(seed);
        int[] rank = new int[n];
        for (int i = 0; i < n; i++) rank[i] = i;
        for (int i = n - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = rank[i]; rank[i] = rank[j]; rank[j] = t;
        }
        Graph g = new Graph(n, true);
        Set<Long> used = new HashSet<>();
        while (g.edgeCount() < m) {
            int u = rnd.nextInt(n), v = rnd.nextInt(n);
            if (u == v) continue;
            if (rank[u] > rank[v]) { int t = u; u = v; v = t; }
            if (used.add((long) u * n + v)) g.addEdge(u, v, weight(rnd, maxWeight));
        }
        return g;
    }

    private static int weight(Random rnd, int maxWeight) { return 1 + rnd.nextInt(maxWeight); }

    private static long undirectedKey(int u, int v, int n) {
        int a = Math.min(u, v), b = Math.max(u, v);
        return (long) a * n + b;
    }
}
