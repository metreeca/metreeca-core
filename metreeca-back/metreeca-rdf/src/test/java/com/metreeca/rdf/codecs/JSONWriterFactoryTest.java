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

	private final RDFWriterRegistry instance=RDFWriterRegistry.getInstance();


	@Test void testFormatRegisteredWithRegistry() {

		assertThat(instance.getFileFormatForMIMEType(JSONCodec.JSONFormat.getDefaultMIMEType()))
				.as("by mime type")
				.contains(JSONCodec.JSONFormat);

		assertThat(instance.getFileFormatForFileName("test."+JSONCodec.JSONFormat.getDefaultFileExtension()))
				.as("by extension")
				.contains(JSONCodec.JSONFormat);

	}

	@Test void testFactoryRegisteredWithRegistry() {
		assertThat(instance.get(JSONCodec.JSONFormat))
				.as("factory registered")
				.containsInstanceOf(JSONWriterFactory.class);
	}

}
