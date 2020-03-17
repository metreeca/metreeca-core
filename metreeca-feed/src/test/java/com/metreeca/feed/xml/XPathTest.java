/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.feed.xml;

import org.junit.jupiter.api.Test;

import static com.metreeca.feed.xml.XPath.decode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;


final class XPathTest {

	@Test void decodeNumericEntities() {
		assertThat(decode("Italy&#x2019;s &#8220;most powerful&#8221; car"))
				.isEqualTo("Italy’s “most powerful” car");
	}

}
