package com.hongliangjie.fugue.distributions;

import com.hongliangjie.fugue.utils.LogUtils;

/**
 * Created by liangjie on 3/3/16.
 */
public class MultinomialDistribution extends DiscreteDistribution {

    protected Double[] _accumulatedWeights;
    protected Integer _K;

    public MultinomialDistribution(int K){
        _accumulatedWeights = new Double[K];
        _K = K;
    }

    public Integer dimensions(){
        return _K;
    }

    public Double[] setProbabilities(Double[] prob){
        for(int i = 0; i < prob.length; i ++){
            if (i == 0) {
                _accumulatedWeights[i] = prob[i];
            }
            else{
                _accumulatedWeights[i] = _accumulatedWeights[i - 1] + prob[i];
            }
        }
        return _accumulatedWeights;
    }

    @Override
    public int sample(double uniformRV) {
        Double u = uniformRV * _accumulatedWeights[_K - 1];

        int index = -1;
        for (index = 0; index < _K; index++) {
            if (u < _accumulatedWeights[index])
                break;
        }
        return index;
    }

    public Double[] setLogProbabilities(Double[] logProb){
        for(int i = 0; i < logProb.length; i++){
            if (i == 0) {
                _accumulatedWeights[i] = logProb[i];
            }
            else{
                _accumulatedWeights[i] = LogUtils.logSumTwo(_accumulatedWeights[i-1], logProb[i]);
            }
        }
        return _accumulatedWeights;
    }

    public int logSample(double uniformRV){
        // log( (0,1)*N ) -> log(0, 1) + log N where N is the normalization factor
        Double u = Math.log(uniformRV) + _accumulatedWeights[_K - 1];
        int index = -1;
        for (index = 0; index < _K; index++){
            if (u < _accumulatedWeights[index]){
                break;
            }
        }
        return index;
    }
}
