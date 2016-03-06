package com.hongliangjie.fugue.serialization;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by hongliangjie on 3/5/2016.
 */
public class DocumentTest {

    @Test
    public void testGetFeatures() throws Exception {

    }

    @Test
    public void testSetFeatures() throws Exception {

    }

    @Test
    public void testToString() throws Exception {

    }

    @Test
    public void testGetDocId() throws Exception {
        Document d = new Document();
        d.setDocId("abc");
        assertEquals("Testing DocId", "abc", d.getDocId());
    }

    @Test
    public void testSetDocId() throws Exception {

    }
}