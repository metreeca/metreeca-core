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

package com.metreeca.next.formats;

import com.metreeca.next.Format;
import com.metreeca.next.Message;
import com.metreeca.next.Target;

import java.util.Optional;


/**
 * RDF body format.
 */
@Deprecated public final class RDF implements Format<Crate> {

	/**
	 * The singleton RDF body format.
	 */
	public static final Format<Crate> Format=new RDF();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private RDF() {} // singleton


	/**
	 * @return the optional RDF body representation of {@code message}, as retrieved from its {@link In#Format}
	 * representation, if present; an empty optional, otherwise
	 */
	@Override public Optional<Crate> get(final Message<?> message) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	/**
	 * Configures the {@link Out#Format} representation of {@code message} to write the RDF {@code value} to the
	 * accepted {@link Target}.
	 */
	@Override public void set(final Message<?> message, final Crate value) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

}
