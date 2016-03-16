package com.hongliangjie.fugue.topicmodeling.lda;

import com.google.gson.Gson;
import com.hongliangjie.fugue.Message;
import com.hongliangjie.fugue.distributions.MultinomialDistribution;
import com.hongliangjie.fugue.serialization.Document;
import com.hongliangjie.fugue.serialization.Feature;
import com.hongliangjie.fugue.serialization.Model;
import com.hongliangjie.fugue.topicmodeling.TopicModel;
import com.hongliangjie.fugue.utils.LogGamma;
import com.hongliangjie.fugue.utils.MathExp;
import com.hongliangjie.fugue.utils.MathLog;
import com.hongliangjie.fugue.utils.RandomUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by liangjie on 10/29/14.
 */
public class LDA extends TopicModel {
    protected List<Document> internalDocs;
    protected double[] sample_buffer;
    protected HashMap<String, Integer> wordsForwardIndex;
    protected List<ModelCountainer> modelPools;

    protected Message cmdArg;
    protected RandomUtils randomGNR;
    protected MathExp mathExp;
    protected MathLog mathLog;
    protected double[] iterationTimes;

    protected int TOPIC_NUM;
    protected int MAX_ITER;
    protected int CURRENT_ITER;
    protected int INTERVAL;
    protected int TOTAL_TOKEN;
    protected int SAVED;
    protected int TOTAL_SAVES;

    protected static final Logger LOGGER = LogManager.getLogger("FUGUE-TOPICMODELING");

    protected final class ModelCountainer{
        public double[] alpha;
        public double[] beta;
        public double betaSum;
        public double alphaSum;
        public List<int[]> wordTopicCounts; // how many times a term appears in a topic
        public int[] topicCounts; // total number of terms that are assigned to a topic
        public List<int[]> docTopicBuffers; // !!! DENSE !!!
        public List<List<Integer>> docTopicAssignments; // !!! DENSE !!!

        public Map<String, int[]> outsideWordTopicCounts = new HashMap<String, int[]>(); // how many times a term appears in a topic
        public Map<String, Double> outsideBeta;
    }

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
        TOTAL_SAVES = 10;
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

        int expInt = Integer.parseInt(cmdArg.getParam("exp").toString());
        mathExp = new MathExp(expInt);
        LOGGER.info("Math Exp Function:" + expInt);

        int logInt = Integer.parseInt(cmdArg.getParam("log").toString());
        mathLog = new MathLog(logInt);
        LOGGER.info("Math Log Function:" + logInt);
    }

    @Override
    public Message getMessage(){
        return cmdArg;
    }

    protected void initTrainModel() {

        TOPIC_NUM = (Integer) cmdArg.getParam("topics");
        MAX_ITER = (Integer) cmdArg.getParam("iters");
        iterationTimes = new double[MAX_ITER];
        internalDocs = (List<Document>) cmdArg.getParam("docs");

        /* build index */
        LOGGER.info("Start to build index.");
        wordsForwardIndex = new HashMap<String, Integer>();
        wordsForwardIndex.clear();
        for (int d = 0; d < internalDocs.size(); d++) {
            for (Feature f : internalDocs.get(d).getFeatures()) {
                String feature_name = f.getFeatureName();
                if (!wordsForwardIndex.containsKey(feature_name)) {
                    wordsForwardIndex.put(feature_name, wordsForwardIndex.size());
                }
            }
        }

        LOGGER.info("Start to initialize model.");
        LOGGER.info("Topic Num:" + TOPIC_NUM);
        LOGGER.info("ForwardIndex Size:" + wordsForwardIndex.size());

        modelPools = new ArrayList<ModelCountainer>();
        modelPools.add(0, new ModelCountainer());

        /* initialize all model parameters with fixed sizes */
        modelPools.get(0).wordTopicCounts = new ArrayList<int[]>(wordsForwardIndex.size());
        modelPools.get(0).beta = new double[wordsForwardIndex.size()];
        for (int i = 0; i < wordsForwardIndex.size(); i++) {
            int[] topicCounts = new int[TOPIC_NUM];
            modelPools.get(0).wordTopicCounts.add(topicCounts);
            modelPools.get(0).beta[i] = 0.01;
            modelPools.get(0).betaSum += modelPools.get(0).beta[i];
        }
        modelPools.get(0).docTopicBuffers = new ArrayList<int[]>(internalDocs.size());
        modelPools.get(0).docTopicAssignments = new ArrayList<List<Integer>>(internalDocs.size());
        for (int i = 0; i < internalDocs.size(); i++) {
            int[] topicBuffer = new int[TOPIC_NUM];
            modelPools.get(0).docTopicBuffers.add(topicBuffer);
            modelPools.get(0).docTopicAssignments.add(new ArrayList<Integer>());
        }
        modelPools.get(0).topicCounts = new int[TOPIC_NUM];
        modelPools.get(0).alpha = new double[TOPIC_NUM];
        TOTAL_TOKEN = 0;

        for (int k = 0; k < TOPIC_NUM; k++)
            modelPools.get(0).alpha[k] = 50.0 / TOPIC_NUM;
        modelPools.get(0).alphaSum = 50.0; // (50.0/TOPIC_NUM) * TOPIC_NUM

        for (int d = 0; d < internalDocs.size(); d++) {
            for (Feature f : internalDocs.get(d).getFeatures()) {
                String feature_name = f.getFeatureName();
                Integer feature_index = wordsForwardIndex.get(feature_name);
                // we randomly assign a topic for this token
                int topic = randomGNR.nextInt(TOPIC_NUM);
                modelPools.get(0).docTopicAssignments.get(d).add(topic);
                modelPools.get(0).docTopicBuffers.get(d)[topic]++;
                modelPools.get(0).wordTopicCounts.get(feature_index)[topic]++;
                modelPools.get(0).topicCounts[topic]++;
                TOTAL_TOKEN++;
            }

        }

        LOGGER.info("Term Num:" + modelPools.get(0).wordTopicCounts.size());
        LOGGER.info("alphaSum:" + modelPools.get(0).alphaSum);
        LOGGER.info("betaSum:" + modelPools.get(0).betaSum);
        LOGGER.info("Finished initializing model");
    }

    public double likelihood(int modelID) {
        double result_1 = 0.0;
        double result_2 = 0.0;

        // topics side likelihood
        for (int v = 0; v < modelPools.get(modelID).beta.length; v++) {
            result_1 += LogGamma.logGamma(modelPools.get(modelID).beta[v]);
        }
        result_1 = TOPIC_NUM * (LogGamma.logGamma(modelPools.get(modelID).betaSum) - result_1);

        for (int k = 0; k < TOPIC_NUM; k++) {
            double part_1 = 0.0;
            double part_2 = 0.0;
            for (int v=0; v < modelPools.get(0).wordTopicCounts.size(); v++) {
                part_1 = part_1 + LogGamma.logGamma(modelPools.get(modelID).wordTopicCounts.get(v)[k] + modelPools.get(modelID).beta[v]);
                part_2 = part_2 + (modelPools.get(modelID).wordTopicCounts.get(v)[k] + modelPools.get(modelID).beta[v]);
            }
            result_1 = result_1 + part_1 - LogGamma.logGamma(part_2);
        }

        // document side likelihood
        for (int k = 0; k < TOPIC_NUM; k++) {
            result_2 += LogGamma.logGamma(modelPools.get(modelID).alpha[k]);
        }
        result_2 = modelPools.get(modelID).docTopicBuffers.size() * (LogGamma.logGamma(modelPools.get(modelID).alphaSum) - result_2);

        for (int d = 0; d < modelPools.get(modelID).docTopicBuffers.size(); d++) {
            double part_1 = 0.0;
            double part_2 = 0.0;
            for (int k = 0; k < TOPIC_NUM; k++) {
                part_1 = part_1 + LogGamma.logGamma(modelPools.get(modelID).docTopicBuffers.get(d)[k] + modelPools.get(modelID).alpha[k]);
                part_2 = part_2 + modelPools.get(modelID).docTopicBuffers.get(d)[k] + modelPools.get(modelID).alpha[k];
            }
            result_2 = result_2 + part_1 - LogGamma.logGamma(part_2);
        }

        return result_1 + result_2;
    }

    protected abstract class Sampler{
        protected MultinomialDistribution dist;
        protected ProcessDocuments processor;

        public abstract int draw(int modelID, int featureID, double randomRV);
        public void setProcessor(ProcessDocuments proc){
            processor = proc;
        }
    }

    protected class GibbsBinarySampling extends GibbsSampling{
        public GibbsBinarySampling(){
            dist = new MultinomialDistribution(TOPIC_NUM, mathLog, mathExp, "binary");
            LOGGER.info("Gibbs Sampling: Binary");
        }
    }


    protected class GibbsSampling extends Sampler{
        public GibbsSampling(){
            dist = new MultinomialDistribution(TOPIC_NUM, mathLog, mathExp, "normal");
            LOGGER.info("Gibbs Sampling: Normal");
        }

        @Override
        public int draw(int modelID, int featureID, double randomRV){
            processor.computeProbabilities(modelID, featureID);
            dist.setProbabilities(sample_buffer);
            return dist.sample(randomRV);
        }
    }

    protected class GibbsLogSampling extends Sampler{
        public GibbsLogSampling(){
            dist = new MultinomialDistribution(TOPIC_NUM, mathLog, mathExp, "log");
            LOGGER.info("Gibbs Sampling: Log");
        }

        @Override
        public int draw(int modelID, int featureID, double randomRV){
            processor.computeLogProbabilities(modelID, featureID);
            dist.setProbabilities(sample_buffer);
            return dist.sample(randomRV);
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

        public void computeProbabilities(int modelID, int featureID){
            // calculate normal probabilities
            for (int k = 0; k < TOPIC_NUM; k++) {
                sample_buffer[k] = ((modelPools.get(modelID).wordTopicCounts.get(featureID)[k] + modelPools.get(modelID).beta[featureID]) / (modelPools.get(modelID).topicCounts[k] + modelPools.get(modelID).betaSum)) * (docTopicBuffer[k] + modelPools.get(modelID).alpha[k]);
            }
        }

        public void computeLogProbabilities(int modelID, int featureID){
            // calculate log-probabilities
            for (int k = 0; k < TOPIC_NUM; k++) {
                sample_buffer[k] = mathLog.compute(docTopicBuffer[k] + modelPools.get(modelID).alpha[k]);
                sample_buffer[k] += mathLog.compute(modelPools.get(modelID).wordTopicCounts.get(featureID)[k] + modelPools.get(modelID).beta[featureID]);
                sample_buffer[k] -= mathLog.compute(modelPools.get(modelID).topicCounts[k] + modelPools.get(modelID).betaSum);
            }
        }

        protected int sampleOneDoc(List<Document> docs, int index, int modelID){
            Document d = docs.get(index);
            docTopicAssignment = modelPools.get(modelID).docTopicAssignments.get(index);
            docTopicBuffer = modelPools.get(modelID).docTopicBuffers.get(index);
            int pos = 0;

            for (Feature f : d.getFeatures()) {
                String featureName = f.getFeatureName();
                Integer featureIndex = wordsForwardIndex.get(featureName);

                int current_topic = docTopicAssignment.get(pos);
                docTopicBuffer[current_topic]--;
                modelPools.get(modelID).wordTopicCounts.get(featureIndex)[current_topic]--;
                modelPools.get(modelID).topicCounts[current_topic]--;

                double randomRV = randomGNR.nextDouble();
                int new_topic = sampler.draw(modelID, featureIndex, randomRV);

                docTopicBuffer[new_topic]++;
                modelPools.get(modelID).wordTopicCounts.get(featureIndex)[new_topic]++;
                modelPools.get(modelID).topicCounts[new_topic]++;
                docTopicAssignment.set(pos, new_topic);

                pos++;
            }
            return pos;
        }

        public void sampleOverDocs(int modelID, List<Document> docs, int maxIter, int save){
            int overall_pos = 0;
            long overall_startTime = System.currentTimeMillis();
            for (CURRENT_ITER = 0; CURRENT_ITER < maxIter; CURRENT_ITER++) {
                LOGGER.info("Start to Iteration " + CURRENT_ITER);
                long startTime = System.currentTimeMillis();
                int num_d = 0;
                int total_pos = 0;
                for (int d = 0; d < docs.size(); d++) {
                    int doc_pos = sampleOneDoc(docs, d, modelID);
                    overall_pos += doc_pos;
                    total_pos += doc_pos;
                    num_d++;
                    if (num_d % 500 == 0)
                        LOGGER.info("Processed:" + num_d);
                }
                LOGGER.info("Finished sampling.");
                LOGGER.info("Finished Iteration " + CURRENT_ITER);
                if (CURRENT_ITER % 25 == 0) {
                    double likelihood = likelihood(modelID);
                    LOGGER.info("Iteration " + CURRENT_ITER + " Likelihood:" + Double.toString(likelihood));
                }

                if ((CURRENT_ITER % 10 == 0) && (save == 1)){
                    saveModel(0);
                }
                long endTime = System.currentTimeMillis();
                double timeDifference = (endTime - startTime) / 1000.0;
                double tokenPerSeconds = (total_pos / 1000.0) / timeDifference;
                LOGGER.info("Iteration Duration " + CURRENT_ITER + " " + Double.toString(timeDifference));
                LOGGER.info("Tokens (per-K)/Seconds " + CURRENT_ITER + " " + Double.toString(tokenPerSeconds));
                iterationTimes[CURRENT_ITER] = timeDifference;
            }

            double averageTime = 0;
            for (int k = 0; k < maxIter; k++){
                averageTime += iterationTimes[k];
            }
            long overall_endTime = System.currentTimeMillis();
            LOGGER.info("Average Iteration Duration " +  Double.toString(averageTime / (double)maxIter));
            LOGGER.info("Average Tokens (per-K)/Seconds " + Double.toString((overall_pos / 1000.0) /((overall_endTime - overall_startTime) / 1000.0)));
        }
    }

    public void train() {
        initTrainModel();
        LOGGER.info("Start to perform Gibbs Sampling");
        LOGGER.info("MAX_ITER:" + MAX_ITER);
        String samplerStr = cmdArg.getParam("LDASampler").toString();
        int save = (Integer)cmdArg.getParam("saveModel");

        Sampler s = null;
        if (samplerStr != null) {
            if ("normal".equals(samplerStr)) {
                s = new GibbsSampling();

            }
            else if ("log".equals(samplerStr)){
                s = new GibbsLogSampling();
            }
            else if ("binary".equals(samplerStr)){
                s = new GibbsBinarySampling();
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
        p.sampleOverDocs(0, internalDocs, MAX_ITER, save);
        if (save == 1)
            saveModel(0);
    }

    public void test() {
        try {
            loadModel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadModel() throws IOException {
        int multipleModels = Integer.parseInt(cmdArg.getParam("multipleModels").toString());
        String[] modelFileNames = null;
        if (multipleModels == 1) {
            modelFileNames = getModelFiles(true);
        }
        else{
            modelFileNames = new String[1];
            modelFileNames[0] = cmdArg.getParam("modelFile").toString();
        }
        Gson gson = new Gson();
        modelPools = new ArrayList<ModelCountainer>();
        for (int s = 0; s < modelFileNames.length; s++) {
            String modelFileName = modelFileNames[s];
            File modelFile = new File(modelFileName);
            if (modelFile.exists() && !modelFile.isDirectory()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(modelFile), "UTF8"));
                String line = br.readLine();
                if (line != null){
                    Model obj = gson.fromJson(line, LDAModel.class);
                    Message msg = obj.getParameters();
                    ModelCountainer currentModel = new ModelCountainer();
                    currentModel.alpha = (double[])msg.getParam("alpha");
                    currentModel.outsideBeta = (Map<String, Double>)msg.getParam("beta");
                    currentModel.outsideWordTopicCounts = (Map<String, int[]>)msg.getParam("wordTopicCounts");
                    currentModel.topicCounts = (int[])msg.getParam("topicCounts");
                    modelPools.add(currentModel);
                }
                LOGGER.info("Loaded " + modelFileName);
            }
        }
    }

    private String[] getModelFiles(boolean all){
        String[] outputFileParts = cmdArg.getParam("modelFile").toString().split(Pattern.quote("."));
        StringBuilder outputFilePrefix = new StringBuilder();
        for(int i = 0; i < outputFileParts.length - 1; i ++){
            outputFilePrefix.append(outputFileParts[i] + ".");
        }
        if (all) {
            String[] oneFile = new String[1];
            outputFilePrefix.append(Integer.toString(SAVED % TOTAL_SAVES) + ".");
            outputFilePrefix.append(outputFileParts[outputFileParts.length - 1]);
            String outputFileName = outputFilePrefix.toString();
            oneFile[0] = outputFileName;
            return oneFile;
        }
        String[] returnFiles = new String[TOTAL_SAVES];
        for(int i = 0; i < TOTAL_SAVES; i++){
            StringBuilder firstPart = new StringBuilder(outputFilePrefix.toString());
            firstPart.append(Integer.toString(i) + ".");
            firstPart.append(outputFileParts[outputFileParts.length - 1]);
            String outputFileName = firstPart.toString();
            returnFiles[i] = outputFileName;
        }
        return returnFiles;
    }

    public void saveModel(int modelID) {
        String outputFileName = getModelFiles(false)[0];
        LOGGER.info("Starting to save model to:" + outputFileName);
        Gson gson = new Gson();
        Model obj = new LDAModel();
        cmdArg.setParam("alpha", modelPools.get(modelID).alpha);
        cmdArg.setParam("beta", modelPools.get(modelID).beta);
        cmdArg.setParam("topicCounts", modelPools.get(modelID).topicCounts);
        cmdArg.setParam("wordTopicCounts", modelPools.get(modelID).wordTopicCounts);
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
