package app;

import connectivity.SCC;
import connectivity.TopologicalSort;
import graph.Edge;
import graph.Graph;
import mst.Kruskal;
import mst.MSTResult;
import mst.Prim;
import shortestpath.Dijkstra;
import shortestpath.DijkstraResult;

import java.util.ArrayList;
import java.util.List;

public final class CityDemo {
    private static final String[] DISTRICTS = {
        "Central", "Harbor", "Airport", "University", "Old Town", "Tech Park", "Stadium", "Riverside"
    };

    public static void main(String[] args) {
        System.out.println("=== 1. Infrastructure planning: cheapest network connecting all districts (MST) ===");
        Graph costs = new Graph(DISTRICTS.length, false);
        link(costs, "Central", "Old Town", 4);   link(costs, "Central", "University", 7);
        link(costs, "Central", "Tech Park", 9);  link(costs, "Old Town", "Harbor", 6);
        link(costs, "Old Town", "Riverside", 5); link(costs, "Harbor", "Riverside", 3);
        link(costs, "Harbor", "Airport", 12);    link(costs, "University", "Tech Park", 2);
        link(costs, "Tech Park", "Stadium", 8);  link(costs, "Stadium", "Airport", 10);
        link(costs, "Riverside", "Stadium", 11); link(costs, "University", "Old Town", 8);
        MSTResult k = Kruskal.run(costs), p = Prim.run(costs);
        System.out.println("Kruskal: total cost " + k.totalWeight + ", connected = " + k.isConnected());
        for (Edge e : k.edges) System.out.println("   build " + DISTRICTS[e.from] + " <-> " + DISTRICTS[e.to] + "  (cost " + e.weight + ")");
        System.out.println("Prim:    total cost " + p.totalWeight + "  -> same cost: " + (k.totalWeight == p.totalWeight));

        System.out.println("\n=== 2. One-way roads: strongly connected components ===");
        Graph oneWay = new Graph(DISTRICTS.length, true);
        arrow(oneWay, "Central", "Old Town", 1);  arrow(oneWay, "Old Town", "Harbor", 1);
        arrow(oneWay, "Harbor", "Central", 1);    arrow(oneWay, "Harbor", "Airport", 1);
        arrow(oneWay, "University", "Tech Park", 1); arrow(oneWay, "Tech Park", "University", 1);
        arrow(oneWay, "Tech Park", "Stadium", 1); arrow(oneWay, "Stadium", "Riverside", 1);
        arrow(oneWay, "Riverside", "Airport", 1);
        SCC.Result scc = SCC.run(oneWay);
        System.out.println("Number of SCCs: " + scc.count());
        for (List<Integer> c : scc.components) System.out.println("   {" + names(c, ", ") + "}");
        System.out.println("Topological order of the whole road map possible? " + TopologicalSort.run(oneWay).isDag);

        System.out.println("\n=== 3. Service dependencies: topological ordering ===");
        String[] services = {"Power grid", "Water pumps", "Traffic lights", "Control center", "Public transport", "Emergency dispatch"};
        Graph deps = new Graph(services.length, true);
        int[][] d = {{0, 1}, {0, 2}, {0, 3}, {1, 3}, {2, 4}, {3, 4}, {3, 5}, {4, 5}};
        for (int[] e : d) deps.addEdge(e[0], e[1], 1);
        TopologicalSort.Result t = TopologicalSort.run(deps);
        System.out.println("Is DAG: " + t.isDag);
        StringBuilder sb = new StringBuilder("Start-up order: ");
        for (int i = 0; i < t.order.size(); i++) sb.append(i == 0 ? "" : " -> ").append(services[t.order.get(i)]);
        System.out.println(sb);
        deps.addEdge(5, 0, 1);
        TopologicalSort.Result bad = TopologicalSort.run(deps);
        System.out.println("After adding dispatch -> power: is DAG = " + bad.isDag + " (" + bad.unorderedVertices + " services can no longer be ordered: they are on or behind the cycle)");

        System.out.println("\n=== 4. Route planning: Dijkstra from Central (distances in km) ===");
        Graph roads = new Graph(DISTRICTS.length + 1, false);
        for (Edge e : costs.edges()) roads.addEdge(e.from, e.to, e.weight);
        DijkstraResult r = Dijkstra.run(roads, 0);
        for (int v = 0; v < roads.vertexCount(); v++) {
            String name = v < DISTRICTS.length ? DISTRICTS[v] : "Remote Village (not connected)";
            if (r.isReachable(v)) System.out.printf("   %-18s %3d km   route: %s%n", name, r.dist[v], names(r.pathTo(v)));
            else System.out.printf("   %-18s unreachable%n", name);
        }
    }

    private static int id(String name) {
        for (int i = 0; i < DISTRICTS.length; i++) if (DISTRICTS[i].equals(name)) return i;
        throw new IllegalArgumentException(name);
    }
    private static void link(Graph g, String a, String b, int w) { g.addEdge(id(a), id(b), w); }
    private static void arrow(Graph g, String a, String b, int w) { g.addEdge(id(a), id(b), w); }
    private static String names(List<Integer> vs) { return names(vs, " -> "); }
    private static String names(List<Integer> vs, String separator) {
        List<String> out = new ArrayList<>();
        for (int v : vs) out.add(DISTRICTS[v]);
        return String.join(separator, out);
    }
}
