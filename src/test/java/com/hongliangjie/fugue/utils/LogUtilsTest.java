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
        Double logprob12 = LogUtils.logAddExp(logprob1, logprob2);
        Double prob12 = Math.exp(logprob12);

        Double e1 = Math.abs(logprob12 - (-113.87649168120691));
        Double e2 = Math.abs(prob12 - (3.5000000000000057e-50));
        assertEquals("Testing logAddExp 1", true, e1 < 1e-10);
        assertEquals("Testing logAddExp 2", true, e2 < 1e-10);
    }
}