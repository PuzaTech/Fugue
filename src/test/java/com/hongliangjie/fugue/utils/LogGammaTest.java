package com.hongliangjie.fugue.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by liangjie on 3/1/16.
 */
public class LogGammaTest {

    @Test
    public void testLogGamma() throws Exception {
        double y1_diff = Math.abs(LogGamma.logGamma(1.0) - 0.0);
        assertEquals("Testing lnGamma(1.0)", true, y1_diff < 10e-10);
        double y2_diff = Math.abs(LogGamma.logGamma(0.5) - 0.5723649429246999);
        assertEquals("Testing lnGamma(0.5)", true, y2_diff < 10e-10);
        double y3_diff = Math.abs(LogGamma.logGamma(0.3) - 1.0957979948180756);
        assertEquals("Testing lnGamma(0.3)", true, y3_diff < 10e-10);
    }
}