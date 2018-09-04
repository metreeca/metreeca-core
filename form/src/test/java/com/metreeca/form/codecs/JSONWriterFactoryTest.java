/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.form.codecs;

import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.junit.Test;

import static org.junit.Assert.assertTrue;


public class JSONWriterFactoryTest {

	@Test public void testFormatRegisteredWithRegistry() {

		assertTrue("by mime type", RDFWriterRegistry.getInstance()
				.getFileFormatForMIMEType(JSONAdapter.JSONFormat.getDefaultMIMEType())
				.filter(format -> format.equals(JSONAdapter.JSONFormat))
				.isPresent());

		assertTrue("by extension", RDFWriterRegistry.getInstance()
				.getFileFormatForFileName("test."+JSONAdapter.JSONFormat.getDefaultFileExtension())
				.filter(format -> format.equals(JSONAdapter.JSONFormat))
				.isPresent());

	}

	@Test public void testFactoryRegisteredWithRegistry() {
		assertTrue("factory registered", RDFWriterRegistry.getInstance()
				.get(JSONAdapter.JSONFormat)
				.filter(factory -> factory instanceof JSONWriterFactory)
				.isPresent());

	}

}
