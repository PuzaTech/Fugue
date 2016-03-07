package com.hongliangjie.fugue.topicmodeling;

import com.hongliangjie.fugue.Message;
import com.hongliangjie.fugue.topicmodeling.LDA.LDA;
import com.hongliangjie.fugue.io.DataReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Created by liangjie on 10/29/14.
 */
public class TopicModelDriver {

    private static final Logger LOGGER = LogManager.getLogger("FUGUE-TOPICMODELING");

    private TopicModel model;
    private Message msg;

    public TopicModelDriver(Message args){
        model = new LDA();
        msg = args;
    }

    public void performTask(){

        DataReader r = new DataReader();
        LOGGER.info("Start to read documents.");
        try {
            msg = r.Read(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGGER.info("Finished reading documents.");

        model.setMessage(msg);

        String task = msg.getParam("task").toString();

        if(task.equals("train") == true){
            LOGGER.info("Start to train.");
            model.train();
            LOGGER.info("Finished training.");
        }
        else if (task.equals("test") == true){
            LOGGER.info("Start to test.");
            model.test();
            LOGGER.info("Finished testing.");
        }

    }
}