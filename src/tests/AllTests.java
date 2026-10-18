package tests;

import connectivity.SCC;
import connectivity.TopologicalSort;
import dsu.DSU;
import graph.Edge;
import graph.Graph;
import graph.GraphGenerator;
import mst.Kruskal;
import mst.MSTResult;
import mst.Prim;
import shortestpath.Dijkstra;
import shortestpath.DijkstraResult;

import java.util.*;

public final class AllTests {
    private static int passed = 0, failed = 0;

    private static void check(boolean ok, String name) {
        if (ok) { passed++; System.out.println("  PASS  " + name); }
        else { failed++; System.out.println("  FAIL  " + name); }
    }

    private static void section(String title) { System.out.println("\n== " + title + " =="); }

    public static void main(String[] args) {
        testDsu();
        testMstKnownGraphs();
        testMstSpecialCases();
        testMstRandomized();
        testTopologicalSort();
        testScc();
        testDijkstra();
        testGenerators();
        System.out.println("\nPassed: " + passed + "   Failed: " + failed);
        System.exit(failed == 0 ? 0 : 1);
    }

    private static Graph undirected(int n, int[][] edges) {
        Graph g = new Graph(n, false);
        for (int[] e : edges) g.addEdge(e[0], e[1], e[2]);
        return g;
    }

    private static Graph directed(int n, int[][] edges) {
        Graph g = new Graph(n, true);
        for (int[] e : edges) g.addEdge(e[0], e[1], e.length > 2 ? e[2] : 1);
        return g;
    }

    private static boolean isSpanningForest(Graph g, MSTResult r) {
        DSU d = new DSU(g.vertexCount());
        for (Edge e : r.edges) if (!d.union(e.from, e.to)) return false;
        DSU full = new DSU(g.vertexCount());
        for (Edge e : g.edges()) full.union(e.from, e.to);
        return d.components() == full.components();
    }

    private static long[] bellmanFord(Graph g, int s) {
        int n = g.vertexCount();
        long[] dist = new long[n];
        Arrays.fill(dist, DijkstraResult.INF);
        dist[s] = 0;
        for (int i = 0; i < n - 1; i++) {
            boolean changed = false;
            for (Edge e : g.edges()) {
                if (dist[e.from] != DijkstraResult.INF && dist[e.from] + e.weight < dist[e.to]) {
                    dist[e.to] = dist[e.from] + e.weight; changed = true;
                }
                if (!g.isDirected() && dist[e.to] != DijkstraResult.INF && dist[e.to] + e.weight < dist[e.from]) {
                    dist[e.from] = dist[e.to] + e.weight; changed = true;
                }
            }
            if (!changed) break;
        }
        return dist;
    }

    private static void testDsu() {
        section("DSU");
        DSU d = new DSU(6);
        check(d.components() == 6, "starts with n singleton sets");
        check(d.union(0, 1) && d.union(2, 3) && d.union(1, 3), "union merges different sets");
        check(!d.union(0, 3), "union of already-connected elements returns false");
        check(d.connected(0, 2) && !d.connected(0, 4), "find/connected answers correctly");
        check(d.components() == 3, "component count is updated");

        Random rnd = new Random(1);
        boolean allAgree = true;
        for (int variant = 0; variant < 4; variant++) {
            DSU x = new DSU(50, (variant & 1) != 0, (variant & 2) != 0);
            int[] label = new int[50];
            for (int i = 0; i < 50; i++) label[i] = i;
            for (int op = 0; op < 300; op++) {
                int a = rnd.nextInt(50), b = rnd.nextInt(50);
                x.union(a, b);
                int la = label[a], lb = label[b];
                if (la != lb) for (int i = 0; i < 50; i++) if (label[i] == lb) label[i] = la;
                int p = rnd.nextInt(50), q = rnd.nextInt(50);
                if (x.connected(p, q) != (label[p] == label[q])) allAgree = false;
            }
        }
        check(allAgree, "all 4 optimisation variants match a naive reference on random operations");

        DSU chain = new DSU(1000, false, false);
        for (int i = 0; i + 1 < 1000; i++) chain.union(i, i + 1);
        long before = chain.parentSteps;
        chain.find(0);
        check(chain.parentSteps - before == 999, "naive DSU builds a chain (find(0) walks 999 parent links)");
        DSU comp = new DSU(1000, true, false);
        for (int i = 0; i + 1 < 1000; i++) comp.union(i, i + 1);
        comp.find(0);
        before = comp.parentSteps;
        comp.find(0);
        check(comp.parentSteps - before <= 1, "path compression makes repeated find nearly O(1)");
        DSU rank = new DSU(1024, false, true);
        for (int i = 0; i + 1 < 1024; i++) rank.union(i, i + 1);
        long worst = 0;
        for (int i = 0; i < 1024; i++) { long b0 = rank.parentSteps; rank.find(i); worst = Math.max(worst, rank.parentSteps - b0); }
        check(worst <= 10, "union by rank keeps tree height <= log2(n) (worst find = " + worst + " steps)");
    }

    private static void testMstKnownGraphs() {
        section("MST - small known graph");
        Graph g = undirected(5, new int[][]{{0, 1, 2}, {0, 3, 6}, {1, 2, 3}, {1, 3, 8}, {1, 4, 5}, {2, 4, 7}, {3, 4, 9}});
        MSTResult k = Kruskal.run(g), p = Prim.run(g);
        check(k.totalWeight == 16, "Kruskal weight = 16");
        check(p.totalWeight == 16, "Prim weight = 16");
        check(k.edges.size() == 4 && p.edges.size() == 4, "both return V-1 edges");
        check(k.isConnected() && p.isConnected(), "connected graph detected");
        check(isSpanningForest(g, k) && isSpanningForest(g, p), "results are spanning trees (acyclic + spanning)");
    }

    private static void testMstSpecialCases() {
        section("MST - special cases");
        Graph disc = undirected(5, new int[][]{{0, 1, 1}, {1, 2, 2}, {0, 2, 5}, {3, 4, 7}});
        MSTResult k = Kruskal.run(disc), p = Prim.run(disc);
        check(!k.isConnected() && !p.isConnected(), "disconnected graph detected by both");
        check(k.components == 2 && p.components == 2, "2 components reported");
        check(k.totalWeight == 10 && p.totalWeight == 10, "minimum spanning forest weight = 10");
        check(isSpanningForest(disc, k) && isSpanningForest(disc, p), "forest is valid");

        Graph k4 = new Graph(4, false);
        for (int i = 0; i < 4; i++) for (int j = i + 1; j < 4; j++) k4.addEdge(i, j, 3);
        check(Kruskal.run(k4).totalWeight == 9 && Prim.run(k4).totalWeight == 9, "duplicate weights / multiple MSTs: weight = 9");

        Graph tri = undirected(3, new int[][]{{0, 1, 4}, {1, 2, 4}, {0, 2, 4}});
        check(Kruskal.run(tri).totalWeight == Prim.run(tri).totalWeight, "equal-weight triangle: same weight");

        Graph one = new Graph(1, false);
        MSTResult k1 = Kruskal.run(one), p1 = Prim.run(one);
        check(k1.totalWeight == 0 && p1.totalWeight == 0 && k1.edges.isEmpty() && p1.edges.isEmpty(), "single vertex: weight 0, no edges");
        check(k1.isConnected() && p1.isConnected(), "single vertex is connected");

        Graph isolated = undirected(4, new int[][]{{0, 1, 3}});
        check(Kruskal.run(isolated).components == 3 && Prim.run(isolated).components == 3, "isolated vertices counted as components");

        boolean threw = false;
        try { Kruskal.run(new Graph(3, true)); } catch (IllegalArgumentException e) { threw = true; }
        check(threw, "directed graph rejected by MST algorithms");
    }

    private static void testMstRandomized() {
        section("MST - randomized: Kruskal == Prim");
        boolean same = true, valid = true;
        int runs = 0;
        for (int seed = 0; seed < 300; seed++) {
            Random r = new Random(seed);
            int n = 2 + r.nextInt(40);
            long max = (long) n * (n - 1) / 2;
            int m = (int) Math.min(max, n - 1 + r.nextInt(3 * n));
            int maxW = (seed % 2 == 0) ? 3 : 1000;
            Graph g = GraphGenerator.connectedUndirected(n, m, seed, maxW);
            MSTResult k = Kruskal.run(g), p = Prim.run(g);
            if (k.totalWeight != p.totalWeight) same = false;
            if (!isSpanningForest(g, k) || !isSpanningForest(g, p) || k.edges.size() != n - 1 || p.edges.size() != n - 1) valid = false;
            runs++;
        }
        check(same, "Kruskal and Prim give the same weight on " + runs + " random connected graphs (incl. many ties)");
        check(valid, "all returned trees are valid spanning trees with V-1 edges");

        boolean sameForest = true;
        for (int seed = 0; seed < 100; seed++) {
            Random r = new Random(1000 + seed);
            int n = 5 + r.nextInt(30);
            Graph g = new Graph(n, false);
            Set<Long> used = new HashSet<>();
            for (int i = 0; i < n; i++) {
                int u = r.nextInt(n), v = r.nextInt(n);
                if (u != v && used.add((long) Math.min(u, v) * n + Math.max(u, v))) g.addEdge(u, v, 1 + r.nextInt(5));
            }
            MSTResult k = Kruskal.run(g), p = Prim.run(g);
            if (k.totalWeight != p.totalWeight || k.components != p.components || !isSpanningForest(g, k) || !isSpanningForest(g, p)) sameForest = false;
        }
        check(sameForest, "Kruskal and Prim agree on 100 random (mostly disconnected) graphs: forest weight and components");
    }

    private static void testTopologicalSort() {
        section("Topological ordering");
        Graph dag = directed(6, new int[][]{{5, 2}, {5, 0}, {4, 0}, {4, 1}, {2, 3}, {3, 1}});
        TopologicalSort.Result r = TopologicalSort.run(dag);
        check(r.isDag && TopologicalSort.isValidOrder(dag, r.order), "classic DAG: valid order " + r.order);

        boolean allValid = true;
        for (int seed = 0; seed < 200; seed++) {
            int n = 1 + new Random(seed).nextInt(60);
            long max = (long) n * (n - 1) / 2;
            int m = (int) Math.min(max, (long) n * 2);
            Graph g = GraphGenerator.randomDag(n, m, seed, 5);
            TopologicalSort.Result t = TopologicalSort.run(g);
            if (!t.isDag || !TopologicalSort.isValidOrder(g, t.order)) allValid = false;
        }
        check(allValid, "valid order for 200 random DAGs");

        Graph cyc = directed(3, new int[][]{{0, 1}, {1, 2}, {2, 0}});
        TopologicalSort.Result c = TopologicalSort.run(cyc);
        check(!c.isDag && c.unorderedVertices == 3, "3-cycle: impossible, all 3 vertices unordered");
        Graph selfLoop = directed(2, new int[][]{{0, 0}, {0, 1}});
        check(!TopologicalSort.run(selfLoop).isDag, "self-loop counts as a cycle");
        Graph mixed = directed(5, new int[][]{{0, 1}, {1, 2}, {2, 1}, {2, 3}, {3, 4}});
        TopologicalSort.Result m = TopologicalSort.run(mixed);
        check(!m.isDag && m.unorderedVertices == 4 && m.order.equals(List.of(0)), "cycle inside a larger graph is detected");
        check(!TopologicalSort.isValidOrder(dag, List.of(0, 1, 2, 3, 4, 5)), "validator rejects a wrong order");
        Graph noEdges = new Graph(4, true);
        check(TopologicalSort.run(noEdges).isDag, "graph without edges is a DAG");
        Graph one = new Graph(1, true);
        check(TopologicalSort.run(one).order.equals(List.of(0)), "single vertex: order [0]");

        boolean cyclicDetected = true;
        for (int seed = 0; seed < 100; seed++) {
            Graph g = GraphGenerator.randomDirected(20, 60, seed, 5);
            boolean hasCycle = SCC.run(g).largestComponentSize() > 1;
            if (TopologicalSort.run(g).isDag == hasCycle) cyclicDetected = false;
        }
        check(cyclicDetected, "topological sort fails exactly when SCC finds a component with >1 vertex (100 random digraphs)");
    }

    private static void testScc() {
        section("Strongly connected components");
        Graph g = directed(8, new int[][]{{0, 1}, {1, 2}, {2, 0}, {1, 3}, {3, 4}, {4, 5}, {5, 3}, {6, 5}, {6, 7}, {7, 6}});
        SCC.Result r = SCC.run(g);
        Set<Set<Integer>> found = new HashSet<>();
        for (List<Integer> c : r.components) found.add(new HashSet<>(c));
        Set<Set<Integer>> expected = Set.of(Set.of(0, 1, 2), Set.of(3, 4, 5), Set.of(6, 7));
        check(found.equals(expected), "known graph: components {0,1,2} {3,4,5} {6,7} -> " + r.components);

        check(SCC.run(directed(4, new int[][]{{0, 1}, {1, 2}, {2, 3}})).count() == 4, "path graph: every vertex is its own SCC");
        check(SCC.run(directed(1, new int[][]{})).count() == 1, "single vertex: one SCC");
        check(SCC.run(new Graph(5, true)).count() == 5, "no edges: V components");

        boolean oracle = true, partition = true;
        for (int seed = 0; seed < 200; seed++) {
            Random rnd = new Random(seed);
            int n = 1 + rnd.nextInt(12);
            int m = (int) Math.min((long) n * (n - 1), rnd.nextInt(3 * n + 1));
            Graph h = GraphGenerator.randomDirected(n, m, seed, 3);
            boolean[][] reach = new boolean[n][n];
            for (int i = 0; i < n; i++) reach[i][i] = true;
            for (Edge e : h.edges()) reach[e.from][e.to] = true;
            for (int k = 0; k < n; k++) for (int i = 0; i < n; i++) for (int j = 0; j < n; j++)
                if (reach[i][k] && reach[k][j]) reach[i][j] = true;
            SCC.Result s = SCC.run(h);
            int total = 0;
            for (List<Integer> c : s.components) total += c.size();
            if (total != n) partition = false;
            for (int i = 0; i < n; i++) for (int j = 0; j < n; j++)
                if ((s.componentOf[i] == s.componentOf[j]) != (reach[i][j] && reach[j][i])) oracle = false;
        }
        check(partition, "every vertex belongs to exactly one SCC (sizes sum to V, 200 random graphs)");
        check(oracle, "SCC membership equals mutual reachability (Floyd-Warshall oracle, 200 random graphs)");

        Graph dagOfScc = SCC.condensation(g, r);
        TopologicalSort.Result t = TopologicalSort.run(dagOfScc);
        check(dagOfScc.vertexCount() == 3 && t.isDag, "condensation graph is a DAG");

        Graph longPath = new Graph(200_000, true);
        for (int i = 0; i + 1 < 200_000; i++) longPath.addEdge(i, i + 1, 1);
        check(SCC.run(longPath).count() == 200_000, "200,000-vertex path: no StackOverflowError (iterative DFS)");
    }

    private static void testDijkstra() {
        section("Dijkstra");
        Graph g = directed(6, new int[][]{{0, 1, 7}, {0, 2, 9}, {0, 5, 14}, {1, 2, 10}, {1, 3, 15}, {2, 3, 11}, {2, 5, 2}, {5, 4, 9}, {3, 4, 6}});
        DijkstraResult r = Dijkstra.run(g, 0);
        check(Arrays.equals(r.dist, new long[]{0, 7, 9, 20, 20, 11}), "known graph distances " + Arrays.toString(r.dist));
        check(r.pathTo(4).equals(List.of(0, 2, 5, 4)), "path to 4 = " + r.pathTo(4));
        check(r.pathTo(0).equals(List.of(0)), "path to the source is [source]");

        Graph un = directed(4, new int[][]{{0, 1, 5}, {2, 3, 1}});
        DijkstraResult u = Dijkstra.run(un, 0);
        check(u.dist[2] == DijkstraResult.INF && u.dist[3] == DijkstraResult.INF, "unreachable vertices get INF");
        check(u.pathTo(3).isEmpty() && !u.isReachable(3) && u.reachableCount() == 2, "unreachable: empty path, reachable count = 2");

        DijkstraResult s = Dijkstra.run(new Graph(1, false), 0);
        check(s.dist[0] == 0 && s.reachableCount() == 1, "single vertex: distance 0");

        Graph zero = directed(3, new int[][]{{0, 1, 0}, {1, 2, 0}});
        check(Dijkstra.run(zero, 0).dist[2] == 0, "zero-weight edges are allowed");

        boolean match = true, pathsOk = true;
        for (int seed = 0; seed < 150; seed++) {
            Random rnd = new Random(seed);
            int n = 2 + rnd.nextInt(40);
            boolean dir = seed % 2 == 0;
            Graph h;
            if (dir) h = GraphGenerator.randomDirected(n, Math.min(n * (n - 1), rnd.nextInt(4 * n)), seed, 20);
            else h = GraphGenerator.connectedUndirected(n, (int) Math.min((long) n * (n - 1) / 2, n - 1 + rnd.nextInt(3 * n)), seed, 20);
            int src = rnd.nextInt(n);
            DijkstraResult d = Dijkstra.run(h, src);
            long[] ref = bellmanFord(h, src);
            if (!Arrays.equals(d.dist, ref)) match = false;
            for (int v = 0; v < n; v++) {
                List<Integer> p = d.pathTo(v);
                if (!d.isReachable(v)) continue;
                long sum = 0;
                for (int i = 0; i + 1 < p.size(); i++) {
                    long best = Long.MAX_VALUE;
                    for (Edge e : h.neighbors(p.get(i))) if (e.to == p.get(i + 1)) best = Math.min(best, e.weight);
                    sum += best;
                }
                if (sum != d.dist[v] || p.get(0) != src) pathsOk = false;
            }
        }
        check(match, "distances equal Bellman-Ford on 150 random graphs (directed + undirected)");
        check(pathsOk, "reconstructed path weights equal reported distances");

        Graph neg = directed(3, new int[][]{{0, 1, 2}, {0, 2, 5}, {2, 1, -4}});
        boolean threw = false;
        try { Dijkstra.run(neg, 0); } catch (IllegalArgumentException e) { threw = true; }
        check(threw, "negative edge weight is rejected");
        long trueDist = bellmanFord(neg, 0)[1];
        long dijkstraDist = Dijkstra.runUnchecked(neg, 0).dist[1];
        check(trueDist == 1 && dijkstraDist == 2,
              "why Dijkstra fails with negative edges: true dist(0->1) = " + trueDist + ", unchecked Dijkstra says " + dijkstraDist);
    }

    private static void testGenerators() {
        section("Graph generators");
        Graph a = GraphGenerator.connectedUndirected(100, 500, 42, 100);
        Graph b = GraphGenerator.connectedUndirected(100, 500, 42, 100);
        boolean identical = a.edgeCount() == b.edgeCount();
        for (int i = 0; identical && i < a.edgeCount(); i++) {
            Edge x = a.edges().get(i), y = b.edges().get(i);
            identical = x.from == y.from && x.to == y.to && x.weight == y.weight;
        }
        check(identical, "same seed -> identical graph (reproducible)");

        boolean connected = true, exact = true, noDup = true;
        int[][] cfg = {{10, 30}, {100, 500}, {1000, 5000}, {50, 49}, {30, 435}};
        for (int[] c : cfg) {
            Graph g = GraphGenerator.connectedUndirected(c[0], c[1], 7, 50);
            DSU d = new DSU(c[0]);
            Set<Long> keys = new HashSet<>();
            for (Edge e : g.edges()) {
                d.union(e.from, e.to);
                if (e.from == e.to || !keys.add((long) Math.min(e.from, e.to) * c[0] + Math.max(e.from, e.to))) noDup = false;
            }
            if (d.components() != 1) connected = false;
            if (g.edgeCount() != c[1]) exact = false;
        }
        check(connected, "undirected generator always produces a connected graph (10/30, 100/500, 1000/5000, tree, complete)");
        check(exact && noDup, "exact edge count, no self-loops, no duplicate edges");

        boolean dagOk = true, cycOk = true;
        for (int[] c : new int[][]{{10, 30}, {100, 500}, {1000, 5000}}) {
            dagOk &= TopologicalSort.run(GraphGenerator.randomDag(c[0], c[1], 11, 10)).isDag;
            cycOk &= SCC.run(GraphGenerator.randomDirected(c[0], c[1], 11, 10)).largestComponentSize() > 1;
        }
        check(dagOk, "DAG generator output is acyclic for all experiment sizes");
        check(cycOk, "random directed generator output contains cycles for all experiment sizes");
    }
}
