package experiments;

import connectivity.SCC;
import connectivity.TopologicalSort;
import dsu.DSU;
import graph.Graph;
import graph.GraphGenerator;
import mst.Kruskal;
import mst.MSTResult;
import mst.Prim;
import shortestpath.Dijkstra;
import shortestpath.DijkstraResult;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;

public final class Benchmark {
    static final long BASE_SEED = 42;
    static final int REPS = 5;
    static final int WARMUP = 20;
    static final int MAX_WEIGHT = 100;

    record Config(String name, int v, int e) {}

    static final Config[] REQUIRED = {
        new Config("Small", 10, 30), new Config("Medium", 100, 500), new Config("Large", 1000, 5000)
    };
    static final int[] VERTEX_SWEEP = {250, 500, 1000, 2000, 4000, 8000, 16000};
    static final int[] EDGE_SWEEP = {1000, 2000, 5000, 10000, 20000, 50000, 100000};

    private static long sink = 0;

    static final class Row {
        String experiment, graph, algorithm, graphType, info = "";
        int v, e;
        double avgUs;
        long weight = -1, edgeExams, comparisons, dsuFinds, dsuUnions, dsuSteps, pqPush, pqPop, relax;
        String label() { return algorithm + " / " + graphType; }
    }

    static final class Measured<T> {
        final double avgUs; final T result;
        Measured(double avgUs, T result) { this.avgUs = avgUs; this.result = result; }
    }

    static <T> Measured<T> measure(Supplier<T> task) {
        T res = null;
        long start = System.nanoTime();
        int i = 0;
        while (i < WARMUP || System.nanoTime() - start < 100_000_000L) { res = task.get(); i++; }
        long t0 = System.nanoTime();
        for (int k = 0; k < 5; k++) res = task.get();
        long estimate = Math.max(1, (System.nanoTime() - t0) / 5);
        int inner = (int) Math.max(1, Math.min(2000, 10_000_000L / estimate));
        sink += System.identityHashCode(res);
        System.gc();
        double sumPerRun = 0;
        for (int rep = 0; rep < REPS; rep++) {
            long a = System.nanoTime();
            for (int j = 0; j < inner; j++) res = task.get();
            long b = System.nanoTime();
            sumPerRun += (b - a) / (double) inner;
        }
        sink += System.identityHashCode(res);
        return new Measured<>(sumPerRun / REPS / 1000.0, res);
    }

    static long seed(int type, int v, int e) { return BASE_SEED + type * 1_000_003L + v * 10_007L + e; }

    static Row row(String exp, String graphName, int v, int e, String algo, String type, double avgUs) {
        Row r = new Row();
        r.experiment = exp; r.graph = graphName; r.v = v; r.e = e; r.algorithm = algo; r.graphType = type; r.avgUs = avgUs;
        return r;
    }

    static List<Row> benchmarkAll(String exp, String name, int v, int e) {
        List<Row> rows = new ArrayList<>();
        Graph ug = GraphGenerator.connectedUndirected(v, e, seed(0, v, e), MAX_WEIGHT);
        Graph dg = GraphGenerator.randomDirected(v, e, seed(1, v, e), MAX_WEIGHT);
        Graph dag = GraphGenerator.randomDag(v, e, seed(2, v, e), MAX_WEIGHT);

        Measured<MSTResult> mk = measure(() -> Kruskal.run(ug));
        Measured<MSTResult> mp = measure(() -> Prim.run(ug));
        if (mk.result.totalWeight != mp.result.totalWeight || !mk.result.isConnected() || !mp.result.isConnected())
            throw new IllegalStateException("Kruskal/Prim mismatch on " + name);
        Row rk = row(exp, name, v, e, "Kruskal", "undirected connected", mk.avgUs);
        rk.weight = mk.result.totalWeight; rk.edgeExams = mk.result.edgeExaminations; rk.comparisons = mk.result.comparisons;
        rk.dsuFinds = mk.result.dsuFinds; rk.dsuUnions = mk.result.dsuUnions; rk.dsuSteps = mk.result.dsuParentSteps;
        rk.info = "components=" + mk.result.components + ";edges=" + mk.result.edges.size();
        Row rp = row(exp, name, v, e, "Prim", "undirected connected", mp.avgUs);
        rp.weight = mp.result.totalWeight; rp.edgeExams = mp.result.edgeExaminations; rp.comparisons = mp.result.comparisons;
        rp.pqPush = mp.result.pqPushes; rp.pqPop = mp.result.pqPops;
        rp.info = "components=" + mp.result.components + ";edges=" + mp.result.edges.size();
        rows.add(rk); rows.add(rp);

        Measured<TopologicalSort.Result> td = measure(() -> TopologicalSort.run(dag));
        Measured<TopologicalSort.Result> tc = measure(() -> TopologicalSort.run(dg));
        if (!td.result.isDag || !TopologicalSort.isValidOrder(dag, td.result.order)) throw new IllegalStateException("topo DAG failed " + name);
        if (tc.result.isDag) throw new IllegalStateException("cyclic graph reported as DAG " + name);
        Row r1 = row(exp, name, v, e, "TopoSort (Kahn)", "DAG", td.avgUs);
        r1.edgeExams = td.result.edgeExaminations; r1.pqPop = td.result.vertexPops;
        r1.info = "isDag=true;validOrder=true;ordered=" + td.result.order.size();
        Row r2 = row(exp, name, v, e, "TopoSort (Kahn)", "directed cyclic", tc.avgUs);
        r2.edgeExams = tc.result.edgeExaminations; r2.pqPop = tc.result.vertexPops;
        r2.info = "isDag=false;ordered=" + tc.result.order.size() + ";unordered=" + tc.result.unorderedVertices;
        rows.add(r1); rows.add(r2);

        Measured<SCC.Result> sc = measure(() -> SCC.run(dg));
        Measured<SCC.Result> sd = measure(() -> SCC.run(dag));
        Row r3 = row(exp, name, v, e, "SCC (Tarjan)", "directed cyclic", sc.avgUs);
        r3.edgeExams = sc.result.edgeExaminations; r3.pqPop = sc.result.vertexVisits;
        r3.info = "sccs=" + sc.result.count() + ";largest=" + sc.result.largestComponentSize();
        Row r4 = row(exp, name, v, e, "SCC (Tarjan)", "DAG", sd.avgUs);
        r4.edgeExams = sd.result.edgeExaminations; r4.pqPop = sd.result.vertexVisits;
        r4.info = "sccs=" + sd.result.count() + ";largest=" + sd.result.largestComponentSize();
        rows.add(r3); rows.add(r4);

        Measured<DijkstraResult> du = measure(() -> Dijkstra.run(ug, 0));
        Measured<DijkstraResult> dd = measure(() -> Dijkstra.run(dg, 0));
        rows.add(dijkstraRow(exp, name, v, e, "undirected connected", du));
        rows.add(dijkstraRow(exp, name, v, e, "directed random", dd));
        return rows;
    }

    static Row dijkstraRow(String exp, String name, int v, int e, String type, Measured<DijkstraResult> m) {
        Row r = row(exp, name, v, e, "Dijkstra", type, m.avgUs);
        DijkstraResult d = m.result;
        r.edgeExams = d.edgeExaminations; r.comparisons = d.comparisons; r.pqPush = d.pqPushes; r.pqPop = d.pqPops; r.relax = d.relaxations;
        long far = 0;
        for (long x : d.dist) if (x != DijkstraResult.INF) far = Math.max(far, x);
        r.info = "reachable=" + d.reachableCount() + "/" + v + ";maxDist=" + far;
        return r;
    }

    static final String[] VARIANT_NAMES = {
        "no optimisation", "path compression only", "union by rank only", "path compression + union by rank"
    };

    static List<Row> dsuAblation() {
        List<Row> rows = new ArrayList<>();
        List<Config> cfgs = new ArrayList<>(Arrays.asList(REQUIRED));
        cfgs.add(new Config("XL", 10000, 50000));
        for (Config c : cfgs) {
            Graph ug = GraphGenerator.connectedUndirected(c.v(), c.e(), seed(0, c.v(), c.e()), MAX_WEIGHT);
            for (int variant = 0; variant < 4; variant++) {
                boolean pc = (variant & 1) != 0, ubr = (variant & 2) != 0;
                Measured<MSTResult> m = measure(() -> Kruskal.run(ug, pc, ubr));
                Row r = row("dsu_ablation", c.name(), c.v(), c.e(), "Kruskal + DSU", VARIANT_NAMES[variant], m.avgUs);
                r.weight = m.result.totalWeight; r.edgeExams = m.result.edgeExaminations; r.comparisons = m.result.comparisons;
                r.dsuFinds = m.result.dsuFinds; r.dsuUnions = m.result.dsuUnions; r.dsuSteps = m.result.dsuParentSteps;
                rows.add(r);
            }
        }
        return rows;
    }

    static List<Row> dsuChain(int n) {
        List<Row> rows = new ArrayList<>();
        for (int variant = 0; variant < 4; variant++) {
            boolean pc = (variant & 1) != 0, ubr = (variant & 2) != 0;
            Measured<DSU> m = measure(() -> {
                DSU d = new DSU(n, pc, ubr);
                for (int i = 0; i + 1 < n; i++) d.union(i, i + 1);
                for (int i = 0; i < n; i++) d.find(i);
                return d;
            });
            Row r = row("dsu_chain", "chain n=" + n, n, n - 1, "DSU", VARIANT_NAMES[variant], m.avgUs);
            r.dsuFinds = m.result.findCalls; r.dsuUnions = m.result.unionCalls; r.dsuSteps = m.result.parentSteps;
            rows.add(r);
        }
        return rows;
    }

    static final class Table {
        final List<String> headers; final List<List<String>> rows = new ArrayList<>();
        Table(String... headers) { this.headers = Arrays.asList(headers); }
        void add(Object... cells) {
            List<String> r = new ArrayList<>();
            for (Object c : cells) r.add(String.valueOf(c));
            rows.add(r);
        }
        void writeCsv(Path p) throws IOException {
            try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(p))) {
                w.println(String.join(",", headers));
                for (List<String> r : rows) {
                    List<String> q = new ArrayList<>();
                    for (String c : r) q.add(c.contains(",") ? "\"" + c + "\"" : c);
                    w.println(String.join(",", q));
                }
            }
        }
        void writeMarkdown(Path p) throws IOException {
            try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(p))) {
                w.println("| " + String.join(" | ", headers) + " |");
                w.println("|" + "---|".repeat(headers.size()));
                for (List<String> r : rows) w.println("| " + String.join(" | ", r) + " |");
            }
        }
    }

    static String us(double x) { return String.format(Locale.ROOT, "%.2f", x); }

    static void save(Table t, Path dir, String name) throws IOException {
        t.writeCsv(dir.resolve(name + ".csv"));
    }

    static List<Row> only(List<Row> rows, String exp) {
        List<Row> out = new ArrayList<>();
        for (Row r : rows) if (r.experiment.equals(exp)) out.add(r);
        return out;
    }

    static Table pivot(List<Row> rows, java.util.function.Function<Row, String> colKey) {
        LinkedHashMap<String, LinkedHashMap<String, Double>> data = new LinkedHashMap<>();
        LinkedHashSet<String> cols = new LinkedHashSet<>();
        for (Row r : rows) {
            cols.add(colKey.apply(r));
            data.computeIfAbsent(r.label(), k -> new LinkedHashMap<>()).put(colKey.apply(r), r.avgUs);
        }
        List<String> headers = new ArrayList<>();
        headers.add("Algorithm / graph");
        headers.addAll(cols);
        Table t = new Table(headers.toArray(new String[0]));
        for (var en : data.entrySet()) {
            List<Object> cells = new ArrayList<>();
            cells.add(en.getKey());
            for (String c : cols) cells.add(us(en.getValue().get(c)));
            t.add(cells.toArray());
        }
        return t;
    }

    static void writeAll(List<Row> required, List<Row> vsweep, List<Row> esweep, List<Row> ablation, List<Row> chain, Path dir) throws IOException {
        Files.createDirectories(dir);

        Table all = new Table("experiment", "graph", "V", "E", "algorithm", "graph_type", "avg_time_us", "mst_weight",
                "edge_exams", "comparisons", "dsu_finds", "dsu_unions", "dsu_parent_steps", "pq_pushes", "pq_pops_or_visits", "relaxations", "info");
        for (List<Row> list : List.of(required, vsweep, esweep, ablation, chain))
            for (Row r : list)
                all.add(r.experiment, r.graph, r.v, r.e, r.algorithm, r.graphType, us(r.avgUs), r.weight < 0 ? "" : r.weight,
                        r.edgeExams, r.comparisons, r.dsuFinds, r.dsuUnions, r.dsuSteps, r.pqPush, r.pqPop, r.relax, r.info);
        all.writeCsv(dir.resolve("all_results.csv"));

        save(pivot(required, r -> r.graph + " (V=" + r.v + ", E=" + r.e + ")"), dir, "summary_required_sizes");
        save(pivot(vsweep, r -> "V=" + r.v), dir, "sweep_vertices");
        save(pivot(esweep, r -> "E=" + r.e), dir, "sweep_edges");

        Table mst = new Table("Graph", "V", "E", "Algorithm", "MST weight", "Avg time (us)", "Edge exams", "Comparisons",
                "DSU find calls", "DSU union calls", "DSU parent steps", "PQ pushes", "PQ pops");
        for (Row r : required) if (r.algorithm.equals("Kruskal") || r.algorithm.equals("Prim"))
            mst.add(r.graph, r.v, r.e, r.algorithm, r.weight, us(r.avgUs), r.edgeExams, r.comparisons,
                    r.dsuFinds == 0 ? "-" : r.dsuFinds, r.dsuUnions == 0 ? "-" : r.dsuUnions, r.dsuFinds == 0 ? "-" : r.dsuSteps,
                    r.algorithm.equals("Prim") ? r.pqPush : "-", r.algorithm.equals("Prim") ? r.pqPop : "-");
        save(mst, dir, "mst_kruskal_vs_prim");

        Table ab = new Table("Graph", "V", "E", "DSU variant", "MST weight", "Avg time (us)", "DSU find calls", "DSU union calls", "DSU parent steps");
        for (Row r : ablation) ab.add(r.graph, r.v, r.e, r.graphType, r.weight, us(r.avgUs), r.dsuFinds, r.dsuUnions, r.dsuSteps);
        save(ab, dir, "dsu_ablation_kruskal");
        Table ch = new Table("Scenario", "DSU variant", "Avg time (us)", "find calls", "union calls", "parent steps");
        for (Row r : chain) ch.add(r.graph, r.graphType, us(r.avgUs), r.dsuFinds, r.dsuUnions, r.dsuSteps);
        save(ch, dir, "dsu_chain_worst_case");

        Table topo = new Table("Graph", "V", "E", "Input type", "Result", "Vertices ordered", "Avg time (us)", "Edge exams", "Vertices popped");
        for (Row r : required) if (r.algorithm.startsWith("TopoSort"))
            topo.add(r.graph, r.v, r.e, r.graphType, r.info.contains("isDag=true") ? "valid order found" : "cycle detected",
                     field(r.info, "ordered"), us(r.avgUs), r.edgeExams, r.pqPop);
        save(topo, dir, "topological_sort");

        Table scc = new Table("Graph", "V", "E", "Input type", "#SCCs", "Largest SCC", "Avg time (us)", "Edge exams", "Vertex visits");
        for (Row r : required) if (r.algorithm.startsWith("SCC"))
            scc.add(r.graph, r.v, r.e, r.graphType, field(r.info, "sccs"), field(r.info, "largest"), us(r.avgUs), r.edgeExams, r.pqPop);
        save(scc, dir, "scc");
        Table sccScale = new Table("V", "E", "Avg time (us)", "us per (V+E)", "#SCCs", "Largest SCC");
        for (Row r : vsweep) if (r.algorithm.startsWith("SCC") && r.graphType.equals("directed cyclic"))
            sccScale.add(r.v, r.e, us(r.avgUs), String.format(Locale.ROOT, "%.4f", r.avgUs / (r.v + r.e)), field(r.info, "sccs"), field(r.info, "largest"));
        save(sccScale, dir, "scc_scaling");

        Table dj = new Table("Graph", "V", "E", "Input type", "Reachable from source", "Avg time (us)", "Edge exams", "Relaxations", "PQ pushes", "PQ pops", "Comparisons");
        for (Row r : required) if (r.algorithm.equals("Dijkstra"))
            dj.add(r.graph, r.v, r.e, r.graphType, field(r.info, "reachable"), us(r.avgUs), r.edgeExams, r.relax, r.pqPush, r.pqPop, r.comparisons);
        save(dj, dir, "dijkstra");
    }

    static String field(String info, String key) {
        for (String part : info.split(";")) if (part.startsWith(key + "=")) return part.substring(key.length() + 1);
        return "";
    }

    public static void main(String[] args) throws IOException {
        Path out = Paths.get(args.length > 0 ? args[0] : "results", "tables");
        System.out.println("JVM: " + System.getProperty("java.vm.name") + " " + System.getProperty("java.version")
                + " | seed=" + BASE_SEED + " | warm-up runs=" + WARMUP + " | timed repetitions=" + REPS);

        System.out.println("JVM warm-up ...");
        for (int i = 0; i < 30; i++) {
            Graph u = GraphGenerator.connectedUndirected(200, 1000, i, MAX_WEIGHT);
            Graph d = GraphGenerator.randomDirected(200, 1000, i, MAX_WEIGHT);
            for (int k = 0; k < 20; k++) {
                sink += Kruskal.run(u).totalWeight + Prim.run(u).totalWeight;
                sink += TopologicalSort.run(d).order.size() + SCC.run(d).count() + Dijkstra.run(u, 0).reachableCount();
            }
        }

        for (Config c : REQUIRED) benchmarkAll("discarded_warmup", c.name(), c.v(), c.e());

        List<Row> required = new ArrayList<>();
        for (Config c : REQUIRED) {
            System.out.println("Required size " + c.name() + " (V=" + c.v() + ", E=" + c.e() + ")");
            required.addAll(benchmarkAll("required", c.name(), c.v(), c.e()));
        }
        List<Row> vsweep = new ArrayList<>();
        for (int v : VERTEX_SWEEP) {
            System.out.println("Vertex sweep V=" + v + ", E=" + 5 * v);
            vsweep.addAll(benchmarkAll("vertex_sweep", "V" + v, v, 5 * v));
        }
        List<Row> esweep = new ArrayList<>();
        for (int e : EDGE_SWEEP) {
            System.out.println("Edge sweep V=1000, E=" + e);
            esweep.addAll(benchmarkAll("edge_sweep", "E" + e, 1000, e));
        }
        System.out.println("DSU ablation ...");
        List<Row> ablation = dsuAblation();
        System.out.println("DSU chain micro-benchmark ...");
        List<Row> chain = dsuChain(10000);

        writeAll(required, vsweep, esweep, ablation, chain, out);
        System.out.println("Tables written to " + out.toAbsolutePath() + "  (checksum " + sink + ")");
    }
}
