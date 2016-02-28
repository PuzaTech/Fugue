package com.hongliangjie.fugue.topicmodeling;



/**
 * Created by liangjie on 10/29/14.
 */
public interface TopicModel {
    void Train();
    void Test();
    void SetMessage(Message m);
    Message GetMessage();
}
