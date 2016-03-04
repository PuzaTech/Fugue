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
        Double[] accu_p = theta.SetProbabilities(p);
        assertEquals("Testing Accumulate Distribution:", true, (accu_p[2] >= accu_p[1]) && (accu_p[1] >= accu_p[0]));
    }

    private int[] _sampleHistogram(MultinomialDistribution dist, int  SampleSize) {
        if (SampleSize <= 0)
            SampleSize = 1;
        int[] _localHistogram = new int[dist.Dimensions()];
        for (int i=0; i < SampleSize; i++){
            int currentIndex = dist.Sample(RandomUtils.NativeRandom());
            _localHistogram[currentIndex] ++;
        }
        return _localHistogram;
    }

    @Test
    public void testSample() throws Exception {
        MultinomialDistribution theta = new MultinomialDistribution(3);
        Double[] p = new Double[3];
        p[0] = 0.0; p[1] = 0.0; p[2] = 1.0;
        theta.SetProbabilities(p);

        int oneSample = theta.Sample(RandomUtils.NativeRandom());
        assertEquals("Testing Sample's Boundaries:", true, ((oneSample < p.length) && (oneSample >= 0)));

        int[] h1 = _sampleHistogram(theta, 1000);

        assertEquals("Testing Extreme Samples 0:", 0, h1[0]);
        assertEquals("Testing Extreme Samples 1:", 0, h1[1]);
        assertEquals("Testing Extreme Samples 2:", 1000, h1[2]);

        p[0] = 0.2; p[1] = 0.7; p[2] = 0.1;
        theta.SetProbabilities(p);

        int N = 1000000;
        int[] h2 = _sampleHistogram(theta, N);

        System.out.println(Math.abs(h2[0]/(double)N));
        System.out.println(Math.abs(h2[1]/(double)N));
        System.out.println(Math.abs(h2[2]/(double)N));

        double e1 = Math.abs(h2[0]/(double)N - p[0]);
        double e2 = Math.abs(h2[1]/(double)N - p[1]);
        double e3 = Math.abs(h2[2]/(double)N - p[2]);

        assertEquals("Testing Error 1:", true, e1 < 0.001);
        assertEquals("Testing Error 2:", true, e2 < 0.001);
        assertEquals("Testing Error 3:", true, e3 < 0.001);

    }
}