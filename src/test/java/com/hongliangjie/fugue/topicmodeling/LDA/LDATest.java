package com.hongliangjie.fugue.topicmodeling.lda;

import com.hongliangjie.fugue.Message;
import com.hongliangjie.fugue.io.DataReader;
import com.hongliangjie.fugue.serialization.Document;
import com.hongliangjie.fugue.serialization.Feature;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by liangjie on 3/6/16.
 */
public class LDATest {

    @Test
    public void testLoadModel(){
        URL testFileURL = this.getClass().getClassLoader().getResource("model.ap.json");
        String testFileName = testFileURL.getPath();
        Message msg = new Message();
        msg.setParam("modelFile", testFileName);
        msg.setParam("multipleModels", 0);
        msg.setParam("topics", 10);
        msg.setParam("iters", 50);
        msg.setParam("saveModel", 0);
        msg.setParam("random", "native");
        msg.setParam("exp", 1);
        msg.setParam("log", 1);
        LDA lda = new LDA();
        lda.setMessage(msg);
        try {
            lda.loadModel();
            assertEquals("Load Finished", true, true);
        }
        catch (IOException e){
            assertEquals("Load Finished Failed", true, false);
        }

    }

    @Test
    public void testRebuildIndex() throws Exception {
        Message msg = prepareDocuments(100);
        LDA lda = new LDA();
        lda.setMessage(msg);
        lda.rebuildIndex();
        Message returnMsg = lda.getMessage();
        HashMap<String, Integer> wordsForwardIndex = (HashMap<String, Integer>)returnMsg.getParam("forwardIndex");
        HashMap<Integer, String> wordsInvertedIndex = (HashMap<Integer, String>)returnMsg.getParam("invertedIndex");
        assertEquals("Equal Index Size", true, wordsForwardIndex.size() == wordsInvertedIndex.size());
    }

    @Test
    public void testTest(){
        try {
            Message msg = prepareDocuments(1000000);
            URL testFileURL = this.getClass().getClassLoader().getResource("model.ap.json");
            String testFileName = testFileURL.getPath();
            msg.setParam("modelFile", testFileName);
            msg.setParam("multipleModels", 0);
            msg.setParam("topics", 10);
            msg.setParam("iters", 50);
            msg.setParam("saveModel", 0);
            msg.setParam("random", "native");
            msg.setParam("exp", 1);
            msg.setParam("log", 1);
            msg.setParam("LDASampler", "binary");
            msg.setParam("random", "deterministic");
            LDA lda = new LDA();
            lda.setMessage(msg);
            lda.test();
        }
        catch (Exception e){
            assertEquals("LDA test failed.", true, false);
        }
    }


    private class DeepTestLDA extends LDA{


        protected class TestProcesser extends ProcessDocuments{
            protected Sampler sampler2;
            TestProcesser(Sampler g1, Sampler g2){
                super(g1);
                sampler2 = g2;
            }

            @Override
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
                    double[] backupP = new double[TOPIC_NUM];
                    for(int k = 0; k < TOPIC_NUM; k++){
                        backupP[k] = sample_buffer[k];
                    }

                    int log_topic = sampler2.draw(modelID, featureIndex, randomRV);

                    for(int k = 0; k < TOPIC_NUM; k++){
                        double e = Math.abs(Math.exp(sample_buffer[k]) - backupP[k]);
                        assertEquals("probability is not consistent", true, e < 1e-10);
                    }

                    assertEquals("normal sampling is not consistent with log sampling", new_topic, log_topic);

                    docTopicBuffer[new_topic]++;
                    modelPools.get(modelID).wordTopicCounts.get(featureIndex)[new_topic]++;
                    modelPools.get(modelID).topicCounts[new_topic]++;
                    docTopicAssignment.set(pos, new_topic);

                    pos++;
                }
                return pos;
            }

            @Override
            public void sampleOverDocs(int modelID, List<Document> docs, int maxIter, int save){
                for (CURRENT_ITER = 0; CURRENT_ITER < maxIter; CURRENT_ITER++) {
                    LOGGER.info("Start to Iteration " + CURRENT_ITER);
                    for (int d = 0; d < docs.size(); d++) {
                        sampleOneDoc(docs, d, modelID);
                    }
                    LOGGER.info("Finished sampling.");
                    LOGGER.info("Finished Iteration " + CURRENT_ITER);

                    double likelihood = likelihood(modelID);
                    LOGGER.info("Iteration " + CURRENT_ITER + " Likelihood:" + Double.toString(likelihood));
                    countsCheck();
                }
            }
        }

        @Override
        public void train(){
            initTrainModel();
            countsCheck();
            LOGGER.info("Start to perform Gibbs Sampling");
            LOGGER.info("MAX_ITER:" + MAX_ITER);

            Sampler g1 = new GibbsBinarySampling();
            Sampler g2 = new GibbsLogSampling();
            ProcessDocuments p = new TestProcesser(g1,g2);
            g1.setProcessor(p);
            g2.setProcessor(p);
            p.sampleOverDocs(0, internalDocs, MAX_ITER, 0);
        }

        public void countsCheck() {
            int topic_sum = 0;

            for (int k = 0; k < TOPIC_NUM; k++) {
                topic_sum += modelPools.get(0).topicCounts[k];
            }

            assertEquals("topicCounts are not consistent with total token:", topic_sum, TOTAL_TOKEN);

            int term_topic_sum = 0;
            int[] topic_sums = new int[TOPIC_NUM];
            for (int v = 0; v < modelPools.get(0).wordTopicCounts.size(); v++) {
                int[] c = modelPools.get(0).wordTopicCounts.get(v);
                for (int k = 0; k < c.length; k++) {
                    term_topic_sum += c[k];
                    topic_sums[k] += c[k];
                }
            }

           assertEquals("wordTopicCounts are not consistent with total token:", term_topic_sum, TOTAL_TOKEN);


            for (int k = 0; k < TOPIC_NUM; k++) {
                assertEquals("topic_sums are not consistent with topicCounts:" + k, topic_sums[k], modelPools.get(0).topicCounts[k]);
            }

            int[] document_agg_topics = new int[TOPIC_NUM];
            for (int d = 0; d < internalDocs.size(); d++) {
                List<Integer> docTopicAssignment = modelPools.get(0).docTopicAssignments.get(d);
                int[] docTopicBuffer = modelPools.get(0).docTopicBuffers.get(d);
                int pos = internalDocs.get(d).getFeatures().size();
                int total_topic_sum = 0;
                for (int k = 0; k < TOPIC_NUM; k++) {
                    total_topic_sum += docTopicBuffer[k];
                }
                assertEquals("The total_topic_sum is not consistent with number of tokens:" + d, total_topic_sum, pos);

                for (Integer z : docTopicAssignment) {
                    document_agg_topics[z] += 1;
                }

                assertEquals("The total_tokens is not consistent with number of tokens:" + d, docTopicAssignment.size(), pos);
            }

            for (int k = 0; k < TOPIC_NUM; k++) {
                assertEquals("The aggregated topic assignment is not consistent with topicCounts:" + k, document_agg_topics[k], modelPools.get(0).topicCounts[k]);
            }
            LOGGER.info("Counts Verifized,");
        }
    }

    private Message prepareDocuments(int k) throws Exception{
        DataReader r = new DataReader();
        Message msg = new Message();
        URL testFileURL = this.getClass().getClassLoader().getResource("ap-test.json");
        msg.setParam("inputFile", testFileURL.getPath());
        msg.setParam("topk", k);
        msg.setParam("topics", 10);
        msg.setParam("iters", 50);
        msg.setParam("saveModel", 0);
        msg.setParam("random", "native");
        msg.setParam("exp", 1);
        msg.setParam("log", 1);
        msg = r.read(msg);
        return msg;
    }


    @Test
    public void testTrain() throws Exception {
        Message msg = prepareDocuments(100);
        // Here is to test whether normal sampling and log-sampling have the consistent topic assignments for each every step
        // Also this is a very deep test of internal structures
        DeepTestLDA m = new DeepTestLDA();
        m.setMessage(msg);
        m.train();
        assertEquals("Deep LDA Train Passed", true, true);
        // This is a black-box test
        msg.setParam("LDASampler", "normal");
        msg.setParam("random", "deterministic");
        msg.setParam("iters", 1);
        LDA normalLDA1 = new LDA();
        normalLDA1.setMessage(msg);
        normalLDA1.train();
        double l1 = normalLDA1.likelihood(0);
        double e1 = Math.abs(l1 - (-201179.71361803956));
        assertEquals("Deterministic Sampling", true, e1 < 1e-10);
        LDA normalLDA2 = new LDA();
        normalLDA2.setMessage(msg);
        normalLDA2.train();
        double l2 = normalLDA2.likelihood(0);
        double e2 = Math.abs(l2 - (-201179.71361803956));
        assertEquals("Deterministic Sampling", true, e2 < 1e-10);
        msg.setParam("LDASampler", "log");
        LDA logLDA = new LDA();
        logLDA.setMessage(msg);
        logLDA.train();
        double l3 = logLDA.likelihood(0);
        double e3 = Math.abs(l3 - (-201179.71361803956));
        assertEquals("Deterministic Sampling", true, e3 < 1e-10);
    }

}