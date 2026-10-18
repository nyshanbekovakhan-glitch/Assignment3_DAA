| Algorithm / graph | Small (V=10, E=30) | Medium (V=100, E=500) | Large (V=1000, E=5000) |
|---|---|---|---|
| Kruskal / undirected connected | 0.27 | 9.43 | 132.04 |
| Prim / undirected connected | 0.47 | 13.23 | 225.58 |
| TopoSort (Kahn) / DAG | 0.11 | 1.43 | 23.15 |
| TopoSort (Kahn) / directed cyclic | 0.03 | 0.31 | 3.69 |
| SCC (Tarjan) / directed cyclic | 0.38 | 5.20 | 70.38 |
| SCC (Tarjan) / DAG | 0.38 | 4.47 | 49.76 |
| Dijkstra / undirected connected | 0.35 | 7.93 | 110.62 |
| Dijkstra / directed random | 0.19 | 4.66 | 69.72 |
