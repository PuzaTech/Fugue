package com.hongliangjie.fugue.topicmodeling;

/**
 * Created by liangjie on 10/30/14.
 */
public class Feature {
    private String feature_type;
    private String feature_name;
    private Double feature_value;

    public Feature(){

    }

    public String getFeature_type(){
        return feature_type;
    }

    public String getFeature_name(){
        return feature_name;
    }

    public Double getFeature_value(){
        return feature_value;
    }
}
