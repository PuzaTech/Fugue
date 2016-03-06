package com.hongliangjie.fugue.distributions;

import com.hongliangjie.fugue.utils.RandomUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by liangjie on 3/3/16.
 */
public class MultinomialDistributionTest {

    @Test
    public void testSet() throws Exception {
        MultinomialDistribution theta = new MultinomialDistribution(3);
        Double[] p = new Double[3];
        p[0] = 0.2; p[1] = 0.5; p[2] = 0.3;
        Double[] accu_p = theta.setProbabilities(p);
        assertEquals("Testing Accumulate Distribution:", true, (accu_p[2] >= accu_p[1]) && (accu_p[1] >= accu_p[0]));
    }

    @Test
    public void testSetLogProbabilities() throws Exception {
        MultinomialDistribution theta1 = new MultinomialDistribution(3);
        MultinomialDistribution theta2 = new MultinomialDistribution(3);
        Double[] p = new Double[3];
        p[0] = 0.2; p[1] = 0.5; p[2] = 0.3;
        Double[] logp = new Double[3];
        logp[0] = Math.log(0.2); logp[1] = Math.log(0.5); logp[2] = Math.log(0.3);
        Double[] accu_p = theta1.setProbabilities(p);
        Double[] accu_logp = theta2.setLogProbabilities(logp);
        for (int i = 0; i < 3; i++) {
            Double e = Math.abs(accu_p[i] - Math.exp(accu_logp[i]));
            assertEquals("Testing Accumulate Distribution:", true, e < 1e-10);
        }
    }

    private int[] sampleHistogram(MultinomialDistribution dist, int  sampleSize) {
        int localSampleSize = 1;
        if (sampleSize <= 0) {
            localSampleSize = 1;
        }
        else{
            localSampleSize = sampleSize;
        }
        int[] localHistogram = new int[dist.dimensions()];
        for (int i=0; i < localSampleSize; i++){
            int currentIndex = dist.sample(RandomUtils.NativeRandom());
            localHistogram[currentIndex] ++;
        }
        return localHistogram;
    }

    @Test
    public void testSample() throws Exception {
        MultinomialDistribution theta = new MultinomialDistribution(3);
        Double[] p = new Double[3];
        p[0] = 0.0; p[1] = 0.0; p[2] = 1.0;
        theta.setProbabilities(p);

        int oneSample = theta.sample(RandomUtils.NativeRandom());
        assertEquals("Testing Sample's Boundaries:", true, ((oneSample < p.length) && (oneSample >= 0)));

        int[] h1 = sampleHistogram(theta, 1000);

        assertEquals("Testing Extreme Samples 0:", 0, h1[0]);
        assertEquals("Testing Extreme Samples 1:", 0, h1[1]);
        assertEquals("Testing Extreme Samples 2:", 1000, h1[2]);

        p[0] = 0.2; p[1] = 0.7; p[2] = 0.1;
        theta.setProbabilities(p);

        int N = 1000000;
        int[] h2 = sampleHistogram(theta, N);



        double e1 = Math.abs(h2[0]/(double)N - p[0]);
        double e2 = Math.abs(h2[1]/(double)N - p[1]);
        double e3 = Math.abs(h2[2]/(double)N - p[2]);

        assertEquals("Testing Error 1:", true, e1 < 0.001);
        assertEquals("Testing Error 2:", true, e2 < 0.001);
        assertEquals("Testing Error 3:", true, e3 < 0.001);

    }



    @Test
    public void testLogSample() throws Exception {
        MultinomialDistribution theta1 = new MultinomialDistribution(3);
        MultinomialDistribution theta2 = new MultinomialDistribution(3);
        Double[] p = new Double[3];
        p[0] = 1.2; p[1] = 2.3; p[2] = 11.5;
        Double[] logp = new Double[3];
        for(int i = 0; i < p.length; i++)
            logp[i] = Math.log(p[i]);
        theta1.setProbabilities(p);
        theta2.setLogProbabilities(logp);

        int N = 10000;
        for (int i = 0; i < N; i++) {
            Double r = RandomUtils.NativeRandom();
            int i1 = theta1.sample(r);
            int i2 = theta2.logSample(r);
            assertEquals("Testing Log Sampling Accuracy", i1, i2);
        }

    }
}