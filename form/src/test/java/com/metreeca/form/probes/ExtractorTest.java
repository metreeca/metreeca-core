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

package com.metreeca.form.probes;

import com.metreeca.form.Form;
import com.metreeca.form.Shape;

import org.eclipse.rdf4j.model.Statement;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.metreeca.form.probes.Evaluator.fail;
import static com.metreeca.form.probes.Evaluator.pass;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.ValuesTest.*;
import static com.metreeca.form.truths.ModelAssert.assertThat;

import static java.util.stream.Collectors.toSet;


final class ExtractorTest {

	private Set<Statement> extract(final Shape shape) {
		return shape.map(new Extractor(small(), set(item("employees/1370")))).collect(toSet());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testExtractShapeEnvelope() {

		final Shape shape=Employee
				.map(new Redactor(Form.task, Form.relate))
				.map(new Redactor(Form.view, Form.detail))
				.map(new Redactor(Form.mode, Form.convey))
				.map(new Redactor(Form.role, Salesman))
				.map(new Optimizer());

		assertThat(extract(shape))
				.isIsomorphicTo(decode("<employees/1370> a :Employee ;\n"
						+"\trdfs:label \"Gerard Hernandez\" ;\n"
						+"\t:code \"1370\" ;\n"
						+"\t:surname \"Hernandez\" ;\n"
						+"\t:forename \"Gerard\" ;\n"
						+"\t:email \"ghernande@example.com\" ;\n"
						+"\t:title \"Sales Rep\" .\n")
				);
	}

	@Test void testIgnoreEmptyShapes() {

		assertThat(extract(pass())).isEmpty();
		assertThat(extract(fail())).isEmpty();

	}

}
