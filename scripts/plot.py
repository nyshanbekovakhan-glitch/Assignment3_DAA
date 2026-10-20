#!/usr/bin/env python3
"""Creates all plots from results/tables/all_results.csv  (requires: matplotlib).
Usage: python3 scripts/plot.py [results_dir]"""
import csv
import sys
from collections import defaultdict
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

root = Path(sys.argv[1] if len(sys.argv) > 1 else "results")
rows = list(csv.DictReader(open(root / "tables" / "all_results.csv", encoding="utf-8")))
plots = root / "plots"
plots.mkdir(parents=True, exist_ok=True)

def sel(exp):
    return [r for r in rows if r["experiment"] == exp]

def series(exp, xkey):
    """label -> sorted list of (x, avg_time_us)"""
    d = defaultdict(list)
    for r in sel(exp):
        d[f'{r["algorithm"]} / {r["graph_type"]}'].append((int(r[xkey]), float(r["avg_time_us"])))
    return {k: sorted(v) for k, v in d.items()}

STYLE = {
    "Kruskal / undirected connected": ("tab:blue", "o", "-"),
    "Prim / undirected connected": ("tab:orange", "s", "-"),
    "TopoSort (Kahn) / DAG": ("tab:green", "^", "-"),
    "TopoSort (Kahn) / directed cyclic": ("tab:green", "v", "--"),
    "SCC (Tarjan) / directed cyclic": ("tab:red", "D", "-"),
    "SCC (Tarjan) / DAG": ("tab:red", "d", "--"),
    "Dijkstra / undirected connected": ("tab:purple", "P", "-"),
    "Dijkstra / directed random": ("tab:purple", "X", "--"),
}

def time_plot(exp, xkey, xlabel, title, fname, loglog=True):
    fig, ax = plt.subplots(figsize=(8, 5.2))
    for label, pts in series(exp, xkey).items():
        c, m, ls = STYLE[label]
        ax.plot([p[0] for p in pts], [p[1] for p in pts], marker=m, color=c, linestyle=ls, label=label)
    if loglog:
        ax.set_xscale("log"); ax.set_yscale("log")
    ax.set_xlabel(xlabel); ax.set_ylabel("average execution time (microseconds)")
    ax.set_title(title); ax.grid(True, which="both", alpha=0.3); ax.legend(fontsize=8)
    fig.tight_layout(); fig.savefig(plots / fname, dpi=150); plt.close(fig)

time_plot("required", "V", "number of vertices V", "Execution time vs number of vertices (Small, Medium, Large)", "time_vs_vertices.png")
time_plot("required", "E", "number of edges E", "Execution time vs number of edges (Small, Medium, Large)", "time_vs_edges.png")

req = sel("required")
names = ["Small", "Medium", "Large"]
fig, axes = plt.subplots(1, 3, figsize=(14, 4.4))
w = 0.38
for ax, (key, title) in zip(axes, [("avg_time_us", "average time (us)"), ("edge_exams", "edge examinations"), ("comparisons", "comparisons (sort / heap)")]):
    for i, algo in enumerate(["Kruskal", "Prim"]):
        vals = [float(next(r for r in req if r["algorithm"] == algo and r["graph"] == n)[key]) for n in names]
        ax.bar([x + (i - 0.5) * w for x in range(3)], vals, w, label=algo, color=["tab:blue", "tab:orange"][i])
    ax.set_xticks(range(3)); ax.set_xticklabels(names); ax.set_yscale("log"); ax.set_title(title); ax.grid(True, axis="y", alpha=0.3)
axes[0].legend()
fig.suptitle("Kruskal vs Prim (log scale)")
fig.tight_layout(); fig.savefig(plots / "kruskal_vs_prim.png", dpi=150); plt.close(fig)

ab = sel("dsu_ablation")
variants = ["no optimisation", "path compression only", "union by rank only", "path compression + union by rank"]
cfgs = ["Small", "Medium", "Large", "XL"]
fig, axes = plt.subplots(1, 2, figsize=(14, 4.8))
w = 0.2
for ax, (key, title) in zip(axes, [("dsu_parent_steps", "DSU parent-pointer steps"), ("avg_time_us", "Kruskal average time (us)")]):
    for i, v in enumerate(variants):
        vals = [float(next(r for r in ab if r["graph"] == c and r["graph_type"] == v)[key]) for c in cfgs]
        ax.bar([x + (i - 1.5) * w for x in range(4)], vals, w, label=v)
    ax.set_xticks(range(4)); ax.set_xticklabels(["Small\n10/30", "Medium\n100/500", "Large\n1000/5000", "XL\n10000/50000"])
    ax.set_yscale("log"); ax.set_title(title); ax.grid(True, axis="y", alpha=0.3)
axes[0].legend(fontsize=8)
fig.suptitle("Effect of path compression and union by rank inside Kruskal (log scale)")
fig.tight_layout(); fig.savefig(plots / "dsu_ablation.png", dpi=150); plt.close(fig)

print("plots written to", plots)
