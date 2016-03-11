package com.hongliangjie.fugue;

import org.apache.commons.cli.Option;
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

        Option inputFileOption = options.getOption("inputFile");
        assertEquals("inputFile", true, inputFileOption != null);
        Option modelFileOption = options.getOption("modelFile");
        assertEquals("modelFile", true, modelFileOption != null);
        Option taskOption = options.getOption("task");
        assertEquals("task", true, taskOption != null);
        Option topicsOption = options.getOption("topics");
        assertEquals("topics", true, topicsOption != null);
        Option itersOption = options.getOption("iters");
        assertEquals("iters", true, itersOption != null);
        Option topkOption = options.getOption("topk");
        assertEquals("topk", true, topkOption != null);
        Option LDASamplerOption = options.getOption("LDASampler");
        assertEquals("LDASampler", true, LDASamplerOption != null);
        Option randomOption = options.getOption("random");
        assertEquals("random", true, randomOption != null);
        Option expOption = options.getOption("exp");
        assertEquals("exp", true, expOption != null);
        Option logOption = options.getOption("log");
        assertEquals("log", true, logOption != null);
        Option saveModelOption = options.getOption("saveModel");
        assertEquals("saveModel", true, saveModelOption != null);

        Message cmdMsg = main.getCMD(null);
        assertEquals("inputFile", true, cmdMsg.getParam("inputFile") != null);
        assertEquals("modelFile", true, cmdMsg.getParam("modelFile") != null);
        assertEquals("task", true, cmdMsg.getParam("task") != null);
        assertEquals("topics", true, cmdMsg.getParam("topics") != null);
        assertEquals("iters", true, cmdMsg.getParam("iters") != null);
        assertEquals("topk", true, cmdMsg.getParam("topk") != null);
        assertEquals("LDASampler", true, cmdMsg.getParam("LDASampler") != null);
        assertEquals("random", true, cmdMsg.getParam("random") != null);
        assertEquals("exp", true, cmdMsg.getParam("exp") != null);
        assertEquals("log", true, cmdMsg.getParam("log") != null);
        assertEquals("saveModel", true, cmdMsg.getParam("saveModel") != null);

        MainEntrance.main(null);
    }
}