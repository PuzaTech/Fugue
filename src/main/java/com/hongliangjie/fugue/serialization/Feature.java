package com.hongliangjie.fugue.serialization;

/**
 * Created by liangjie on 10/30/14.
 */
public class Feature {
    private String featureType;
    private String featureName;
    private Double featureValue;


    public String getFeatureType(){
        return featureType;
    }
    public String getFeatureName(){
        return featureName;
    }
    public Double getFeatureValue(){
        return featureValue;
    }

    public void setFeatureType(String featureType) {
        this.featureType = featureType;
    }

    public void setFeatureName(String featureName) {
        this.featureName = featureName;
    }

    public void setFeatureValue(Double featureValue) {
        this.featureValue = featureValue;
    }
}
