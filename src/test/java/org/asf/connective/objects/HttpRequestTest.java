package org.asf.connective.objects;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.asf.connective.headers.HeaderCollection;
import org.junit.jupiter.api.Test;

public class HttpRequestTest {

	@Test
	public void testSanitizeRequestPath() {
		HttpRequest req = new HttpRequest(null, 0, new HeaderCollection(), "HTTP/1.1", "geT",
				"////some/not/so/////very/clean\\path/to//////clean/");
		assertTrue(req.getRequestPath().equals("/some/not/so/very/clean/path/to/clean"));
		assertTrue(req.getRequestMethod().equals("GET"));
	}

	@Test
	public void testSanityChecks() {
		assertThrows(IllegalArgumentException.class, () -> {
			new HttpRequest(null, 0, new HeaderCollection(), "HTTP/1.1", "geT", "../illegalpath");
		});
		assertThrows(IllegalArgumentException.class, () -> {
			new HttpRequest(null, 0, new HeaderCollection(), "HTTP/1.1", "geT",
					"/a/disguised/path/../../../..../that/may/be/missed");
		});
	}

	@Test
	public void testDecodeRequestPath() {
		HttpRequest req = new HttpRequest(null, 0, new HeaderCollection(), "HTTP/1.1", "geT",
				"some%20encoded+url path");
		assertTrue(req.getRequestPath().equals("/some encoded url path"));
	}

	@Test
	public void testDecodeRequestQuery() {
		HttpRequest req = new HttpRequest(null, 0, new HeaderCollection(), "HTTP/1.1", "geT",
				"/resource?some=query&that=is%20not&very=clean&at=the+moment");
		assertTrue(req.getRequestQueryParameters().get("some").equals("query"));
		assertTrue(req.getRequestQueryParameters().get("that").equals("is not"));
		assertTrue(req.getRequestQueryParameters().get("very").equals("clean"));
		assertTrue(req.getRequestQueryParameters().get("at").equals("the moment"));
		assertTrue(req.getRequestQuery().equals("some=query&that=is%20not&very=clean&at=the+moment"));
	}
}
