package com.hongliangjie.fugue.topicmodeling.LDA;

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
    protected HashMap<Integer, String> wordsInvertedIndex;
    protected List<ModelCountainer> modelPools;

    protected Message cmdArg;
    protected RandomUtils randomGNR;
    protected MathExp mathExp;
    protected MathLog mathLog;
    protected double[] iterationTimes;

    protected int TOPIC_NUM;
    protected int MAX_ITER;
    protected int CURRENT_ITER;
    protected int BURN_IN;
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

        public Map<String, int[]> outsideWordTopicCounts; // how many times a term appears in a topic
        public Map<String, Double> outsideBeta;

        public double[][] phi;

        public void computePhi(){
            phi = new double[wordTopicCounts.size()][alpha.length];
            for(int i = 0; i < wordTopicCounts.size(); i++){
                for (int j = 0; j < wordTopicCounts.get(i).length; j++){
                    phi[i][j] = (wordTopicCounts.get(i)[j] + beta[i]) / (topicCounts[j] + betaSum);
                }
            }
        }

    }

    public LDA() {
        this(new RandomUtils(0));
        LOGGER.info("Random Number Generator: Native");
    }

    public LDA(RandomUtils r){
        TOPIC_NUM = 1;
        MAX_ITER = 250;
        BURN_IN = 100;
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

        TOPIC_NUM = Integer.parseInt(cmdArg.getParam("topics").toString());
        MAX_ITER = Integer.parseInt(cmdArg.getParam("iters").toString());

        iterationTimes = new double[MAX_ITER];
        internalDocs = (List<Document>) cmdArg.getParam("docs");

    }

    @Override
    public Message getMessage(){
        cmdArg.setParam("invertedIndex", wordsInvertedIndex);
        cmdArg.setParam("forwardIndex", wordsForwardIndex);
        return cmdArg;
    }

    public void rebuildIndex(){
        /* build index */
        LOGGER.info("Start to build index.");
        wordsForwardIndex = new HashMap<String, Integer>();
        wordsForwardIndex.clear();
        wordsInvertedIndex = new HashMap<Integer, String>();
        wordsInvertedIndex.clear();
        for (int d = 0; d < internalDocs.size(); d++) {
            for (Feature f : internalDocs.get(d).getFeatures()) {
                String feature_name = f.getFeatureName();
                if (!wordsForwardIndex.containsKey(feature_name)) {
                    wordsForwardIndex.put(feature_name, wordsForwardIndex.size());
                    Integer word_index = wordsForwardIndex.get(feature_name);
                    wordsInvertedIndex.put(word_index, feature_name);
                }
            }
        }
        LOGGER.info("Index Size:" + wordsForwardIndex.size());
    }

    protected void initTrainModel() {
        rebuildIndex();
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

    protected abstract class HyperparameterOptimization{
        public abstract void optimize(); // as hyper-parameter optimization only happens in training, the modelID is always set to 0 and therefore ignored.
    }

    protected class SliceSampling extends HyperparameterOptimization{

        protected int _samplesNum; // the number of samples
        protected double _step; // the step used in StepOut
        protected int _hyperIterations;

        public SliceSampling(){
            _samplesNum = Integer.parseInt(cmdArg.getParam("sliceSamples").toString());
            _step = Double.parseDouble(cmdArg.getParam("sliceSteps").toString());
            _hyperIterations = Integer.parseInt(cmdArg.getParam("sliceIters").toString());
        }

        protected void copyArray(double[] src, double[] dest){
            for(int i = 0; i < src.length; i++)
                dest[i] = src[i];
        }

        @Override
        public void optimize() {
            double[] alpha = new double[modelPools.get(0).alpha.length];
            double alphaSum = modelPools.get(0).alphaSum;
            double[] beta = new double[modelPools.get(0).beta.length];
            double betaSum = modelPools.get(0).betaSum;

            double[] alphaLeft = new double[alpha.length];
            double[] alphaRight = new double[alpha.length];
            double[] betaLeft = new double[beta.length];
            double[] betaRight = new double[beta.length];

            double[] alphaNew = new double[alpha.length];
            double[] betaNew = new double[beta.length];
            double alphaNewSum = 0.0;
            double betaNewSum = 0.0;

            copyArray(modelPools.get(0).alpha, alpha);
            copyArray(modelPools.get(0).beta, beta);

            for(int k = 0; k < _samplesNum; k++){
                double old_likelihood = likelihood(modelPools.get(0).wordTopicCounts, modelPools.get(0).docTopicBuffers, alpha, beta, alphaSum, betaSum);
                double new_likelihood = mathLog.compute(randomGNR.nextDouble()) + old_likelihood;

                // stepping out
                for (int i = 0; i < alpha.length; i++){
                    alphaLeft[i] = alpha[i] - randomGNR.nextDouble() * _step;
                    alphaRight[i] = alphaLeft[i] + _step;
                }

                for (int i = 0; i < beta.length; i++){
                    betaLeft[i] = beta[i] - randomGNR.nextDouble() * _step;
                    betaRight[i] = betaLeft[i] + _step;
                }
                // This stepping out is simplified, please look at Fig 3. in Neal's "Slice Sampling" paper

                for(int j = 0; j < _hyperIterations; j++){
                    alphaNewSum = 0.0;
                    betaNewSum = 0.0;

                    for(int i = 0; i < alpha.length; i++){
                        alphaNew[i] = randomGNR.nextDouble() * (alphaRight[i] - alphaLeft[i]) + alphaLeft[i];
                        alphaNewSum += alphaNew[i];
                    }

                    for(int i = 0; i < beta.length; i++){
                        betaNew[i] = randomGNR.nextDouble() * (betaRight[i] - betaLeft[i]) + betaLeft[i];
                        betaNewSum += betaNew[i];
                    }

                    double test_likelihood = likelihood(modelPools.get(0).wordTopicCounts, modelPools.get(0).docTopicBuffers, alphaNew, betaNew, alphaNewSum, betaNewSum);

                    if (test_likelihood > new_likelihood){
                        copyArray(alphaNew, alpha);
                        alphaSum = alphaNewSum;

                        copyArray(betaNew, beta);
                        betaSum = betaNewSum;
                        LOGGER.info("[Slice Sampling]: Sample " + k + " A new set of hyper-parameter with likelihood " + test_likelihood);
                        break;
                    }
                    else{
                        for(int i = 0; i < alpha.length; i++){
                            if(alphaNew[i] < alpha[i]){
                                alphaLeft[i] = alphaNew[i];
                            }
                            else{
                                alphaRight[i] = alphaNew[i];
                            }
                        }
                        for(int i = 0; i < beta.length; i++){
                            if(betaNew[i] < beta[i]){
                                betaLeft[i] = betaNew[i];
                            }
                            else{
                                betaRight[i] = betaNew[i];
                            }
                        }
                    }
                }
            }
            // only keep the last sample for both alpha and beta
            // update back to models
            copyArray(alpha, modelPools.get(0).alpha);
            modelPools.get(0).alphaSum = alphaSum;

            copyArray(beta, modelPools.get(0).beta);
            modelPools.get(0).betaSum = betaSum;
        }
    }


    public double likelihood(int modelID){
        return likelihood(modelPools.get(modelID).wordTopicCounts, modelPools.get(modelID).docTopicBuffers, modelPools.get(modelID).alpha, modelPools.get(modelID).beta, modelPools.get(modelID).alphaSum, modelPools.get(modelID).betaSum);
    }

    public double likelihood(List<int[]> wordTopicCounts, List<int[]> docTopicBuffers, double[] alpha, double[] beta, double alphaSum, double betaSum) {
        double result_1 = 0.0;
        double result_2 = 0.0;

        // topics side likelihood
        for (int v = 0; v < beta.length; v++) {
            result_1 += LogGamma.logGamma(beta[v]);
        }
        result_1 = TOPIC_NUM * (LogGamma.logGamma(betaSum) - result_1);

        for (int k = 0; k < TOPIC_NUM; k++) {
            double part_1 = 0.0;
            double part_2 = 0.0;
            for (int v=0; v < modelPools.get(0).wordTopicCounts.size(); v++) {
                part_1 = part_1 + LogGamma.logGamma(wordTopicCounts.get(v)[k] + beta[v]);
                part_2 = part_2 + (wordTopicCounts.get(v)[k] + beta[v]);
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
        protected HyperparameterOptimization hyperOpt;

        public ProcessDocuments(){
            this(new GibbsSampling(), null);
        }

        public ProcessDocuments(Sampler s, HyperparameterOptimization hyper){
            sample_buffer = new double[TOPIC_NUM];
            sampler = s;
            hyperOpt = hyper;
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

        public void sampleOverDocs(int modelID, List<Document> docs, int start, int end, int maxIter, int save){
            int overall_pos = 0;
            long overall_startTime = System.currentTimeMillis();
            for (CURRENT_ITER = 0; CURRENT_ITER < maxIter; CURRENT_ITER++) {
                LOGGER.info("Start to Iteration " + CURRENT_ITER);
                long startTime = System.currentTimeMillis();
                int num_d = 0;
                int total_pos = 0;
                for (int d = start; d < end; d++) {
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
                    double likelihood = likelihood(modelPools.get(modelID).wordTopicCounts, modelPools.get(modelID).docTopicBuffers, modelPools.get(modelID).alpha, modelPools.get(modelID).beta, modelPools.get(modelID).alphaSum, modelPools.get(modelID).betaSum);
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

                if ((CURRENT_ITER >= BURN_IN) && (CURRENT_ITER % 25 == 0) && (hyperOpt != null)){
                    // hyper-parameter optimization
                    LOGGER.info("Start Hyper-parameter Optimization");
                    hyperOpt.optimize();
                    LOGGER.info("Finished Hyper-parameter Optimization");
                }
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

    protected class ProcessTestDocuments extends ProcessDocuments{

        protected double[] modelPerplexity;
        protected int[] modelSaved;

        public ProcessTestDocuments(Sampler s){
            super(s, null);
        }

        protected double computeTermProbability(double[] theta, int featureID, ModelCountainer m){
            double prob = 0.0;
            for (int k = 0; k < TOPIC_NUM; k++) {
                prob += theta[k] * m.phi[featureID][k];
            }
            return prob;
        }

        protected void sampleTestDoc(List<Document> docs, int maxIter, int docIndex) {
            // for each test document, half of the document is used to "fold-in" and the other half is used to compute "perplexity"
            for (int m = 0; m < modelPools.size(); m++) {
                docTopicAssignment = new ArrayList<Integer>();
                docTopicBuffer = new int[TOPIC_NUM];
                double[] theta = new double[TOPIC_NUM];
                List<Feature> currentFeatures = docs.get(docIndex).getFeatures();
                int docLength = currentFeatures.size();
                int foldIn = docLength / 2;
                // firstly init topic assignments
                for (int i = 0; i < foldIn; i++) {
                    // we randomly assign a topic for this token
                    int topic = randomGNR.nextInt(TOPIC_NUM);
                    docTopicAssignment.add(topic);
                    docTopicBuffer[topic]++;
                }
                int BURN_IN = 0;
                for (CURRENT_ITER = 0; CURRENT_ITER < maxIter; CURRENT_ITER++) {
                    // fold-in
                    for (int i = 0; i < foldIn; i++) {
                        String featureName = currentFeatures.get(i).getFeatureName();
                        Integer featureIndex = wordsForwardIndex.get(featureName);

                        int current_topic = docTopicAssignment.get(i);
                        docTopicBuffer[current_topic]--;

                        double randomRV = randomGNR.nextDouble();
                        int new_topic = sampler.draw(m, featureIndex, randomRV);

                        docTopicBuffer[new_topic]++;
                        docTopicAssignment.set(i, new_topic);
                    }
                    if ((CURRENT_ITER >= BURN_IN) && (CURRENT_ITER % 5 == 0)) {
                        // estimate theta
                        for (int k = 0; k < TOPIC_NUM; k++) {
                            theta[k] = (docTopicBuffer[k] + modelPools.get(m).alpha[k]) / (foldIn + modelPools.get(m).alphaSum);
                        }

                        // compute perplexity
                        for (int i = foldIn; i < currentFeatures.size(); i++) {
                            String featureName = currentFeatures.get(i).getFeatureName();
                            Integer featureID = wordsForwardIndex.get(featureName);

                            double prob = computeTermProbability(theta, featureID, modelPools.get(m));

                            modelPerplexity[m] = modelPerplexity[m] + Math.log(prob);
                            modelSaved[m] ++;
                        }
                    }
                }
            }
        }

        @Override
        public void sampleOverDocs(int modelID, List<Document> docs, int start, int end, int maxIter, int save){
            LOGGER.info("Start to testing.");
            long overall_startTime = System.currentTimeMillis();
            int num_d = 0;
            modelPerplexity = new double[modelPools.size()];
            modelSaved = new int[modelPools.size()];

            for (int d = start; d < end; d++){
                sampleTestDoc(docs, 150, d);
                num_d++;
                LOGGER.info("Processed:" + d);
            }

            // average perplexity
            double totalAverage = 0.0;
            for (int m = 0; m < modelPools.size(); m++){
                modelPerplexity[m] = Math.exp(-modelPerplexity[m] / modelSaved[m]);
                LOGGER.info("Model " + m + "\t" + modelPerplexity[m]);
                totalAverage += modelPerplexity[m];
            }
            LOGGER.info("Total Average Perplexity:" + totalAverage / modelPools.size());
            LOGGER.info("Finished testing.");
            long overall_endTime = System.currentTimeMillis();
            LOGGER.info("Average Document (per-K)/Seconds " + Double.toString((num_d / 1000.0) /((overall_endTime - overall_startTime) / 1000.0)));
        }
    }

    protected Sampler getSampler(String samplerStr){
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
        return s;
    }

    protected HyperparameterOptimization getHyperOpt(String hyperOptStr){
        HyperparameterOptimization hyper = null;
        if (hyperOptStr != null){
            if ("none".equals(hyperOptStr)){
                hyper = null;
            }
            else if ("slice".equals(hyperOptStr)){
                hyper = new SliceSampling();
            }
            else{
                hyper = null;
            }
        }
        return hyper;
    }

    public void train(){
        int start = Integer.parseInt(cmdArg.getParam("start").toString());
        int end = Integer.parseInt(cmdArg.getParam("end").toString());
        if (start < 0)
            start = 0;
        if (end < 0)
            end = internalDocs.size();
        train(start, end);
    }

    public void train(int start, int end){
        initTrainModel();
        LOGGER.info("Start to perform Gibbs Sampling");
        LOGGER.info("MAX_ITER:" + MAX_ITER);
        String samplerStr = cmdArg.getParam("LDASampler").toString();
        String hyperOptStr = cmdArg.getParam("LDAHyperOpt").toString();
        int save = Integer.parseInt(cmdArg.getParam("saveModel").toString());
        Sampler s = getSampler(samplerStr);
        HyperparameterOptimization hyper = getHyperOpt(hyperOptStr);
        ProcessDocuments p = new ProcessDocuments(s, hyper);
        s.setProcessor(p);
        p.sampleOverDocs(0, internalDocs, start, end, MAX_ITER, save);
        if (save == 1)
            saveModel(0);
    }

    public void initTestModels(){
        for (ModelCountainer m : modelPools){
            m.beta = new double[m.outsideBeta.size()];
            for (int v = 0; v < m.outsideBeta.size(); v++ ){
                // this is the default value
                m.beta[v] = 0.01;
            }
            m.betaSum = 0.0;
            for (Map.Entry<String, Double> entry : m.outsideBeta.entrySet()){
                String sKey = entry.getKey();
                Double sValue = entry.getValue();
                if (wordsForwardIndex.containsKey(sKey)){
                    int wordIndex = wordsForwardIndex.get(sKey);
                    m.beta[wordIndex] = sValue;
                    m.betaSum += sValue;
                }
                else{
                    LOGGER.info("[WARNING]: Term:" + sKey + " is not in the dictionary when constructing beta array.");
                }
            }

            m.alphaSum = 0.0;
            for (int k = 0; k < m.alpha.length; k ++){
                m.alphaSum += m.alpha[k];
            }

            m.wordTopicCounts = new ArrayList<int[]>();
            for (int k = 0; k < m.outsideWordTopicCounts.size(); k ++){
                int[] topicCounts = new int[m.alpha.length];
                m.wordTopicCounts.add(topicCounts);
            }
            for (Map.Entry<String, int[]> entry : m.outsideWordTopicCounts.entrySet()){
                String sKey = entry.getKey();
                if (wordsForwardIndex.containsKey(sKey)){
                    int wordIndex = wordsForwardIndex.get(sKey);
                    int[] wordCounts = entry.getValue();
                    for ( int k = 0; k < wordCounts.length; k ++) {
                        m.wordTopicCounts.get(wordIndex)[k] = wordCounts[k];
                    }
                }
                else{
                    LOGGER.info("[WARNING]: Term:" + sKey + " is not in the dictionary when constructing word topic counts.");
                }
            }

            if (m.alpha.length != TOPIC_NUM)
                TOPIC_NUM = m.alpha.length;

            m.computePhi(); // cache phi

            LOGGER.info("TOPIC NUM:" + TOPIC_NUM);
            LOGGER.info("Outside Term Num:" + m.outsideWordTopicCounts.size());
            LOGGER.info("Term Num:" + m.wordTopicCounts.size());
            LOGGER.info("alphaSum:" + m.alphaSum);
            LOGGER.info("betaSum:" + m.betaSum);
            LOGGER.info("Finished initializing model");
        }
    }

    public void test(){
        int start = Integer.parseInt(cmdArg.getParam("start").toString());
        int end = Integer.parseInt(cmdArg.getParam("end").toString());
        if (start < 0)
            start = 0;
        if (end < 0)
            end = internalDocs.size();
        test(start, end);
    }

    public void test(int start, int end){
        try {
            loadModel();
            rebuildIndex();
            initTestModels();
            LOGGER.info("Start to perform Gibbs Sampling");
            LOGGER.info("MAX_ITER:" + MAX_ITER);
            String samplerStr = cmdArg.getParam("LDASampler").toString();
            Sampler s = getSampler(samplerStr);
            ProcessDocuments p = new ProcessTestDocuments(s);
            s.setProcessor(p);
            p.sampleOverDocs(-1, internalDocs, start, end, MAX_ITER, 0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadModel() throws IOException {
        int multipleModels = Integer.parseInt(cmdArg.getParam("multipleModels").toString());
        String[] modelFileNames = null;
        LOGGER.info("Load Multiple Test Models:" + multipleModels);
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
            LOGGER.info("Trying to load " + modelFileName);
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
        if (!all) {
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
        cmdArg.setParam("invertedIndex", wordsInvertedIndex);
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
