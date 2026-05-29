# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Fugue is a Java-based topic modeling package focused on LDA (Latent Dirichlet Allocation) with collapsed Gibbs sampling. The Java core handles training and inference; Python scripts wrap the build process and handle data preprocessing.

## Commands

### Build
```sh
python Fugue.py                          # runs gradle build + gradle fatJar
gradle build                             # compile and run tests
gradle fatJar                            # produce the fat JAR
```

### Run tests
```sh
gradle test                              # all tests
gradle test --tests "com.hongliangjie.fugue.distributions.GammaDistributionTest"  # single test class
```

### Train / test LDA on AP corpus
```sh
python Fugue.py --task train                                  # ProfileAPTrain (docs 0–1909)
python Fugue.py --task train --profile ProfileAPTrainHyper    # with slice sampling hyperopt
python Fugue.py --task test  --profile ProfileAPTest          # perplexity on held-out docs
```

### Run the JAR directly
```sh
java -Xmx8192m -jar build/libs/fugue-topicmodeling-all-0.2.jar \
  --task train --inputFile examples/data/ap.json \
  --modelFile examples/models/model.out.json \
  --topics 100 --iters 1000 --LDASampler binary
```

> **Note**: `Fugue.py` hardcodes `fugue-topicmodeling-all-0.1.jar` but `build.gradle` produces `0.2`. Update the path in `Fugue.py:cmdBuilder.run()` if the jar is not found.

## Architecture

### Request flow
`MainEntrance` (CLI parsing) → `TopicModelDriver` → `DataReader` (load JSONL) → `LDA.setMessage()` → `LDA.train()` / `LDA.test()`

### Key classes
| File | Purpose |
|------|---------|
| `MainEntrance.java` | CLI entry point; parses args into a `Message` and delegates to `TopicModelDriver` |
| `Message.java` | Typeless key-value parameter bag used throughout; all config and runtime state travels in this object |
| `io/DataReader.java` | Reads JSONL input, retains only `TOKEN`-type features, populates `docs` key in `Message` |
| `topicmodeling/TopicModelDriver.java` | Loads data, sets model message, dispatches `train` or `test` |
| `topicmodeling/LDA/LDA.java` | Core algorithm: Gibbs sampling, multi-chain model pools, hyperparameter optimization, model save/load |
| `serialization/Document.java`, `Feature.java` | Input data structures (JSONL ↔ Java) |
| `serialization/Model.java`, `LDAModel.java` | Model serialization to/from JSONL via Gson |
| `distributions/` | `MultinomialDistribution` (normal/binary/log variants), `GammaDistribution` |
| `utils/` | `LogGamma`, `MathExp`, `MathLog` (switchable implementations), `RandomUtils` |

### Input data format
JSONL — one document per line:
```json
{"docId": 0, "features": [{"featureName": "word", "featureType": "TOKEN", "featureValue": 1.0}, ...]}
```
`DataReader` silently discards any feature whose `featureType` is not `TOKEN`.

### Model persistence (multi-chain)
`LDA` maintains a `modelPools` list of `ModelCountainer` objects. During training only pool 0 is used. At test time, `multipleModels=1` loads all 10 checkpoint files (e.g. `model.ap.0.json` … `model.ap.9.json`) and averages their perplexity estimates. Models are saved every 10 iterations, rotating through `TOTAL_SAVES=10` slots (ring buffer).

### Sampler options (`--LDASampler`)
- `normal` — standard unnormalized probabilities
- `binary` — binary search sampling (recommended; used in AP profiles)
- `log` — log-space probabilities

### Hyperparameter optimization (`--LDAHyperOpt`)
- `none` — fixed α (50/K per topic) and β (0.01 per term)
- `slice` — slice sampling after burn-in (every 25 iterations after iteration 100)

## Python utilities

- `Fugue.py` — build/run wrapper (Python 3). The `ProfileParam.getParams()` method uses `iteritems()` (Python 2 only) and is not called in normal build/train/test flows; the actual command building bypasses it.
- `src/main/python/tm.py` — data preprocessing (build vocabulary, parse raw JSON → JSONL) and post-processing (average topic distributions across multiple model files). **This script still uses Python 2 syntax** (`iteritems()`, codec wrapping on stdin/stdout) and will not run under Python 3.
