/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.xml.actions;

import org.junit.jupiter.api.Test;

import static com.metreeca.xml.actions.XPath.decode;
import static org.assertj.core.api.Assertions.assertThat;


final class XPathTest {

	@Test void decodeNumericEntities() {
		assertThat(decode("Italy&#x2019;s &#8220;most powerful&#8221; car"))
				.isEqualTo("Italy’s “most powerful” car");
	}

}
