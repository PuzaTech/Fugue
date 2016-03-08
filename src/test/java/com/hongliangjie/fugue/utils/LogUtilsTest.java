package com.hongliangjie.fugue.utils;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by hongliangjie on 3/4/2016.
 */
public class LogUtilsTest {

    private void logAddExp(LogUtils u, double error){
        double logprob1 = Math.log(1e-50);
        double logprob2 = Math.log(2.5e-50);
        double logprob12 = u.logSumTwo(logprob1, logprob2);
        double prob12 = Math.exp(logprob12);

        double e1 = Math.abs(logprob12 - (-113.87649168120691));
        double e2 = Math.abs(prob12 - (3.5000000000000057e-50));

        assertEquals("Testing logSumTwo 1", true, e1 < error);
        assertEquals("Testing logSumTwo 2", true, e2 < error);
    }

    @Test
    public void testLogAddExp() throws Exception {
        LogUtils u1 = new LogUtils(new MathLog(0), new MathExp(0));
        logAddExp(u1, 1e-10);
        LogUtils u2 = new LogUtils(new MathLog(1), new MathExp(1));
        logAddExp(u2, 1e-10);

        LogUtils u3 = new LogUtils(new MathLog(2), new MathExp(2));

        double logprob1 = Math.log(1e-50);
        double logprob2 = Math.log(2.5e-50);
        double logprob12 = u3.logSumTwo(logprob1, logprob2);
        double prob12 = Math.exp(logprob12);

        double e1 = Math.abs((logprob12 - (-113.87649168120691))/(-113.87649168120691));
        double e2 = Math.abs((prob12 - (3.5000000000000057e-50))/3.5000000000000057e-50);

        assertEquals("Testing logSumTwo 1", true, e1 < 1e-4);
        assertEquals("Testing logSumTwo 2", true, e2 < 1e-2);


    }

    @Test
    public void testLogSumAll() throws Exception {
        LogUtils u = new LogUtils(new MathLog(1), new MathExp(1));
        double[] x = new double[3];
        x[0] = Math.log(0.3); x[1] = Math.log(0.05); x[2] = Math.log(0.65);
        double logprob13 = u.logSumAll(x);
        double prob13 = Math.exp(logprob13);

        double e1 = Math.abs(logprob13 - (-5.55111512313e-17));
        double e2 = Math.abs(prob13 - (1.0));


        assertEquals("Testing logSumAll 1", true, e1 < 1e-10);
        assertEquals("Testing logSumAll 2", true, e2 < 1e-10);
    }
}