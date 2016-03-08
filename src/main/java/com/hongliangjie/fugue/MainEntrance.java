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

    private static Options createOptions(){

        Options options = new Options();

        Option inputFileOption = Option.builder().longOpt("inputFile").desc("the input file").hasArg().argName("inputFile").build();
        Option modelFileOption = Option.builder().longOpt("modelFile").desc("the model file").hasArg().argName("modelFile").build();
        Option taskOption = Option.builder().longOpt("task").desc("the task to perform").hasArg().argName("task").build();
        Option topicsOption = Option.builder().longOpt("topics").desc("the number of topics to be discovered").hasArg().argName("topics").build();
        Option iterOption = Option.builder().longOpt("iters").desc("the number of iterations to perform").hasArg().argName("iters").build();
        Option topKOption = Option.builder().longOpt("topk").desc("the number of docs to read").hasArg().argName("topk").build();
        Option LDASamplerOption = Option.builder().longOpt("LDASampler").desc("the sampler to use for LDA").hasArg().argName("LDASampler").build();
        Option randomOption = Option.builder().longOpt("random").desc("the random number generator").hasArg().argName("random").build();



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

        return options;
    }

    private static Message parseOptions(Options options, String[] args){
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
                    LOGGER.info("INPUT FILE:" + cmd.getParam("inputFile").toString());
                }
                if (line.hasOption("modelFile")) {
                    cmd.setParam("modelFile", line.getOptionValue("modelFile"));
                    LOGGER.info("MODEL FILE:" + cmd.getParam("modelFile").toString());
                }
                if (line.hasOption("task")) {
                    cmd.setParam("task", line.getOptionValue("task"));
                    LOGGER.info("TASK:" + cmd.getParam("task").toString());
                }
                if (line.hasOption("topics")) {
                    cmd.setParam("topics", Integer.parseInt(line.getOptionValue("topics")));
                    LOGGER.info("TOPICS:" + Integer.toString((Integer)cmd.getParam("topics")));
                }
                if (line.hasOption("iters")) {
                    cmd.setParam("iters", Integer.parseInt(line.getOptionValue("iters")));
                    LOGGER.info("ITERS:" + Integer.toString((Integer)cmd.getParam("iters")));
                }
                if (line.hasOption("topk")) {
                    cmd.setParam("topk", Integer.parseInt(line.getOptionValue("topk")));
                    LOGGER.info("TOPK:" + Integer.toString((Integer)cmd.getParam("topk")));
                }
                if (line.hasOption("LDASampler")) {
                    cmd.setParam("LDASampler", line.getOptionValue("LDASampler"));
                    LOGGER.info("LDASampler:" + cmd.getParam("LDASampler").toString());
                }
                if (line.hasOption("random")) {
                    cmd.setParam("random", line.getOptionValue("random"));
                    LOGGER.info("random:" + cmd.getParam("random").toString());
                }
                cmd.setParam("saveModel", 1);
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
        if (cmd != null) {
            TopicModelDriver m = new TopicModelDriver(cmd);
            m.performTask();
        }
        else{
            LOGGER.info("Failed.");
        }

        LOGGER.info("Complete Application");
    }

}
