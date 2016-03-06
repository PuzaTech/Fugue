package com.hongliangjie.fugue.serialization;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liangjie on 10/29/14.
 */
public class Document {

    private String docId;
    private List<Feature> features = new ArrayList<Feature>();

    public List<Feature> getFeatures(){
        return features;
    }

    public void setFeatures(List<Feature> f){
        features = f;
    }

    @Override
    public String toString() {
        return  docId + ' ' + Integer.toString(features.size());
    }
    public String getDocId() {
        return docId;
    }
    public void setDocId(String docId) {
        this.docId = docId;
    }
}
