package com.hongliangjie.fugue.topicmodeling;

import com.hongliangjie.fugue.topicmodeling.latentdirichletallocation.*;
import com.hongliangjie.fugue.topicmodeling.reader.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by liangjie on 10/29/14.
 */
public class TopicModels {

    private static final Logger LOGGER = LogManager.getLogger("FUGUE-TOPICMODELING");

    TopicModel model;
    Message cmdArgs;

    public TopicModels(Message args){
        model = new LDA();
        cmdArgs = args;
    }

    public void PerformTask(){

        DataReader r = new DataReader();
        LOGGER.info("Start to read documents.");
        List<Document> docs = new ArrayList<Document>();
        try {
            cmdArgs = r.Read(cmdArgs);
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGGER.info("Finished reading documents.");

        model.SetMessage(cmdArgs);

        String task = cmdArgs.GetParam("task").toString();

        if(task.equals("train") == true){
            LOGGER.info("Start to train.");
            model.Train();
            LOGGER.info("Finished training.");
        }
        else if (task.equals("test") == true){
            LOGGER.info("Start to test.");
            model.Test();
            LOGGER.info("Finished testing.");
        }

    }
}