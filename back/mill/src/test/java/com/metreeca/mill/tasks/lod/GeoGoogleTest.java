/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.mill.tasks.lod;

import com.metreeca.jeep.IO;
import com.metreeca.mill.Mill;
import com.metreeca.mill.tasks.Item;
import com.metreeca.mill.tasks.Peek;
import com.metreeca.mill.tasks.Pipe;
import com.metreeca.mill.tasks.xml.XSLT;

import org.junit.Test;


public final class GeoGoogleTest {

	@Test public void work() {

		new Mill().execute(new Pipe(

				new Item().text(IO.text(GeoGoogle.class, "GeoGoogle.xml")),

				new XSLT()

						.transform(IO.text(GeoGoogle.class, "GeoGoogle.xsl")),


				new Peek()
		));
	}

}
