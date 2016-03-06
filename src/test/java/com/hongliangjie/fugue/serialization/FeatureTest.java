package com.hongliangjie.fugue.serialization;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by hongliangjie on 3/5/2016.
 */
public class FeatureTest {

    @Test
    public void testGetFeatureType() throws Exception {
        Feature f = new Feature();
        f.setFeatureType("abc");
        assertEquals("Testing FeatureType", "abc", f.getFeatureType());
    }

    @Test
    public void testGetFeatureName() throws Exception {
        Feature f = new Feature();
        f.setFeatureName("abc");
        assertEquals("Testing FeatureName", "abc", f.getFeatureName());
    }

    @Test
    public void testGetFeatureValue() throws Exception {
        Feature f = new Feature();
        f.setFeatureValue(1.0);
        assertEquals("Testing FeatureValue", true, Math.abs(f.getFeatureValue() - 1.0) < 1e-10);
    }
}