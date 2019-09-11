/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.rdf.codecs;

import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


final class JSONWriterFactoryTest {

	@Test void testFormatRegisteredWithRegistry() {

		assertThat(RDFWriterRegistry.getInstance()
				.getFileFormatForMIMEType(JSONCodec.JSONFormat.getDefaultMIMEType())
				.filter(format -> format.equals(JSONCodec.JSONFormat))
				.isPresent()).as("by mime type").isTrue();

		assertThat(RDFWriterRegistry.getInstance()
				.getFileFormatForFileName("test."+JSONCodec.JSONFormat.getDefaultFileExtension())
				.filter(format -> format.equals(JSONCodec.JSONFormat))
				.isPresent()).as("by extension").isTrue();

	}

	@Test void testFactoryRegisteredWithRegistry() {
		assertThat(RDFWriterRegistry.getInstance()
				.get(JSONCodec.JSONFormat)
				.filter(factory -> factory instanceof JSONWriterFactory)
				.isPresent()).as("factory registered").isTrue();

	}

}
