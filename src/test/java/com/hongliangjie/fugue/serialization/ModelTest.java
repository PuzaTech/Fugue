package com.hongliangjie.fugue.serialization;

import com.hongliangjie.fugue.Message;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by hongliangjie on 3/5/2016.
 */
public class ModelTest {

    private class testModel extends Model{

        private Message internalMsg;

        @Override
        public void setParameters(Message msg) {
            internalMsg = msg;
        }

        @Override
        public Message getParameters() {
            return internalMsg;
        }
    }

    @Test
    public void testGetModelId() throws Exception {
        Model m = new testModel();
        m.setModelId("abc");
        assertEquals("Testing getModelId", "abc", m.getModelId());

    }

    @Test
    public void testSetModelId() throws Exception {
        Model m = new testModel();
        m.setModelId("abc");
        assertEquals("Testing getModelId", "abc", m.getModelId());
    }
}