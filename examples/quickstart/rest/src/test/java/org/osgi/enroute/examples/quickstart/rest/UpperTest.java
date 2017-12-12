package org.osgi.enroute.examples.quickstart.rest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UpperTest {
    
    @Test
    public void testUpper() {
        Upper upper = new Upper();
        
        assertEquals("FOO",upper.toUpper("foo"));
    }
}
