/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.xml.actions;

import com.metreeca.rest.Either;
import com.metreeca.rest.Xtream;
import com.metreeca.xml.formats.XMLFormat;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static com.metreeca.xml.actions.XPath.decode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;


final class XPathTest {

	@Test void testDecodeNumericEntities() {
		assertThat(decode("Italy&#x2019;s &#8220;most powerful&#8221; car"))
				.isEqualTo("Italy’s “most powerful” car");
	}

	@Test void testUseEnclosingNamespaces() {
		assertThat(Xtream

				.of("<ns:x xmlns:ns='http://example.com/o'><ns:y><ns:z>text</ns:z></ns:y></ns:x>")

				.map(x -> XMLFormat.xml(new ByteArrayInputStream(x.getBytes(UTF_8))))

				.optMap(Either::get)

				.flatMap(new XPath<>(m -> m.nodes("//ns:y")))
				.flatMap(new XPath<>(m1 -> m1.strings("//ns:z")))

		).containsExactly("text");
	}

}
