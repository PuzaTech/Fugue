package com.hongliangjie.fugue;

import org.apache.commons.cli.Options;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by liangjie on 3/11/16.
 */
public class MainEntranceTest {

    protected class MainTestClass extends MainEntrance{
        public Options getOptions(){
            return MainEntrance.createOptions();
        }

        public Message getCMD(String[] args){
            return MainEntrance.parseOptions(MainEntrance.createOptions(), args);
        }
    }

    @Test
    public void testMain() throws Exception {
        MainTestClass main = new MainTestClass();
        Options options = main.getOptions();

        assertEquals("inputFile", true, options.getOption("inputFile") != null);
        assertEquals("modelFile", true, options.getOption("modelFile") != null);
        assertEquals("task", true, options.getOption("task") != null);
        assertEquals("topics", true, options.getOption("topics") != null);
        assertEquals("iters", true, options.getOption("iters") != null);
        assertEquals("topk", true, options.getOption("topk") != null);
        assertEquals("LDASampler", true, options.getOption("LDASampler") != null);
        assertEquals("LDAHyperOpt", true, options.getOption("LDAHyperOpt") != null);
        assertEquals("random", true, options.getOption("random") != null);
        assertEquals("exp", true, options.getOption("exp") != null);
        assertEquals("log", true, options.getOption("log") != null);
        assertEquals("saveModel", true, options.getOption("saveModel") != null);

        Message cmdMsg = main.getCMD(null);
        assertEquals("inputFile", true, cmdMsg.getParam("inputFile") != null);
        assertEquals("modelFile", true, cmdMsg.getParam("modelFile") != null);
        assertEquals("task", true, cmdMsg.getParam("task") != null);
        assertEquals("topics", true, cmdMsg.getParam("topics") != null);
        assertEquals("iters", true, cmdMsg.getParam("iters") != null);
        assertEquals("topk", true, cmdMsg.getParam("topk") != null);
        assertEquals("LDASampler", true, cmdMsg.getParam("LDASampler") != null);
        assertEquals("LDAHyperOpt", true, cmdMsg.getParam("LDAHyperOpt") != null);
        assertEquals("sliceSamples", true, cmdMsg.getParam("sliceSamples") != null);
        assertEquals("sliceSteps", true, cmdMsg.getParam("sliceSteps") != null);
        assertEquals("sliceIters", true, cmdMsg.getParam("sliceIters") != null);
        assertEquals("random", true, cmdMsg.getParam("random") != null);
        assertEquals("exp", true, cmdMsg.getParam("exp") != null);
        assertEquals("log", true, cmdMsg.getParam("log") != null);
        assertEquals("saveModel", true, cmdMsg.getParam("saveModel") != null);

        MainEntrance.main(null);
    }
}