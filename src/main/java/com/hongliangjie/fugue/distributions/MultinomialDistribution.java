package com.hongliangjie.fugue.distributions;

import com.hongliangjie.fugue.utils.LogUtils;
import com.hongliangjie.fugue.utils.MathExp;
import com.hongliangjie.fugue.utils.MathLog;

/**
 * Created by liangjie on 3/3/16.
 */
public class MultinomialDistribution extends DiscreteDistribution {

    protected double[] _accumulatedWeights;
    protected int _K;
    protected MathLog mathLog;
    protected MathExp mathExp;
    protected LogUtils logU;
    protected Sampler s;
    protected int comparisons = 0;

    public int getComparisons(){
        return comparisons;
    }

    public MultinomialDistribution(int K){
        this(K, new MathLog(), new MathExp(), "normal");
    }


    public MultinomialDistribution(int K, MathLog m, MathExp e, String method){
        _accumulatedWeights = new double[K];
        _K = K;
        mathLog = m;
        mathExp = e;
        logU = new LogUtils(m, e);

        if ("normal".equals(method)){
            s = new NormalSampler();
        }
        else if ("log".equals(method)){
            s = new LogSampler();
        }
        else if ("binary".equals(method)){
            s = new NormalBinarySearchSampler();
        }
    }

    protected abstract class Sampler{
        public abstract int sample(double uniformRV);
        public abstract double[] setWeights(double[] weights);
    }

    protected class NormalBinarySearchSampler extends NormalSampler{
        @Override
        public int sample(double uniformRV){
            double u = uniformRV * _accumulatedWeights[_K - 1];
            int lower = 0;
            int upper = _accumulatedWeights.length - 1;
            while (lower <= upper){
                int mid = lower + (upper - lower) / 2;
                comparisons += 1;
                if((_accumulatedWeights[mid] - u) > 0){
                    upper = mid - 1;
                }
                else{
                    lower = mid + 1;
                }
            }
            return lower;
        }
    }

    protected class NormalSampler extends Sampler{


        @Override
        public int sample(double uniformRV) {
            double u = uniformRV * _accumulatedWeights[_K - 1];

            int index = -1;
            for (index = 0; index < _K; index++) {
                comparisons += 1;
                if (u < _accumulatedWeights[index])
                    break;
            }
            return index;
        }

        @Override
        public double[] setWeights(double[] weights) {
            for(int i = 0; i < weights.length; i ++){
                if (i == 0) {
                    _accumulatedWeights[i] = weights[i];
                }
                else{
                    _accumulatedWeights[i] = _accumulatedWeights[i - 1] + weights[i];
                }
            }
            return _accumulatedWeights;
        }
    }

    protected class LogSampler extends Sampler{

        @Override
        public int sample(double uniformRV) {
            // log( (0,1)*N ) -> log(0, 1) + log N where N is the normalization factor
            double u = mathLog.compute(uniformRV) + _accumulatedWeights[_K - 1];
            int index = -1;
            for (index = 0; index < _K; index++){
                comparisons += 1;
                if (u < _accumulatedWeights[index]){
                    break;
                }
            }
            return index;
        }

        @Override
        public double[] setWeights(double[] weights) {
            for(int i = 0; i < weights.length; i++){
                if (i == 0) {
                    _accumulatedWeights[i] = weights[i];
                }
                else{
                    _accumulatedWeights[i] = logU.logSumTwo(_accumulatedWeights[i-1], weights[i]);
                }
            }
            return _accumulatedWeights;
        }
    }

    public int dimensions(){
        return _K;
    }

    public double[] setProbabilities(double[] prob){
        return s.setWeights(prob);
    }

    @Override
    public int sample(double uniformRV) {
        return s.sample(uniformRV);
    }

}
