package com.hongliangjie.fugue;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by liangjie on 3/6/16.
 */
public class MessageTest {

    @Test
    public void testSetParam() throws Exception {
        Message msg = new Message();
        msg.setParam("abc", "def");
        assertEquals("Testing SetParam", "def", msg.getParam("abc"));
    }

    @Test
    public void testGetParam() throws Exception {
        Message msg = new Message();
        msg.setParam("abc", "def");
        assertEquals("Testing SetParam", "def", msg.getParam("abc"));
    }
}