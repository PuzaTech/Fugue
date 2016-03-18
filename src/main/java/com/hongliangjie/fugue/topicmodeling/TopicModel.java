package com.hongliangjie.fugue.topicmodeling;

import com.hongliangjie.fugue.Message;

/**
 * Created by liangjie on 10/29/14.
 */
public abstract class TopicModel {
    public abstract void train(int start, int end);
    public abstract void test(int start, int end);
    public abstract void train();
    public abstract void test();
    public abstract void setMessage(Message m);
    public abstract Message getMessage();
}
