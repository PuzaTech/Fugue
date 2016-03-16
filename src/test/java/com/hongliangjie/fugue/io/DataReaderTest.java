package com.hongliangjie.fugue.io;

import com.hongliangjie.fugue.Message;
import com.hongliangjie.fugue.serialization.Document;
import com.hongliangjie.fugue.serialization.Feature;
import org.junit.Test;

import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by liangjie on 3/6/16.
 */
public class DataReaderTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testRead() throws Exception {
        DataReader r = new DataReader();
        Message msg = new Message();
        URL testFileURL = this.getClass().getClassLoader().getResource("ap-test.json");
        if (testFileURL != null) {
            msg.setParam("inputFile", testFileURL.getPath());
            msg.setParam("topk", "1");
        }
        msg = r.read(msg);
        // check document
        List<Document> docs = (List<Document>)msg.getParam("docs");
        assertEquals("DataReader docLength", 1, docs.size());
        // check doc id
        Document d = docs.get(0);
        assertEquals("DataReader docId", "0", d.getDocId());
        List<Feature> features = d.getFeatures();
        assertEquals("DataReader featuresLength", 263, features.size());
        Feature f1 = features.get(0);
        assertEquals("DataReader f1 Type", "TOKEN", f1.getFeatureType());
        assertEquals("DataReader f1 Name", "i", f1.getFeatureName());
        assertEquals("DataReader f1 Value", true, Math.abs(f1.getFeatureValue() - 1.0) < 1e-10);
        Feature f2 = features.get(1);
        assertEquals("DataReader f2 Type", "TOKEN", f2.getFeatureType());
        assertEquals("DataReader f2 Name", "maurice", f2.getFeatureName());
        assertEquals("DataReader f2 Value", true, Math.abs(f2.getFeatureValue() - 1.0) < 1e-10);
        Feature f3 = features.get(2);
        assertEquals("DataReader f3 Type", "TOKEN", f3.getFeatureType());
        assertEquals("DataReader f3 Name", "adult", f3.getFeatureName());
        assertEquals("DataReader f3 Value", true, Math.abs(f3.getFeatureValue() - 1.0) < 1e-10);
    }
}