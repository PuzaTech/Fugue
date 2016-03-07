package com.hongliangjie.fugue.serialization;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by hongliangjie on 3/5/2016.
 */
public class DocumentTest {

    private Document buildDocument(){
        Document d = new Document();
        d.setDocId("abc");
        List<Feature> features = new ArrayList<Feature>();

        Feature f = new Feature();
        f.setFeatureType("TEST_TYPE"); f.setFeatureName("TEST_NAME"); f.setFeatureValue(1.0);
        features.add(f);

        d.setFeatures(features);
        return d;
    }

    @Test
    public void testGetFeatures() throws Exception {
        Document d = buildDocument();
        List<Feature> features = d.getFeatures();
        Feature f = features.get(0);
        assertEquals("Testing getFeatures", "TEST_TYPE", f.getFeatureType());
    }

    @Test
    public void testSetFeatures() throws Exception {
        Document d = buildDocument();
        List<Feature> features = d.getFeatures();
        Feature f = features.get(0);
        assertEquals("Testing setFeatures", "TEST_TYPE", f.getFeatureType());
    }

    @Test
    public void testToString() throws Exception {
        Document d = buildDocument();
        String dStr = d.toString();
        assertEquals("Testing Document toString", "abc 1", dStr);

    }

    @Test
    public void testGetDocId() throws Exception {
        Document d = buildDocument();
        assertEquals("Testing DocId", "abc", d.getDocId());
    }

    @Test
    public void testSetDocId() throws Exception {
        Document d = buildDocument();
        assertEquals("Testing DocId", "abc", d.getDocId());
    }
}