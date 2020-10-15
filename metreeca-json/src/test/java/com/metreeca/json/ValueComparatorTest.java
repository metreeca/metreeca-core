
/*
 * Copyright © 2013-2020 Metreeca srl
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

package com.metreeca.json;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


final class ValueComparatorTest {

	private final ValueFactory factory=SimpleValueFactory.getInstance();

	private final BNode bnode1=factory.createBNode();
	private final BNode bnode2=factory.createBNode();

	private final IRI uri1=factory.createIRI("http://script.example/Latin");
	private final IRI uri2=factory.createIRI("http://script.example/Кириллица");
	private final IRI uri3=factory.createIRI("http://script.example/日本語");

	private final Literal typed1=factory.createLiteral("http://script.example/Latin", XSD.STRING);

	private final ValueComparator cmp=new ValueComparator();


	@Test void testBothNull() {
		assertTrue(cmp.compare(null, null) == 0);
	}

	@Test void testLeftNull() {
		assertTrue(cmp.compare(null, typed1) < 0);
	}

	@Test void testRightNull() {
		assertTrue(cmp.compare(typed1, null) > 0);
	}


	@Test void testBothBnode() {
		assertTrue(cmp.compare(bnode1, bnode1) == 0);
		assertTrue(cmp.compare(bnode2, bnode2) == 0);
		assertTrue(cmp.compare(bnode1, bnode2) != cmp.compare(bnode2, bnode1));
		assertTrue(cmp.compare(bnode1, bnode2) == -1*cmp.compare(bnode2, bnode1));
	}

	@Test void testLeftBnode() {
		assertTrue(cmp.compare(bnode1, typed1) < 0);
	}

	@Test void testRightBnode() {
		assertTrue(cmp.compare(typed1, bnode1) > 0);
	}


	@Test void testBothURI() {
		assertTrue(cmp.compare(uri1, uri1) == 0);
		assertTrue(cmp.compare(uri1, uri2) < 0);
		assertTrue(cmp.compare(uri1, uri3) < 0);
		assertTrue(cmp.compare(uri2, uri1) > 0);
		assertTrue(cmp.compare(uri2, uri2) == 0);
		assertTrue(cmp.compare(uri2, uri3) < 0);
		assertTrue(cmp.compare(uri3, uri1) > 0);
		assertTrue(cmp.compare(uri3, uri2) > 0);
		assertTrue(cmp.compare(uri3, uri3) == 0);
	}

	@Test void testLeftURI() {
		assertTrue(cmp.compare(uri1, typed1) < 0);
	}

	@Test void testRightURI() {
		assertTrue(cmp.compare(typed1, uri1) > 0);
	}


	/**
	 * Tests whether xsd:int's are properly sorted in a list with mixed value types.
	 */
	@Test void testOrder1() {
		final Literal en4=factory.createLiteral("4", "en");
		final Literal int10=factory.createLiteral(10);
		final Literal int9=factory.createLiteral(9);

		final List<Literal> valueList=Arrays.asList(en4, int10, int9);
		Collections.sort(valueList, cmp);

		assertTrue(valueList.indexOf(int9) < valueList.indexOf(int10));
	}

	/**
	 * Tests whether various numerics are properly sorted in a list with mixed value types.
	 */
	@Test void testOrder2() {
		final Literal en4=factory.createLiteral("4", "en");
		final Literal int10=factory.createLiteral(10);
		final Literal int9=factory.createLiteral(9);
		final Literal plain9=factory.createLiteral("9");
		final Literal integer5=factory.createLiteral("5", XSD.INTEGER);
		final Literal float9=factory.createLiteral(9f);
		final Literal plain4=factory.createLiteral("4");
		final Literal plain10=factory.createLiteral("10");

		final List<Literal> valueList=Arrays.asList(en4, int10, int9, plain9, integer5, float9, plain4, plain10);
		valueList.sort(cmp);

		assertTrue(valueList.indexOf(integer5) < valueList.indexOf(float9));
		assertTrue(valueList.indexOf(integer5) < valueList.indexOf(int9));
		assertTrue(valueList.indexOf(integer5) < valueList.indexOf(int10));
		assertTrue(valueList.indexOf(float9) < valueList.indexOf(int10));
		assertTrue(valueList.indexOf(int9) < valueList.indexOf(int10));
		assertTrue(valueList.indexOf(int9) < valueList.indexOf(int10));
	}

	/**
	 * Tests whether numerics of different types are properly sorted. The list also contains a datatype that would be
	 * sorted between the numerics if the datatypes were to be sorted alphabetically.
	 */
	@Test void testOrder3() {
		final Literal year1234=factory.createLiteral("1234", XSD.GYEAR);
		final Literal float2000=factory.createLiteral(2000f);
		final Literal int1000=factory.createLiteral(1000);

		final List<Literal> valueList=Arrays.asList(year1234, float2000, int1000);
		Collections.sort(valueList, cmp);
		assertTrue(valueList.indexOf(int1000) < valueList.indexOf(float2000));
	}


	@Test void testNonStrictComparisons() {
		cmp.setStrict(false);
		assertFalse(cmp.isStrict());
		final Literal date1=factory.createLiteral("2019-09-02", XSD.DATE);
		final Literal date2=factory.createLiteral("2018", XSD.GYEAR);
		assertTrue(cmp.compare(date1, date2) > 0);
	}

	@Test void testStrictComparisons() {
		cmp.setStrict(true);
		assertTrue(cmp.isStrict());
		final Literal date1=factory.createLiteral("2019-09-02", XSD.DATE);
		final Literal date2=factory.createLiteral("2018", XSD.GYEAR);
		assertTrue(cmp.compare(date1, date2) < 0);
	}

}
