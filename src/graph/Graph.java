package graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Graph {
    private final int n;
    private final boolean directed;
    private final List<List<Edge>> adj;
    private final List<Edge> edges = new ArrayList<>();
    private boolean hasNegativeWeight = false;

    public Graph(int n, boolean directed) {
        if (n < 0) throw new IllegalArgumentException("n must be >= 0");
        this.n = n;
        this.directed = directed;
        this.adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    }

    public void addEdge(int u, int v, int weight) {
        if (u < 0 || u >= n || v < 0 || v >= n)
            throw new IllegalArgumentException("vertex out of range: " + u + ", " + v);
        Edge e = new Edge(u, v, weight);
        edges.add(e);
        adj.get(u).add(e);
        if (!directed && u != v) adj.get(v).add(new Edge(v, u, weight));
        if (weight < 0) hasNegativeWeight = true;
    }

    public int vertexCount() { return n; }
    public int edgeCount() { return edges.size(); }
    public boolean isDirected() { return directed; }
    public boolean hasNegativeWeight() { return hasNegativeWeight; }

    public List<Edge> neighbors(int u) { return Collections.unmodifiableList(adj.get(u)); }

    public List<Edge> edges() { return Collections.unmodifiableList(edges); }

    @Override
    public String toString() {
        return (directed ? "Directed" : "Undirected") + "Graph(V=" + n + ", E=" + edges.size() + ")";
    }
}
