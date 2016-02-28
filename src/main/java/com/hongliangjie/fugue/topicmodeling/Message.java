package com.hongliangjie.fugue.topicmodeling;

import java.util.Hashtable;

/**
 * Created by liangjie on 10/29/14.
 */
public class Message {

    public Hashtable<String, Object> msgContainer;

    public Message(){
        msgContainer = new Hashtable<String, Object>();
        msgContainer.put("inputFile", "");
        msgContainer.put("modelFile", "");
        msgContainer.put("task", "train");
        msgContainer.put("topicNum", 1);
        msgContainer.put("iterNum", 1);
        msgContainer.put("topK", -1);
    }

    public void SetParam(String key, Object value){
        msgContainer.put(key, value);
    }

    public Object GetParam(String key){
        return msgContainer.get(key);
    }
}
