package com.hongliangjie.fugue.topicmodeling;

import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by liangjie on 10/28/14.
 */
public class MainEntrance {

    private static final Logger LOGGER = LogManager.getLogger("FUGUE-TOPICMODELING");

    private static Options createOptions(){
        Option inputFileOption = OptionBuilder.withArgName("inputFile").hasArg().withDescription("the input file").create("input_file");
        Option modelFileOption = OptionBuilder.withArgName("modelFile").hasArg().withDescription("the model file").create("model_file");
        Option taskOption = OptionBuilder.withArgName("task").hasArg().withDescription("the task to perform").create("task");
        Option topicsOption = OptionBuilder.withArgName("topics").hasArg().withDescription("the number of topics to be discovered").create("topics");
        Option iterOption = OptionBuilder.withArgName("iters").hasArg().withDescription("the number of iterations to perform").create("iters");
        Option topKOption = OptionBuilder.withArgName("topk").hasArg().withDescription("the number of docs to read").create("topk");

        Option helpOption = new Option("help", "print out help message");

        Options options = new Options();
        options.addOption(inputFileOption);
        options.addOption(modelFileOption);
        options.addOption(taskOption);
        options.addOption(topicsOption);
        options.addOption(helpOption);
        options.addOption(iterOption);
        options.addOption(topKOption);

        return options;
    }

    private static Message parseOptions(Options options, String[] args){
        // create the parser
        Message cmd = new Message();
        CommandLineParser parser = new GnuParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);
            if (line.hasOption("help")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("fugue-topicmodeling", options);
            } else {
                if (line.hasOption("input_file")) {
                    cmd.SetParam("inputFile", line.getOptionValue("input_file"));
                    LOGGER.info("INPUT FILE:" + cmd.GetParam("inputFile").toString());
                }
                if (line.hasOption("model_file")) {
                    cmd.SetParam("modelFile", line.getOptionValue("model_file"));
                    LOGGER.info("MODEL FILE:" + cmd.GetParam("modelFile").toString());
                }
                if (line.hasOption("task")) {
                    cmd.SetParam("task", line.getOptionValue("task"));
                    LOGGER.info("TASK:" + cmd.GetParam("task").toString());
                }
                if (line.hasOption("topics")) {
                    cmd.SetParam("topics", Integer.parseInt(line.getOptionValue("topics")));
                    LOGGER.info("TOPICS:" + Integer.toString((Integer)cmd.GetParam("topics")));
                }
                if (line.hasOption("iters")) {
                    cmd.SetParam("iters", Integer.parseInt(line.getOptionValue("iters")));
                    LOGGER.info("ITERS:" + Integer.toString((Integer)cmd.GetParam("iters")));
                }
                if (line.hasOption("topk")) {
                    cmd.SetParam("topk", Integer.parseInt(line.getOptionValue("topk")));
                    LOGGER.info("TOPK:" + Integer.toString((Integer)cmd.GetParam("topk")));
                }
            }
        } catch (ParseException exp) {
            // oops, something went wrong
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
            System.exit(0);
        }
        return cmd;
    }

    public static void main(String[] args) {

        LOGGER.info("Start Application");

        Options options = MainEntrance.createOptions();
        Message cmd = MainEntrance.parseOptions(options, args);

        TopicModels m = new TopicModels(cmd);
        m.PerformTask();

        LOGGER.info("Complete Application");
    }

}
