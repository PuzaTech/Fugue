package com.hongliangjie.fugue.topicmodeling;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liangjie on 10/29/14.
 */
public class Document {

    private String doc_id;
    private List<Feature> features = new ArrayList<Feature>();

    public Document(){

    }

    public List<Feature> getFeatures(){
        return features;
    }

    public void setFeatures(List<Feature> f){
        features = f;
    }

    @Override
    public String toString() {
        return  doc_id + ' ' + Integer.toString(features.size());
    }

    public String getDoc_id() {
        return doc_id;
    }

    public void setDoc_id(String doc_id) {
        this.doc_id = doc_id;
    }
}
