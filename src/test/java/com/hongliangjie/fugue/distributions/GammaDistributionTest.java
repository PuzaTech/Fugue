package com.hongliangjie.fugue.distributions;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by hongliangjie on 3/4/2016.
 */
public class GammaDistributionTest {

    @Test
    public void testSample() throws Exception {
        GammaDistribution g = new GammaDistribution();
        Double sample = g.sample();
        assertEquals("PlaceHolder Test", true, 1 == 1);
    }
}