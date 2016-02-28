#!/usr/bin/env bash

function train-ap {
    java -Xmx8192m \
	-jar build/libs/fugue-topicmodeling-all-0.1.jar \
        -input_file data/ap.json \
        -task train \
        -topics 100 \
        -iters 1000 \
        -model_file models/model.ap.json \
        -topk 100000
}

function topics-ap {
    python src/main/python/tm.py \
    --task topics \
    --model_file models/model.ap.0.json,models/model.ap.1.json,models/model.ap.2.json,models/model.ap.3.json,models/model.ap.4.json,models/model.ap.5.json,models/model.ap.6.json,models/model.ap.7.json,models/model.ap.8.json,models/model.ap.9.json \
    --output_file results/ap.average.topics
}

function build {
    gradle build
    gradle fatJar
}

FUNCTION="$1"
eval ${FUNCTION}