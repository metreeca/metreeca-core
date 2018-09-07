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

package com.metreeca.next;

import com.metreeca.form.things.Transputs;

import java.io.InputStream;
import java.io.Reader;


/**
 * Content source.
 *
 * <p>Provides on-demand textual/binary access to a content source.</p>
 *
 * <p>Must override at least one of the {@link #reader()}/{@link #input()} methods.</p>
 */
public interface Source {

	/**
	 * Retrieves the source reader.
	 *
	 * @return a reader for the source textual content, by default generated from the the source {@linkplain #input()
	 * input} stream using the {@linkplain Transputs#UTF8 default} character encoding.
	 *
	 * @throws IllegalStateException if the source reader is not available
	 */
	public default Reader reader() throws IllegalStateException { return Transputs.reader(input()); }

	/**
	 * Retrieves the source input stream.
	 *
	 * @return an input stream for the source binary content, by default generated from the the source {@linkplain #reader()
	 * reader} using the {@linkplain Transputs#UTF8 default} character encoding.
	 *
	 * @throws IllegalStateException if the source input stream is not available
	 */
	public default InputStream input() throws IllegalStateException { return Transputs.input(reader()); }

}
