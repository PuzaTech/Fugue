# Fugue
[![Build Status](https://travis-ci.org/PuzaTech/Fugue.svg?branch=master)](https://travis-ci.org/PuzaTech/Fugue)
[![codecov.io](https://codecov.io/github/PuzaTech/Fugue/coverage.svg?branch=master)](https://codecov.io/github/PuzaTech/Fugue?branch=master)
[![Known Vulnerabilities](https://snyk.io/test/github/puzatech/fugue/badge.svg?targetFile=build.gradle)](https://snyk.io/test/github/puzatech/fugue?targetFile=build.gradle)
[![Code Climate](https://codeclimate.com/github/PuzaTech/Fugue/badges/gpa.svg)](https://codeclimate.com/github/PuzaTech/Fugue)
[![Codacy Badge](https://api.codacy.com/project/badge/grade/79ec17fe542e4f6792a522c7a9c374b4)](https://www.codacy.com/app/hongliangjie/Fugue)
[![GitHub license](http://dmlc.github.io/img/apache2.svg)](./LICENSE)
## A Topic Modeling Package
Fugue is a research oriented topic modeling package for text mining and machine learning practitioners, designed for understanding models and algorithms. The philosophy of the package is to include mature models and algorithms that represent the state-of-the-art of research in topic modeling.

### Models/Algorithms
* Latent Dirichlet allocation (LDA) with collapsed Gibbs sampling
  * Using "Estimate theta" method for computing perplexity in test documents.
  * Using multiple MCMC chains to average results. 
  * Slice sampling for hyper-parameter sampling
  * Optimizain methods for hyper-parameter tuning

### Roadmap
For 1.0, See [Detailed Plan](https://github.com/PuzaTech/Fugue/wiki/Fugue-1.0-Roadmap).

### Prerequisites
* Gradle
* JDK
* Python 2.7

The design of the package is to limit the prerequesite packages and therefore you can easily experiment new models with minimum environments.

### Platforms
* Mac/Linux

### How to Compile
```sh
$ python Fugue.py
```

### Run AP example
```sh
$ python Fugue.py --task train
```
