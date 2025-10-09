package allTests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import models.Punycode;

class PunycodeTest {

	@Test
	void testToPunycode() {
		assertEquals("xn--hkyrky-ptac70bc.cz",Punycode.toPunycode("háčkyčárky.cz"));
	}
	
	@Test
	void testFromPunnycode() {
		assertEquals("háčkyčárky.cz",Punycode.fromPunnycode("xn--hkyrky-ptac70bc.cz"));
	}

}
