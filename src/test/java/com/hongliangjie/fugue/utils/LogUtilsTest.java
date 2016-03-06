package com.hongliangjie.fugue.utils;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by hongliangjie on 3/4/2016.
 */
public class LogUtilsTest {

    @Test
    public void testLogAddExp() throws Exception {
        Double logprob1 = Math.log(1e-50);
        Double logprob2 = Math.log(2.5e-50);
        Double logprob12 = LogUtils.logSumTwo(logprob1, logprob2);
        Double prob12 = Math.exp(logprob12);

        Double e1 = Math.abs(logprob12 - (-113.87649168120691));
        Double e2 = Math.abs(prob12 - (3.5000000000000057e-50));
        assertEquals("Testing logSumTwo 1", true, e1 < 1e-10);
        assertEquals("Testing logSumTwo 2", true, e2 < 1e-10);
    }

    @Test
    public void testLogSumAll() throws Exception {
        Double[] x = new Double[3];
        x[0] = Math.log(0.3); x[1] = Math.log(0.05); x[2] = Math.log(0.65);
        Double logprob13 = LogUtils.logSumAll(x);
        Double prob13 = Math.exp(logprob13);

        Double e1 = Math.abs(logprob13 - (-5.55111512313e-17));
        Double e2 = Math.abs(prob13 - (1.0));
        assertEquals("Testing logSumAll 1", true, e1 < 1e-10);
        assertEquals("Testing logSumAll 2", true, e2 < 1e-10);
    }
}