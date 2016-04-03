package com.hongliangjie.fugue;

import com.hongliangjie.fugue.topicmodeling.TopicModelDriver;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by liangjie on 10/28/14.
 */
public class MainEntrance {

    private static final Logger LOGGER = LogManager.getLogger("FUGUE-TOPICMODELING");

    protected static Options createOptions(){

        Options options = new Options();

        Option inputFileOption = Option.builder().longOpt("inputFile").desc("the input file").hasArg().argName("inputFile").build();
        Option modelFileOption = Option.builder().longOpt("modelFile").desc("the model file").hasArg().argName("modelFile").build();
        Option taskOption = Option.builder().longOpt("task").desc("the task to perform").hasArg().argName("task").build();
        Option topicsOption = Option.builder().longOpt("topics").desc("the number of topics to be discovered").hasArg().argName("topics").build();
        Option iterOption = Option.builder().longOpt("iters").desc("the number of iterations to perform").hasArg().argName("iters").build();
        Option topKOption = Option.builder().longOpt("topk").desc("the number of docs to read").hasArg().argName("topk").build();
        Option LDASamplerOption = Option.builder().longOpt("LDASampler").desc("the sampler to use for LDA").hasArg().argName("LDASampler").build();
        Option LDAHyperOptOption = Option.builder().longOpt("LDAHyperOpt").desc("the hyper-parameter optimizer").hasArg().argName("LDAHyperOpt").build();
        Option randomOption = Option.builder().longOpt("random").desc("the random number generator").hasArg().argName("random").build();
        Option expOption = Option.builder().longOpt("exp").desc("the math exp function").hasArg().argName("exp").build();
        Option logOption = Option.builder().longOpt("log").desc("the math log function").hasArg().argName("log").build();
        Option saveModelOption = Option.builder().longOpt("saveModel").desc("automatically save models").hasArg().argName("saveModel").build();
        Option multipleModelsOption = Option.builder().longOpt("multipleModels").desc("load multiple instances of test models").hasArg().argName("multipleModels").build();
        Option startOption = Option.builder().longOpt("start").desc("the starting index of documents").hasArg().argName("start").build();
        Option endOption = Option.builder().longOpt("end").desc("the ending index of documents").hasArg().argName("end").build();
        Option helpOption = new Option("help", "print out help message");

        options.addOption(inputFileOption);
        options.addOption(modelFileOption);
        options.addOption(taskOption);
        options.addOption(topicsOption);
        options.addOption(helpOption);
        options.addOption(iterOption);
        options.addOption(topKOption);
        options.addOption(LDASamplerOption);
        options.addOption(LDAHyperOptOption);
        options.addOption(randomOption);
        options.addOption(expOption);
        options.addOption(logOption);
        options.addOption(saveModelOption);
        options.addOption(multipleModelsOption);
        options.addOption(startOption);
        options.addOption(endOption);

        return options;
    }

    protected static Message defaultMessage(){
        Message cmd = new Message();
        cmd.setParam("inputFile", "NULL");
        cmd.setParam("modelFile", "NULL");
        cmd.setParam("task", "NULL");
        cmd.setParam("topics", "100");
        cmd.setParam("iters", "1000");
        cmd.setParam("topk", "100000");
        cmd.setParam("LDASampler", "normal");
        cmd.setParam("LDAHyperOpt", "none");
        cmd.setParam("random", "native");
        cmd.setParam("log", "0");
        cmd.setParam("exp", "0");
        cmd.setParam("saveModel", "1");
        cmd.setParam("multipleModels", "0");
        cmd.setParam("start", "0");
        cmd.setParam("end", "-1");
        cmd.setParam("sliceSamples", "10");
        cmd.setParam("sliceSteps", "1.0");
        cmd.setParam("sliceIters", "50");
        return cmd;
    }

    protected static Message parseOptions(Options options, String[] args) {
        // create the parser
        Message cmd = defaultMessage();
        CommandLineParser parser = new DefaultParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);
            if (line.hasOption("help")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("fugue-topicmodeling", options);
                cmd = null;
            } else {
                Option[] lineOptions = line.getOptions();
                for(int i = 0; i < lineOptions.length; i++){
                    cmd.setParam(lineOptions[i].getArgName(), lineOptions[i].getValue());
                }

                LOGGER.info("[CMD] INPUT FILE:" + cmd.getParam("inputFile").toString());
                LOGGER.info("[CMD] MODEL FILE:" + cmd.getParam("modelFile").toString());
                LOGGER.info("[CMD] TASK:" + cmd.getParam("task").toString());
                LOGGER.info("[CMD] TOPICS:" + cmd.getParam("topics").toString());
                LOGGER.info("[CMD] ITERS:" + cmd.getParam("iters").toString());
                LOGGER.info("[CMD] TOPK:" + cmd.getParam("topk").toString());
                LOGGER.info("[CMD] LDA Sampler:" + cmd.getParam("LDASampler").toString());
                LOGGER.info("[CMD] LDA Hyperparameter Optimization:" + cmd.getParam("LDAHyperOpt").toString());
                LOGGER.info("[CMD] LDA Slice Sampling SampleNums:" + cmd.getParam("sliceSamples").toString());
                LOGGER.info("[CMD] LDA Slice Sampling Steps:" + cmd.getParam("sliceSteps").toString());
                LOGGER.info("[CMD] LDA Slice Sampling Iterations:" + cmd.getParam("sliceIters").toString());
                LOGGER.info("[CMD] Random Number Generator:" + cmd.getParam("random").toString());
                LOGGER.info("[CMD] Math Log:" + cmd.getParam("log").toString());
                LOGGER.info("[CMD] Math Exp:" + cmd.getParam("exp").toString());
                LOGGER.info("[CMD] Start:" + cmd.getParam("start").toString());
                LOGGER.info("[CMD] End:" + cmd.getParam("end").toString());
            }
        } catch (ParseException exp) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
            cmd = null;
        }
        return cmd;
    }

    public static void main(String[] args) {

        LOGGER.info("Start Application");

        Options options = MainEntrance.createOptions();
        Message cmd = MainEntrance.parseOptions(options, args);
        if ("NULL".equals(cmd.getParam("task"))) {
            LOGGER.info("Failed.");
        }
        else{
            TopicModelDriver m = new TopicModelDriver(cmd);
            m.performTask();
        }

        LOGGER.info("Complete Application");
    }

}
