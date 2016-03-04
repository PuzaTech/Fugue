package com.hongliangjie.fugue.distributions;

/**
 * Created by liangjie on 3/3/16.
 */
public class MultinomialDistribution extends DiscreteDistribution {

    protected Double[] _accumulatedProbabilities;
    protected Integer _K;

    public MultinomialDistribution(int K){
        _accumulatedProbabilities = new Double[K];
        _K = K;
    }

    public Integer Dimensions(){
        return _K;
    }

    public Double[] SetProbabilities(Double[] prob){
        for(int i = 0; i < prob.length; i ++){
            if (i > 0) {
                _accumulatedProbabilities[i] = _accumulatedProbabilities[i - 1] + prob[i];
            }
            else{
                _accumulatedProbabilities[i] = prob[i];
            }
        }
        return _accumulatedProbabilities;
    }

    @Override
    public int Sample(double uniformRV) {
        Double u = uniformRV * _accumulatedProbabilities[_K - 1];
        int index = -1;
        for (index = 0; index < _K; index++) {
            if (u < _accumulatedProbabilities[index])
                break;
        }
        return index;
    }
}
