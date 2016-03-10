package com.hongliangjie.fugue.distributions;

import com.hongliangjie.fugue.utils.MathExp;
import com.hongliangjie.fugue.utils.MathLog;
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
        double[] p = new double[3];
        p[0] = 0.2; p[1] = 0.5; p[2] = 0.3;
        double[] accu_p = theta.setProbabilities(p);
        assertEquals("Testing Accumulate Distribution:", true, (accu_p[2] >= accu_p[1]) && (accu_p[1] >= accu_p[0]));

        MultinomialDistribution theta1 = new MultinomialDistribution(3, new MathLog(0), new MathExp(1), "normal");
        MultinomialDistribution theta2 = new MultinomialDistribution(3, new MathLog(1), new MathExp(0), "log");
        double[] p1 = new double[3];
        p1[0] = 0.2; p1[1] = 0.5; p1[2] = 0.3;
        double[] logp = new double[3];
        logp[0] = Math.log(0.2); logp[1] = Math.log(0.5); logp[2] = Math.log(0.3);
        double[] accu_p2 = theta1.setProbabilities(p1);
        double[] accu_logp = theta2.setProbabilities(logp);
        for (int i = 0; i < 3; i++) {
            double e = Math.abs(accu_p2[i] - Math.exp(accu_logp[i]));
            assertEquals("Testing Accumulate Distribution:", true, e < 1e-10);
        }
    }

    private int[] sampleHistogram(MultinomialDistribution dist, int  sampleSize) {
        RandomUtils r = new RandomUtils(1);
        int localSampleSize = 1;
        if (sampleSize <= 0) {
            localSampleSize = 1;
        }
        else{
            localSampleSize = sampleSize;
        }
        int[] localHistogram = new int[dist.dimensions()];
        for (int i=0; i < localSampleSize; i++){
            int currentIndex = dist.sample(r.nextDouble());
            localHistogram[currentIndex] ++;
        }
        return localHistogram;
    }

    @Test
    public void testSample() throws Exception {
        MultinomialDistribution theta = new MultinomialDistribution(3, new MathLog(0), new MathExp(0), "normal");
        MultinomialDistribution thetaBin = new MultinomialDistribution(3, new MathLog(0), new MathExp(0), "binary");
        double[] p = new double[3];
        p[0] = 0.0; p[1] = 0.0; p[2] = 1.0;
        theta.setProbabilities(p);
        thetaBin.setProbabilities(p);

        double randomRV = new RandomUtils(0).nextDouble();
        int oneSample = theta.sample(randomRV);
        int oneSample2 = thetaBin.sample(randomRV);
        assertEquals("Testing Sample's Boundaries:", true, ((oneSample < p.length) && (oneSample >= 0)));
        assertEquals("Testing Sample's Boundaries:", true, ((oneSample2 < p.length) && (oneSample2 >= 0)));
        assertEquals("Testing Equal", true, oneSample == oneSample2);

        int[] h1 = sampleHistogram(theta, 1000);

        assertEquals("Testing Extreme Samples 0:", 0, h1[0]);
        assertEquals("Testing Extreme Samples 1:", 0, h1[1]);
        assertEquals("Testing Extreme Samples 2:", 1000, h1[2]);

        p[0] = 0.33; p[1] = 0.33; p[2] = 0.34;
        theta.setProbabilities(p);
        thetaBin.setProbabilities(p);

        int N = 1000000;
        int[] h2 = sampleHistogram(theta, N);
        int[] hBin = sampleHistogram(thetaBin, N);
        for(int k = 0; k < h2.length; k ++){
            assertEquals("Binary Equal to Linear", true, h2[k] == hBin[k]);
        }

        System.out.println(theta.getComparisons());
        System.out.println(thetaBin.getComparisons());


        double e1 = Math.abs(h2[0]/(double)N - p[0]);
        double e2 = Math.abs(h2[1]/(double)N - p[1]);
        double e3 = Math.abs(h2[2]/(double)N - p[2]);

        assertEquals("Testing Error 1:", true, e1 < 0.001);
        assertEquals("Testing Error 2:", true, e2 < 0.001);
        assertEquals("Testing Error 3:", true, e3 < 0.001);


        MultinomialDistribution theta1 = new MultinomialDistribution(3, new MathLog(0), new MathExp(0), "normal");
        MultinomialDistribution theta2 = new MultinomialDistribution(3, new MathLog(0), new MathExp(0), "log");
        MultinomialDistribution theta3 = new MultinomialDistribution(3, new MathLog(0), new MathExp(0), "binary");

        double[] p2 = new double[3];
        p2[0] = 1.2; p2[1] = 2.3; p2[2] = 11.5;
        double[] logp = new double[3];
        for(int i = 0; i < p2.length; i++)
            logp[i] = Math.log(p2[i]);
        theta1.setProbabilities(p2);
        theta2.setProbabilities(logp);
        theta3.setProbabilities(p2);

        N = 10000;
        RandomUtils ru = new RandomUtils(1);
        for (int i = 0; i < N; i++) {
            double r = ru.nextDouble();
            int i1 = theta1.sample(r);
            int i2 = theta2.sample(r);
            int i3 = theta3.sample(r);
            assertEquals("Testing Log Sampling Accuracy", i1, i2);
            assertEquals("Testing Log Sampling Accuracy", i1, i3);
        }
        System.out.println(theta1.getComparisons());
        System.out.println(theta2.getComparisons());
        System.out.println(theta3.getComparisons());

    }
}