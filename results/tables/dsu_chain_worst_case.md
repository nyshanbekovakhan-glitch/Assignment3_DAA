| Scenario | DSU variant | Avg time (us) | find calls | union calls | parent steps |
|---|---|---|---|---|---|
| chain n=10000 | no optimisation | 65642.19 | 29998 | 9999 | 49995000 |
| chain n=10000 | path compression only | 62.91 | 29998 | 9999 | 19997 |
| chain n=10000 | union by rank only | 71.44 | 29998 | 9999 | 19997 |
| chain n=10000 | path compression + union by rank | 52.45 | 29998 | 9999 | 19997 |
