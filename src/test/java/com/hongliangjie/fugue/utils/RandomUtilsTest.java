package com.hongliangjie.fugue.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by liangjie on 3/7/16.
 */
public class RandomUtilsTest {

    @Test
    public void testNativeRandom() throws Exception {
        double r1 = new RandomUtils().nextDouble();
        assertEquals("Testing Random", true, (r1 >= 0.0) && (r1 <= 1.0));
        double d1 = new RandomUtils(1).nextDouble();
        double e1 = Math.abs(d1 - 0.7322219172863654);
        assertEquals("Testing Deterministic Random Double", true, e1 < 1e-10);
        double d2 = new RandomUtils(1).nextDouble();
        double e2 = Math.abs(d2 - 0.7322219172863654);
        assertEquals("Testing Deterministic Random Double", true, e2 < 1e-10);
        int i1 = new RandomUtils(1).nextInt(5);
        assertEquals("Testing Deterministic Random Int", 2, i1);
        int i2 = new RandomUtils(1).nextInt(5);
        assertEquals("Testing Deterministic Random Int", 2, i2);
    }
}