/*
 * Copyright © 2013-2021 Metreeca srl
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

import org.assertj.core.api.*;
import org.eclipse.rdf4j.model.Statement;

import java.util.List;
import java.util.Optional;

import static com.metreeca.json.Values.indent;

import static org.eclipse.rdf4j.model.util.Models.isomorphic;

import static java.util.stream.Collectors.toList;


public final class FrameAssert extends AbstractAssert<FrameAssert, Frame> {

	public static FrameAssert assertThat(final Frame frame) {
		return new FrameAssert(frame);
	}

	public static OptionalAssert<Frame> assertThat(final Optional<Frame> frame) {
		return Assertions.assertThat(frame).usingValueComparator((x, y) ->
				isomorphic(() -> x.model().iterator(), () -> y.model().iterator()) ? 0 : -1
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private FrameAssert(final Frame actual) {
		super(actual, FrameAssert.class);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public FrameAssert isEmpty() {

		isNotNull();

		if ( !actual.empty() ) {
			failWithMessage("expected frame to be empty but was <\n%s\n>", actual.format());
		}

		return this;
	}

	public FrameAssert isNotEmpty() {

		isNotNull();

		if ( !actual.empty() ) {
			failWithMessage("expected frame not to be empty");
		}

		return this;
	}


	public FrameAssert isIsomorphicTo(final Frame frame) {

		if ( frame == null ) {
			throw new NullPointerException("null frame");
		}

		isNotNull();

		final List<Statement> a=actual.model().collect(toList());
		final List<Statement> f=frame.model().collect(toList());

		if ( !isomorphic(a, f) ) {
			failWithMessage(
					"expected frame <\n%s\n> to be isomorphic to <\n%s\n>",
					indent(actual.format()), indent(frame.format())
			);
		}

		return this;
	}

}
