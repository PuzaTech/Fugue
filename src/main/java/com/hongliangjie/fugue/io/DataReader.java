package com.hongliangjie.fugue.io;

import com.google.gson.Gson;
import com.hongliangjie.fugue.serialization.Document;
import com.hongliangjie.fugue.serialization.Feature;
import com.hongliangjie.fugue.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by liangjie on 10/29/14.
 */
public class DataReader {

    private static final Logger LOGGER = LogManager.getLogger("FUGUE-TOPICMODELING");

    public Message read(Message m) throws IOException {
        String inputFile = m.getParam("inputFile").toString();
        Integer top = Integer.parseInt(m.getParam("topk").toString());

        if (top == null)
            top = 1;

        Gson gson = new Gson();

        List<Document> docs = new ArrayList<Document>();
        HashMap<String, Integer> wordsForwardIndex = new HashMap<String, Integer>();
        HashMap<Integer, String> wordsInvertedIndex = new HashMap<Integer, String>();

        LOGGER.info("READ TOP K:" + Integer.toString(top));
        if(inputFile != null) {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "UTF8"));
            String line;
            while ((line = br.readLine()) != null) {
                Document raw_doc = gson.fromJson(line, Document.class);

                // prune document features to the ones we care about
                Document new_doc = new Document();
                new_doc.setDocId(raw_doc.getDocId());
                List<Feature> new_features = new ArrayList<Feature>();
                for (Feature f : raw_doc.getFeatures()) {
                    String feature_type = f.getFeatureType();
                    String feature_name = f.getFeatureName();
                    if (("TOKEN").equals(feature_type)) {
                        new_features.add(f);
                        if(!wordsForwardIndex.containsKey(feature_name)){
                            wordsForwardIndex.put(feature_name, wordsForwardIndex.size());
                            Integer word_index = wordsForwardIndex.get(feature_name);
                            wordsInvertedIndex.put(word_index, feature_name);
                        }
                    }
                }
                if(new_features.size() > 0) {
                    new_doc.setFeatures(new_features);
                    docs.add(new_doc);
                }
                if((docs.size() >= top) && (top > 0)) {
                    break;
                }
            }
            br.close();
            LOGGER.info("Total number of documents:" + docs.size());
            m.setParam("docs", docs);
            m.setParam("forwardIndex", wordsForwardIndex);
            m.setParam("invertedIndex", wordsInvertedIndex);
        }
        else{
            System.err.println("Cannot find input file.");
            return null;
        }

        return m;
    }
}
