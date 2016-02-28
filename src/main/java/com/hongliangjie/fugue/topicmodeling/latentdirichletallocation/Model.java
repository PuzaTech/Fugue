package com.hongliangjie.fugue.topicmodeling.latentdirichletallocation;

import com.hongliangjie.fugue.topicmodeling.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by liangjie on 10/31/14.
 */
public class Model {
    private double[] alpha = new double[1];
    private Map<String, Double> beta = new HashMap<String, Double>();
    private Map<String, int[]> wordTopicCounts = new HashMap<String, int[]>(); // how many times a term appears in a topic
    private int[] topicCounts = new int[1]; // total number of terms that are assigned to a topic


    public void setAlpha(double[] a, Message m){
        alpha = a;
    }

    public void setBeta(List<Double> b, Message m){
        HashMap<Integer, String> wordsInvertedIndex = (HashMap<Integer, String>)m.GetParam("invertedIndex");
        for(int v=0; v < b.size(); v++){
            String feature_name = wordsInvertedIndex.get(v);
            beta.put(feature_name, b.get(v));
        }
    }

    public void setWordTopicCounts(List<int[]> w, Message m){
        HashMap<Integer, String> wordsInvertedIndex = (HashMap<Integer, String>)m.GetParam("invertedIndex");
        for(int v=0; v < w.size(); v++){
            String feature_name = wordsInvertedIndex.get(v);
            wordTopicCounts.put(feature_name, w.get(v));
        }
    }

    public void setTopicCounts(int[] t){
        topicCounts = t;
    }
}
