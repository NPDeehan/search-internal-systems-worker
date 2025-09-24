package com.example.camunda;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple unit tests that don't require Spring context
 */
public class SimpleUnitTest {

    @Test
    public void testBasicJavaFunctionality() {
        // Test basic Java functionality
        String expected = "Hello World";
        String actual = "Hello" + " " + "World";
        
        assertEquals(expected, actual);
    }

    @Test
    public void testMathOperations() {
        int result = 2 + 2;
        assertEquals(4, result);
        
        assertTrue(result > 0);
        assertFalse(result < 0);
    }

    @Test
    public void testStringOperations() {
        String test = "Camunda Worker Test";
        
        assertNotNull(test);
        assertTrue(test.contains("Camunda"));
        assertTrue(test.contains("Worker"));
        assertEquals(19, test.length());
    }
}
