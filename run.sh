#!/usr/bin/env bash

function train-ap {
    java -Xmx8192m \
	-jar build/libs/fugue-topicmodeling-all-0.1.jar \
        -inputFile examples/data/ap.json \
        -task train \
        -topics 100 \
        -iters 1000 \
        -modelFile examples/models/model.ap.json \
        -topk 100000 \
        -LDASampler binary \
        -random deterministic
}

function train-ap-log {
    java -Xmx8192m \
	-jar build/libs/fugue-topicmodeling-all-0.1.jar \
        -inputFile examples/data/ap.json \
        -task train \
        -topics 100 \
        -iters 1000 \
        -modelFile examples/models/model.ap-log.json \
        -topk 100000 \
        -LDASampler log \
        -random deterministic \
        -exp 2 \
        -log 2
}

function topics-ap {
    PREFIX=examples/models/model.ap-log
    python src/main/python/tm.py \
    --task topics \
    --model_file $PREFIX.0.json,$PREFIX.1.json,$PREFIX.2.json,$PREFIX.3.json,$PREFIX.4.json,$PREFIX.5.json,$PREFIX.6.json,$PREFIX.7.json,$PREFIX.8.json,$PREFIX.9.json \
    --output_file examples/results/ap-log.average.topics
}

function build {
    gradle build
    gradle fatJar
}

FUNCTION="$1"
eval ${FUNCTION}