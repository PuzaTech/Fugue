package com.hongliangjie.fugue.topicmodeling.lda;

import com.hongliangjie.fugue.Message;
import com.hongliangjie.fugue.serialization.Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by liangjie on 3/6/16.
 */
public class LDAModel extends Model{
    private double[] alpha = new double[1];
    private Map<String, Double> beta = new HashMap<String, Double>();
    private Map<String, int[]> wordTopicCounts = new HashMap<String, int[]>(); // how many times a term appears in a topic
    private int[] topicCounts = new int[1]; // total number of terms that are assigned to a topic

    @Override
    public void setParameters(Message msg) {
        // setup alpha
        alpha = (double[])msg.getParam("alpha");
        // setup beta
        List<Double> b = (List<Double>)msg.getParam("beta");
        HashMap<Integer, String> wordsInvertedIndex = (HashMap<Integer, String>)msg.getParam("invertedIndex");
        for(int v=0; v < b.size(); v++){
            String feature_name = wordsInvertedIndex.get(v);
            beta.put(feature_name, b.get(v));
        }
        List<int[]> w = (List<int[]>)msg.getParam("wordTopicCounts");
        for(int v=0; v < w.size(); v++){
            String feature_name = wordsInvertedIndex.get(v);
            wordTopicCounts.put(feature_name, w.get(v));
        }
        topicCounts = (int[])msg.getParam("topicCounts");
    }

    @Override
    public Message getParameters() {
        Message msg = new Message();
        msg.setParam("alpha", alpha);
        List<Double> b = new ArrayList<Double>();
        for (Map.Entry<String, Double> entry : beta.entrySet()){
            Double value = entry.getValue();
            b.add(value);
        }
        msg.setParam("beta", b);
        //msg.setParam("wordTopicCounts", wordTopicCounts);
        msg.setParam("topicCounts", topicCounts);
        return msg;
    }
}
