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
        Option randomOption = Option.builder().longOpt("random").desc("the random number generator").hasArg().argName("random").build();
        Option expOption = Option.builder().longOpt("exp").desc("the math exp function").hasArg().argName("exp").build();
        Option logOption = Option.builder().longOpt("log").desc("the math log function").hasArg().argName("log").build();
        Option saveModelOption = Option.builder().longOpt("saveModel").desc("automatically save models").hasArg().argName("saveModel").build();

        Option helpOption = new Option("help", "print out help message");

        options.addOption(inputFileOption);
        options.addOption(modelFileOption);
        options.addOption(taskOption);
        options.addOption(topicsOption);
        options.addOption(helpOption);
        options.addOption(iterOption);
        options.addOption(topKOption);
        options.addOption(LDASamplerOption);
        options.addOption(randomOption);
        options.addOption(expOption);
        options.addOption(logOption);
        options.addOption(saveModelOption);

        return options;
    }

    protected static Message parseOptions(Options options, String[] args){
        // create the parser
        Message cmd = new Message();
        CommandLineParser parser = new DefaultParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);
            if (line.hasOption("help")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("fugue-topicmodeling", options);
                cmd = null;
            } else {
                if (line.hasOption("inputFile")) {
                    cmd.setParam("inputFile", line.getOptionValue("inputFile"));
                }
                else{
                    cmd.setParam("inputFile", "NULL");
                }
                if (line.hasOption("modelFile")) {
                    cmd.setParam("modelFile", line.getOptionValue("modelFile"));
                }
                else{
                    cmd.setParam("modelFile", "NULL");
                }
                if (line.hasOption("task")) {
                    cmd.setParam("task", line.getOptionValue("task"));
                }
                else{
                    cmd.setParam("task", "NULL");
                }
                if (line.hasOption("topics")) {
                    cmd.setParam("topics", Integer.parseInt(line.getOptionValue("topics")));
                }
                else{
                    cmd.setParam("topics", 100);
                }
                if (line.hasOption("iters")) {
                    cmd.setParam("iters", Integer.parseInt(line.getOptionValue("iters")));
                }
                else{
                    cmd.setParam("iters", 1000);
                }
                if (line.hasOption("topk")) {
                    cmd.setParam("topk", Integer.parseInt(line.getOptionValue("topk")));
                }
                else{
                    cmd.setParam("topk", 100000);
                }
                if (line.hasOption("LDASampler")) {
                    cmd.setParam("LDASampler", line.getOptionValue("LDASampler"));
                }
                else{
                    cmd.setParam("LDASampler", "normal");
                }
                if (line.hasOption("random")) {
                    cmd.setParam("random", line.getOptionValue("random"));
                }
                else{
                    cmd.setParam("random", "native");
                }
                if (line.hasOption("log")) {
                    cmd.setParam("log", Integer.parseInt(line.getOptionValue("log")));
                }
                else{
                    cmd.setParam("log", 0);
                }

                if (line.hasOption("exp")) {
                    cmd.setParam("exp", Integer.parseInt(line.getOptionValue("exp")));
                }
                else {
                    cmd.setParam("exp", 0);
                }

                if (line.hasOption("saveModel")){
                    cmd.setParam("saveModel", Integer.parseInt(line.getOptionValue("saveModel")));
                }
                else {
                    cmd.setParam("saveModel", 1);
                }

                LOGGER.info("[CMD] INPUT FILE:" + cmd.getParam("inputFile").toString());
                LOGGER.info("[CMD] MODEL FILE:" + cmd.getParam("modelFile").toString());
                LOGGER.info("[CMD] TASK:" + cmd.getParam("task").toString());
                LOGGER.info("[CMD] TOPICS:" + Integer.toString((Integer)cmd.getParam("topics")));
                LOGGER.info("[CMD] ITERS:" + Integer.toString((Integer)cmd.getParam("iters")));
                LOGGER.info("[CMD] TOPK:" + Integer.toString((Integer)cmd.getParam("topk")));
                LOGGER.info("[CMD] LDA Sampler:" + cmd.getParam("LDASampler").toString());
                LOGGER.info("[CMD] Random Number Generator:" + cmd.getParam("random").toString());
                LOGGER.info("[CMD] Math Log:" + cmd.getParam("log").toString());
                LOGGER.info("[CMD] Math Exp:" + cmd.getParam("exp").toString());
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
