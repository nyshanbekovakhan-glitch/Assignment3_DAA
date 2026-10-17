| Graph | V | E | Input type | Result | Vertices ordered | Avg time (us) | Edge exams | Vertices popped |
|---|---|---|---|---|---|---|---|---|
| Small | 10 | 30 | DAG | valid order found | 10 | 0.16 | 60 | 10 |
| Small | 10 | 30 | directed cyclic | cycle detected | 0 | 0.03 | 30 | 0 |
| Medium | 100 | 500 | DAG | valid order found | 100 | 1.84 | 1000 | 100 |
| Medium | 100 | 500 | directed cyclic | cycle detected | 0 | 0.41 | 500 | 0 |
| Large | 1000 | 5000 | DAG | valid order found | 1000 | 24.60 | 10000 | 1000 |
| Large | 1000 | 5000 | directed cyclic | cycle detected | 7 | 4.47 | 5039 | 7 |
