package org.asf.connective.headers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class HeaderTest {

	@Test
	public void testMain() {
		// Test header handling
		// Make sure that with all functionality that it doesnt break
		HeaderCollection testCol = new HeaderCollection();
		testCol.addHeader("test", "test2");
		testCol.addHeader("test", "test3");
		testCol.addHeader("test 2", "abc");
		testCol.addHeader("Test 2", "def");
		testCol.addHeader("test 3", "abc");
		testCol.addHeader("Test 3", "def", false);
		testCol.addHeader("Test 4", "abc\ndef");
		testCol.addHeader("Test 5", "abc\ndef\\nghi");
		assertTrue(testCol.getHeaderValue("test 2").equals("abc"));
		assertTrue(testCol.getHeaderValue("Test 2").equals("abc"));
		assertTrue(testCol.getHeaderValues("Test 2").length == 2);
		assertTrue(testCol.getHeaderValues("Test 2")[0].equals("abc"));
		assertTrue(testCol.getHeaderValues("Test 2")[1].equals("def"));
		assertTrue(testCol.getHeaderValue("test 3").equals("def"));
		assertTrue(testCol.getHeaderValue("test 4").equals("abc\ndef"));
		assertTrue(testCol.getHeaderValue("test 5").equals("abc\ndef\\nghi"));
		testCol.removeHeader("test 3");
		assertTrue(testCol.getHeaderValue("test 3") == null);
		testCol.addHeader("test 3", "abc");
		testCol.addHeader("Test 3", "def", false);
		assertTrue(testCol.toString().replace("\r", "").equals("test: test2\n" + "test: test3\n" + "test 2: abc\n"
				+ "test 2: def\n" + "Test 4: abc\\ndef\n" + "Test 5: abc\\ndef\\\\nghi\n" + "test 3: def"));
	}

}
