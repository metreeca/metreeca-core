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

package com.metreeca.tray.iam;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;


public abstract class DigestTest {

	protected abstract Digest digest();


	@Test public void testRandomize() {
		assertNotEquals("randomized", digest().digest("secret"), digest().digest("secret"));
	}

	@Test public void testRecognize() {
		assertTrue("recognized", digest().verify("secret", digest().digest("secret")));
	}

	@Test public void testReject() {
		assertFalse("rejected", digest().verify("public", digest().digest("secret")));
	}

	@Test(expected=IllegalArgumentException.class) public void testMalformed() {
		digest().verify("secret", "malformed");
	}

}
