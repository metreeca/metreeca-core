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

package com.metreeca.rest.formats;

import com.metreeca.form.Shape;
import com.metreeca.rest.Format;


/**
 * Shape body format.
 *
 * <p>Associates a message with an RDF shape describing its expected payload.</p>
 */
public final class ShapeFormat implements Format<Shape> {

	private static final ShapeFormat Instance=new ShapeFormat();


	/**
	 * Retrieves the shape body format.
	 *
	 * @return the singleton shape body format instance
	 */
	public static ShapeFormat shape() {
		return Instance;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private ShapeFormat() {}

}
