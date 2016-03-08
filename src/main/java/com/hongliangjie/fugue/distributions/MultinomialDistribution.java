package com.hongliangjie.fugue.distributions;

import com.hongliangjie.fugue.utils.LogUtils;
import com.hongliangjie.fugue.utils.MathLog;

/**
 * Created by liangjie on 3/3/16.
 */
public class MultinomialDistribution extends DiscreteDistribution {

    protected double[] _accumulatedWeights;
    protected int _K;
    protected MathLog mathLog;

    public MultinomialDistribution(int K, MathLog m){
        _accumulatedWeights = new double[K];
        _K = K;
        mathLog = m;
    }

    public int dimensions(){
        return _K;
    }

    public double[] setProbabilities(double[] prob){
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
        double u = uniformRV * _accumulatedWeights[_K - 1];

        int index = -1;
        for (index = 0; index < _K; index++) {
            if (u < _accumulatedWeights[index])
                break;
        }
        return index;
    }

    public double[] setLogProbabilities(double[] logProb, LogUtils u){
        for(int i = 0; i < logProb.length; i++){
            if (i == 0) {
                _accumulatedWeights[i] = logProb[i];
            }
            else{
                _accumulatedWeights[i] = u.logSumTwo(_accumulatedWeights[i-1], logProb[i]);
            }
        }
        return _accumulatedWeights;
    }

    public int logSample(double uniformRV){
        // log( (0,1)*N ) -> log(0, 1) + log N where N is the normalization factor
        double u = mathLog.compute(uniformRV) + _accumulatedWeights[_K - 1];
        int index = -1;
        for (index = 0; index < _K; index++){
            if (u < _accumulatedWeights[index]){
                break;
            }
        }
        return index;
    }
}
