package com.hongliangjie.fugue.topicmodeling.lda;

import com.google.gson.Gson;
import com.hongliangjie.fugue.Message;
import com.hongliangjie.fugue.distributions.MultinomialDistribution;
import com.hongliangjie.fugue.serialization.Document;
import com.hongliangjie.fugue.serialization.Feature;
import com.hongliangjie.fugue.serialization.Model;
import com.hongliangjie.fugue.topicmodeling.TopicModel;
import com.hongliangjie.fugue.utils.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by liangjie on 10/29/14.
 */
public class LDA extends TopicModel {
    protected Map<String, Integer> wordForwardIndex;
    protected List<int[]> wordTopicCounts; // how many times a term appears in a topic
    protected int[] topicCounts; // total number of terms that are assigned to a topic
    protected List<int[]> docTopicBuffers; // !!! DENSE !!!
    protected List<List<Integer>> docTopicAssignments; // !!! DENSE !!!
    protected List<Document> internalDocs;
    protected double[] alpha;
    protected List<Double> beta;
    protected double[] sample_buffer;
    protected double betaSum;
    protected double alphaSum;
    protected Message cmdArg;
    protected RandomUtils randomGNR;
    protected LogUtils logAdd;
    protected MathExp mathExp;
    protected MathLog mathLog;
    protected long[] iterationTimes;

    protected int TOPIC_NUM;
    protected int MAX_ITER;
    protected int CURRENT_ITER;
    protected int INTERVAL;
    protected int TOTAL_TOKEN;
    protected int SAVED;

    protected static final Logger LOGGER = LogManager.getLogger("FUGUE-TOPICMODELING");

    public LDA() {
        this(new RandomUtils(0));
        LOGGER.info("Random Number Generator: Native");
    }

    public LDA(RandomUtils r){
        TOPIC_NUM = 1;
        MAX_ITER = 250;
        INTERVAL = 5;
        TOTAL_TOKEN = 0;
        SAVED = 0;
        randomGNR = r;
        iterationTimes = new long[MAX_ITER];
    }

    @Override
    public void setMessage(Message m) {
        cmdArg = m;
        String randomGNRStr = cmdArg.getParam("random").toString();
        if (randomGNRStr != null){
            if ("native".equals(randomGNRStr)){
                randomGNR = new RandomUtils(0);
                LOGGER.info("Random Number Generator: Native");

            }
            else if ("deterministic".equals(randomGNRStr)){
                randomGNR = new RandomUtils(1);
                LOGGER.info("Random Number Generator: Deterministic");

            }
            else{
                randomGNR = new RandomUtils(0);
                LOGGER.info("Random Number Generator: Native");

            }
        }
        else{
            randomGNR = new RandomUtils(0);
            LOGGER.info("Random Number Generator: Native");
        }

        int expInt = (Integer)cmdArg.getParam("exp");
        mathExp = new MathExp(expInt);
        LOGGER.info("Math Exp Function:" + expInt);

        int logInt = (Integer)cmdArg.getParam("log");
        mathLog = new MathLog(logInt);
        LOGGER.info("Math Log Function:" + logInt);

        logAdd = new LogUtils(mathLog, mathExp);
    }

    @Override
    public Message getMessage(){
        return cmdArg;
    }

    protected void initModel() {

        TOPIC_NUM = (Integer) cmdArg.getParam("topics");
        MAX_ITER = (Integer) cmdArg.getParam("iters");
        internalDocs = (List<Document>) cmdArg.getParam("docs");
        wordForwardIndex = (HashMap<String, Integer>) cmdArg.getParam("forwardIndex"); // get word forward index

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
                String feature_name = f.getFeatureName();
                Integer feature_index = wordForwardIndex.get(feature_name);
                // we randomly assign a topic for this token
                int topic = randomGNR.nextInt(TOPIC_NUM);
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

    public double likelihood() {
        double result_1 = 0.0;
        double result_2 = 0.0;

        // topics side likelihood
        for (int v = 0; v < beta.size(); v++) {
            result_1 += LogGamma.logGamma(beta.get(v));
        }
        result_1 = TOPIC_NUM * (LogGamma.logGamma(betaSum) - result_1);

        for (int k = 0; k < TOPIC_NUM; k++) {
            double part_1 = 0.0;
            double part_2 = 0.0;
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
            double part_1 = 0.0;
            double part_2 = 0.0;
            for (int k = 0; k < TOPIC_NUM; k++) {
                part_1 = part_1 + LogGamma.logGamma(docTopicBuffers.get(d)[k] + alpha[k]);
                part_2 = part_2 + docTopicBuffers.get(d)[k] + alpha[k];
            }
            result_2 = result_2 + part_1 - LogGamma.logGamma(part_2);
        }

        return result_1 + result_2;
    }

    protected abstract class Sampler{
        protected MultinomialDistribution dist;
        protected ProcessDocuments processor;

        public abstract Integer draw(Integer feature_index, double randomRV);
        public void setProcessor(ProcessDocuments proc){
            processor = proc;
        }
    }

    protected class GibbsSampling extends Sampler{
        public GibbsSampling(){
            dist = new MultinomialDistribution(TOPIC_NUM, mathLog);
            LOGGER.info("Gibbs Sampling: Normal");
        }

        @Override
        public Integer draw(Integer feature_index, double randomRV){
            processor.computeProbabilities(feature_index);
            dist.setProbabilities(sample_buffer);
            return dist.sample(randomRV);
        }
    }

    protected class GibbsLogSampling extends Sampler{
        public GibbsLogSampling(){
            dist = new MultinomialDistribution(TOPIC_NUM, mathLog);
            LOGGER.info("Gibbs Sampling: Log");
        }

        @Override
        public Integer draw(Integer feature_index, double randomRV){
            processor.computeLogProbabilities(feature_index);
            dist.setLogProbabilities(sample_buffer, logAdd);
            return dist.logSample(randomRV);
        }
    }

    protected class ProcessDocuments{
        protected int[] docTopicBuffer;
        protected List<Integer> docTopicAssignment;
        protected Sampler sampler;

        public ProcessDocuments(){
            this(new GibbsSampling());
        }

        public ProcessDocuments(Sampler s){
            sample_buffer = new double[TOPIC_NUM];
            sampler = s;
        }

        public void computeProbabilities(Integer feature_index){
            // calculate normal probabilities
            for (int k = 0; k < TOPIC_NUM; k++) {
                sample_buffer[k] = ((wordTopicCounts.get(feature_index)[k] + beta.get(feature_index)) / (topicCounts[k] + betaSum)) * (docTopicBuffer[k] + alpha[k]);
            }
        }

        public void computeLogProbabilities(Integer feature_index){
            // calculate log-probabilities
            for (int k = 0; k < TOPIC_NUM; k++) {
                sample_buffer[k] = mathLog.compute(docTopicBuffer[k] + alpha[k]);
                sample_buffer[k] += mathLog.compute(wordTopicCounts.get(feature_index)[k] + beta.get(feature_index));
                sample_buffer[k] -= mathLog.compute(topicCounts[k] + betaSum);
            }
        }

        protected void sampleOneDoc(List<Document> docs, int index){
            Document d = docs.get(index);
            docTopicAssignment = docTopicAssignments.get(index);
            docTopicBuffer = docTopicBuffers.get(index);
            int pos = 0;

            for (Feature f : d.getFeatures()) {
                String feature_name = f.getFeatureName();
                Integer feature_index = wordForwardIndex.get(feature_name);

                int current_topic = docTopicAssignment.get(pos);
                docTopicBuffer[current_topic]--;
                wordTopicCounts.get(feature_index)[current_topic]--;
                topicCounts[current_topic]--;

                double randomRV = randomGNR.nextDouble();
                int new_topic = sampler.draw(feature_index, randomRV);

                docTopicBuffer[new_topic]++;
                wordTopicCounts.get(feature_index)[new_topic]++;
                topicCounts[new_topic]++;
                docTopicAssignment.set(pos, new_topic);

                pos++;

            }
        }

        public void sampleOverDocs(List<Document> docs, Integer maxIter, Integer save){
            for (CURRENT_ITER = 0; CURRENT_ITER < maxIter; CURRENT_ITER++) {
                LOGGER.info("Start to Iteration " + CURRENT_ITER);
                long startTime = System.currentTimeMillis();
                int num_d = 0;
                for (int d = 0; d < docs.size(); d++) {
                    sampleOneDoc(docs, d);
                    num_d++;
                    if (num_d % 500 == 0)
                        LOGGER.info("Processed:" + num_d);
                }
                LOGGER.info("Finished sampling.");
                LOGGER.info("Finished Iteration " + CURRENT_ITER);
                if (CURRENT_ITER % 25 == 0) {
                    double likelihood = likelihood();
                    LOGGER.info("Iteration " + CURRENT_ITER + " Likelihood:" + Double.toString(likelihood));
                }

                if ((CURRENT_ITER % 10 == 0) && (save == 1)){
                    saveModel();
                }
                long endTime = System.currentTimeMillis();
                long timeDifference = endTime - startTime;
                LOGGER.info("Iteration Duration " + CURRENT_ITER + " " + Double.toString(timeDifference / 1000.0));
                iterationTimes[CURRENT_ITER] = timeDifference;
            }

            long averageTime = 0;
            for (int k = 0; k < maxIter; k++){
                averageTime += iterationTimes[k];
            }
            LOGGER.info("Average Iteration Duration " +  Double.toString(averageTime / (double)maxIter));
        }
    }

    public void train() {
        initModel();
        LOGGER.info("Start to perform Gibbs Sampling");
        LOGGER.info("MAX_ITER:" + MAX_ITER);
        String samplerStr = cmdArg.getParam("LDASampler").toString();
        Integer save = (Integer)cmdArg.getParam("saveModel");

        Sampler s = null;
        if (samplerStr != null) {
            if ("normal".equals(samplerStr)) {
                s = new GibbsSampling();

            }
            else if ("log".equals(samplerStr)){
                s = new GibbsLogSampling();
            }
            else{
                s = new GibbsSampling();
            }
        }
        else{
            s = new GibbsSampling();
        }

        ProcessDocuments p = new ProcessDocuments(s);
        s.setProcessor(p);
        p.sampleOverDocs(internalDocs, MAX_ITER, save);
        if (save == 1)
            saveModel();
    }

    public void test() {

    }

    public void saveModel() {
        String[] outputFileParts = cmdArg.getParam("modelFile").toString().split(Pattern.quote("."));
        StringBuilder outputFilePrefix = new StringBuilder();
        for(int i = 0; i < outputFileParts.length - 1; i ++){
            outputFilePrefix.append(outputFileParts[i] + ".");
        }
        outputFilePrefix.append(Integer.toString(SAVED % 10) + ".");
        outputFilePrefix.append(outputFileParts[outputFileParts.length - 1]);
        String outputFileName = outputFilePrefix.toString();
        LOGGER.info("Starting to save model to:" + outputFileName);
        Gson gson = new Gson();
        Model obj = new LDAModel();
        cmdArg.setParam("alpha", alpha);
        cmdArg.setParam("beta", beta);
        cmdArg.setParam("topicCounts", topicCounts);
        cmdArg.setParam("wordTopicCounts", wordTopicCounts);
        obj.setParameters(cmdArg);

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
}
