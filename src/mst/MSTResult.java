package mst;

import graph.Edge;

import java.util.ArrayList;
import java.util.List;

public class MSTResult {
    public final List<Edge> edges = new ArrayList<>();
    public long totalWeight;
    public int components;
    public long edgeExaminations;
    public long comparisons;
    public long dsuFinds;
    public long dsuUnions;
    public long dsuParentSteps;
    public long pqPushes;
    public long pqPops;

    public boolean isConnected() { return components <= 1; }
}
