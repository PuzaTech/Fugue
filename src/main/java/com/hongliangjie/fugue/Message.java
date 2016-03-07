package com.hongliangjie.fugue;

import java.util.Hashtable;

/**
 * Created by liangjie on 10/29/14.
 */
public class Message {

    protected Hashtable<String, Object> msgContainer;

    public Message(){
        msgContainer = new Hashtable<String, Object>();
        msgContainer.put("inputFile", "");
        msgContainer.put("modelFile", "");
        msgContainer.put("task", "train");
        msgContainer.put("topicNum", 1);
        msgContainer.put("iterNum", 1);
        msgContainer.put("topK", -1);
    }

    public void setParam(String key, Object value){
        msgContainer.put(key, value);
    }

    public Object getParam(String key){
        if (msgContainer.containsKey(key)) {
            return msgContainer.get(key);
        }
        else{
            return null;
        }
    }
}
