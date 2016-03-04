package com.hongliangjie.fugue.topicmodeling.latentdirichletallocation;

import com.google.gson.Gson;
import com.hongliangjie.fugue.distributions.MultinomialDistribution;
import com.hongliangjie.fugue.topicmodeling.Document;
import com.hongliangjie.fugue.topicmodeling.Feature;
import com.hongliangjie.fugue.topicmodeling.Message;
import com.hongliangjie.fugue.topicmodeling.TopicModel;
import com.hongliangjie.fugue.utils.LogGamma;
import com.hongliangjie.fugue.utils.RandomUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * Created by liangjie on 10/29/14.
 */
public class LDA implements TopicModel {
    protected Map<String, Integer> wordForwardIndex;
    protected List<int[]> wordTopicCounts; // how many times a term appears in a topic
    protected int[] topicCounts; // total number of terms that are assigned to a topic
    protected List<int[]> docTopicBuffers; // !!! DENSE !!!
    protected List<List<Integer>> docTopicAssignments; // !!! DENSE !!!
    protected List<Document> internalDocs;
    protected double[] alpha;
    protected List<Double> beta;
    protected double betaSum;
    protected double alphaSum;
    protected Message cmdArg;

    protected int TOPIC_NUM;
    protected int MAX_ITER;
    protected int CURRENT_ITER;
    protected int INTERVAL;
    protected int TOTAL_TOKEN;
    protected int SAVED;

    protected static final Logger LOGGER = LogManager.getLogger("FUGUE-TOPICMODELING");

    public LDA() {
        TOPIC_NUM = 1;
        MAX_ITER = 250;
        INTERVAL = 5;
        TOTAL_TOKEN = 0;
        SAVED = 0;
    }

    public void SetMessage(Message m) {
        cmdArg = m;
    }

    public Message GetMessage(){
        return cmdArg;
    }

    private void InitModel() {

        TOPIC_NUM = (Integer) cmdArg.GetParam("topics");
        MAX_ITER = (Integer) cmdArg.GetParam("iters");
        internalDocs = (List<Document>) cmdArg.GetParam("docs");
        wordForwardIndex = (HashMap<String, Integer>) cmdArg.GetParam("forwardIndex"); // get word forward index

        LOGGER.info("Start to initialize model");
        LOGGER.info("Topic Num:" + TOPIC_NUM);
        LOGGER.info("ForwardIndex Size:" + wordForwardIndex.size());

        /* initialize all model parameters with fixed sizes */
        wordTopicCounts = new ArrayList<int[]>(wordForwardIndex.size());
        beta = new ArrayList<Double>(wordForwardIndex.size());
        for (int i = 0; i < wordForwardIndex.size(); i++) {
            int[] topicCounts = new int[TOPIC_NUM];
            wordTopicCounts.add(topicCounts);
            beta.add(0.01);
            betaSum += beta.get(i);
        }
        docTopicBuffers = new ArrayList<int[]>(internalDocs.size());
        docTopicAssignments = new ArrayList<List<Integer>>(internalDocs.size());
        for (int i = 0; i < internalDocs.size(); i++) {
            int[] topicBuffer = new int[TOPIC_NUM];
            docTopicBuffers.add(topicBuffer);
            docTopicAssignments.add(new ArrayList<Integer>());
        }
        topicCounts = new int[TOPIC_NUM];
        alpha = new double[TOPIC_NUM];
        TOTAL_TOKEN = 0;

        for (int k = 0; k < TOPIC_NUM; k++)
            alpha[k] = 50.0 / TOPIC_NUM;
        alphaSum = 50.0; // (50.0/TOPIC_NUM) * TOPIC_NUM

        for (int d = 0; d < internalDocs.size(); d++) {
            for (Feature f : internalDocs.get(d).getFeatures()) {
                String feature_name = f.getFeature_name();
                Integer feature_index = wordForwardIndex.get(feature_name);
                // we randomly assign a topic for this token
                int topic = ThreadLocalRandom.current().nextInt(0, TOPIC_NUM);
                docTopicAssignments.get(d).add(topic);
                docTopicBuffers.get(d)[topic]++;
                wordTopicCounts.get(feature_index)[topic]++;
                topicCounts[topic]++;
                TOTAL_TOKEN++;
            }

        }

        LOGGER.info("Term Num:" + wordTopicCounts.size());
        LOGGER.info("alphaSum:" + alphaSum);
        LOGGER.info("betaSum:" + betaSum);
        LOGGER.info("Finished initializing model");
    }

    private void CountsCheck(boolean printOut) {
        int topic_sum = 0;

        for (int k = 0; k < TOPIC_NUM; k++) {
            topic_sum += topicCounts[k];
        }

        if (topic_sum != TOTAL_TOKEN) {
            System.err.println("topicCounts are not consistent with total token:" + topic_sum + " " + TOTAL_TOKEN);
            System.exit(0);
        }
        if (printOut)
            LOGGER.info("topicCounts are consistent with total token:" + topic_sum + " " + TOTAL_TOKEN);

        int term_topic_sum = 0;
        int[] topic_sums = new int[TOPIC_NUM];
        for (int v = 0; v < wordTopicCounts.size(); v++) {
            int[] c = wordTopicCounts.get(v);
            for (int k = 0; k < c.length; k++) {
                term_topic_sum += c[k];
                topic_sums[k] += c[k];
            }
        }

        if (term_topic_sum != TOTAL_TOKEN) {
            System.err.println("wordTopicCounts are not consistent with total token:" + term_topic_sum + " " + TOTAL_TOKEN);
            System.exit(0);
        }
        if (printOut)
            LOGGER.info("wordTopicCounts are consistent with total token:" + term_topic_sum + " " + TOTAL_TOKEN);

        for (int k = 0; k < TOPIC_NUM; k++) {
            if (topic_sums[k] != topicCounts[k]) {
                System.err.println("topic_sums are not consistent with topicCounts:" + k + " " + topic_sums[k] + " " + topicCounts[k]);
                System.exit(0);
            }
            if (printOut)
                LOGGER.info("topic_sums are consistent with topicCounts:" + k + " " + topic_sums[k] + " " + topicCounts[k]);
        }

        int[] document_agg_topics = new int[TOPIC_NUM];
        for (int d = 0; d < internalDocs.size(); d++) {
            List<Integer> docTopicAssignment = docTopicAssignments.get(d);
            int[] docTopicBuffer = docTopicBuffers.get(d);
            int pos = internalDocs.get(d).getFeatures().size();
            int total_topic_sum = 0;
            for (int k = 0; k < TOPIC_NUM; k++) {
                total_topic_sum += docTopicBuffer[k];
            }
            if (total_topic_sum != pos) {
                System.err.println("The total_topic_sum is not consistent with number of tokens:" + d + " " + total_topic_sum + " " + pos);
                System.exit(0);
            }

            for (Integer z : docTopicAssignment) {
                document_agg_topics[z] += 1;
            }

            if (docTopicAssignment.size() != pos) {
                System.err.println("The total_tokens is not consistent with number of tokens:" + d + " " + docTopicAssignment.size() + " " + pos);
                System.exit(0);
            }
        }
        if (printOut)
            LOGGER.info("Document level counts are consistent.");

        for (int k = 0; k < TOPIC_NUM; k++) {
            if (document_agg_topics[k] != topicCounts[k]) {
                System.err.println("The aggregated topic assignment is not consistent with topicCounts:" + k + " " + document_agg_topics[k] + " " + topicCounts[k]);
                System.exit(0);
            }
            if (printOut)
                LOGGER.info("The aggregated topic assignment is consistent with topicCounts:" + k + " " + document_agg_topics[k] + " " + topicCounts[k]);
        }

    }

    private double[] EstimateTheta(int[] docTopicBuffer, int docLength) {
        double[] local_theta = new double[TOPIC_NUM];
        for (int k = 0; k < TOPIC_NUM; k++) {
            local_theta[k] = (docTopicBuffer[k] + alpha[k]) / (docLength + alphaSum);
        }
        return local_theta;
    }

    public double Likelihood() {
        Double result_1 = 0.0;
        Double result_2 = 0.0;

        // topics side likelihood
        for (int v = 0; v < beta.size(); v++) {
            result_1 += LogGamma.logGamma(beta.get(v));
        }
        result_1 = TOPIC_NUM * (LogGamma.logGamma(betaSum) - result_1);

        for (int k = 0; k < TOPIC_NUM; k++) {
            Double part_1 = 0.0;
            Double part_2 = 0.0;
            for (int v=0; v < wordTopicCounts.size(); v++) {
                part_1 = part_1 + LogGamma.logGamma(wordTopicCounts.get(v)[k] + beta.get(v));
                part_2 = part_2 + (wordTopicCounts.get(v)[k] + beta.get(v));
            }
            result_1 = result_1 + part_1 - LogGamma.logGamma(part_2);
        }

        // document side likelihood
        for (int k = 0; k < TOPIC_NUM; k++) {
            result_2 += LogGamma.logGamma(alpha[k]);
        }
        result_2 = docTopicBuffers.size() * (LogGamma.logGamma(alphaSum) - result_2);

        for (int d = 0; d < docTopicBuffers.size(); d++) {
            Double part_1 = 0.0;
            Double part_2 = 0.0;
            for (int k = 0; k < TOPIC_NUM; k++) {
                part_1 = part_1 + LogGamma.logGamma(docTopicBuffers.get(d)[k] + alpha[k]);
                part_2 = part_2 + docTopicBuffers.get(d)[k] + alpha[k];
            }
            result_2 = result_2 + part_1 - LogGamma.logGamma(part_2);
        }

        return result_1 + result_2;
    }

    public void Train() {

        InitModel();
        //CountsCheck(true);

        Double[] p = new Double[TOPIC_NUM];

        LOGGER.info("Start to perform Gibbs Sampling");
        LOGGER.info("MAX_ITER:" + MAX_ITER);

        MultinomialDistribution dist = new MultinomialDistribution(TOPIC_NUM);

        for (CURRENT_ITER = 0; CURRENT_ITER < MAX_ITER; CURRENT_ITER++) {
            LOGGER.info("Start to Iteration " + CURRENT_ITER);
            int num_d = 0;
            for (int d = 0; d < internalDocs.size(); d++) {
                List<Integer> docTopicAssignment = docTopicAssignments.get(d);
                int[] docTopicBuffer = docTopicBuffers.get(d);

                int pos = 0;

                for (Feature f : internalDocs.get(d).getFeatures()) {
                    String feature_name = f.getFeature_name();
                    Integer feature_index = wordForwardIndex.get(feature_name);

                    int current_topic = docTopicAssignment.get(pos);
                    docTopicBuffer[current_topic]--;
                    wordTopicCounts.get(feature_index)[current_topic]--;
                    topicCounts[current_topic]--;

                    // calculate probabilities
                    for (int k = 0; k < TOPIC_NUM; k++) {
                        p[k] = ((wordTopicCounts.get(feature_index)[k] + beta.get(feature_index)) / (topicCounts[k] + betaSum)) * (docTopicBuffer[k] + alpha[k]);
                    }

                    dist.SetProbabilities(p);

                    int new_topic = dist.Sample(RandomUtils.NativeRandom());

                    docTopicBuffer[new_topic]++;
                    wordTopicCounts.get(feature_index)[new_topic]++;
                    topicCounts[new_topic]++;
                    docTopicAssignment.set(pos, new_topic);

                    pos++;

                }

                num_d++;
                if (num_d % 500 == 0)
                    LOGGER.info("Processed:" + num_d);
            }
            LOGGER.info("Finished sampling.");
            LOGGER.info("Finished Iteration " + CURRENT_ITER);
            if (CURRENT_ITER % 25 == 0) {
                Double likelihood = Likelihood();
                LOGGER.info("Iteration " + CURRENT_ITER + " Likelihood:" + Double.toString(likelihood));
                //CountsCheck(true);
            }

            if (CURRENT_ITER % 10 == 0){
                SaveModel();
            }

        }

        SaveModel();
    }

    public void Test() {

    }

    public void SaveModel() {
        String[] outputFileParts = cmdArg.GetParam("modelFile").toString().split(Pattern.quote("."));
        StringBuilder outputFilePrefix = new StringBuilder();
        for(int i = 0; i < outputFileParts.length - 1; i ++){
            outputFilePrefix.append(outputFileParts[i] + ".");
        }
        outputFilePrefix.append(Integer.toString(SAVED % 10) + ".");
        outputFilePrefix.append(outputFileParts[outputFileParts.length - 1]);
        String outputFileName = outputFilePrefix.toString();
        LOGGER.info("Starting to save model to:" + outputFileName);
        Gson gson = new Gson();
        Model obj = new Model();
        obj.setAlpha(alpha, cmdArg);
        obj.setBeta(beta, cmdArg);
        obj.setTopicCounts(topicCounts);
        obj.setWordTopicCounts(wordTopicCounts, cmdArg);
        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFileName), "UTF8"));
            String json = gson.toJson(obj);
            bw.write(json);
            bw.close();
            SAVED ++;

        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGGER.info("Finished save model to:" + outputFileName);
    }

    public void LoadModel(){
        String modelFileName = cmdArg.GetParam("modelFile").toString();
        LOGGER.info("Starting to load model from:" + modelFileName);
        Gson gson = new Gson();

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(modelFileName), "UTF8"));
            String line;
            String jsonString;

            while ((line = br.readLine()) != null) {

            }
            br.close();
        }
        catch (Exception e){
            LOGGER.info("Cannot open the model file.");
        }
    }

}
