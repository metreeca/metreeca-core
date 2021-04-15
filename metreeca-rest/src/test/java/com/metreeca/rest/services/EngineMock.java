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

package com.metreeca.rest.services;

import com.metreeca.json.*;
import com.metreeca.json.queries.*;

import org.eclipse.rdf4j.model.Value;

import java.util.*;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

final class EngineMock implements Engine {

	private final Map<Value, Frame> resources;


	EngineMock(final Collection<Frame> resources) {
		this.resources=resources.stream().collect(toMap(Frame::focus, identity()));
	}


	@Override public Optional<Frame> create(final Frame frame, final Shape shape) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	@Override public Optional<Frame> relate(final Frame frame, final Query query) {
		return query.map(new OptionalProbe());
	}

	@Override public Optional<Frame> update(final Frame frame, final Shape shape) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	@Override public Optional<Frame> delete(final Frame frame, final Shape shape) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class OptionalProbe extends Query.Probe<Optional<Frame>> {

		@Override public Optional<Frame> probe(final Items items) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public Optional<Frame> probe(final Terms terms) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public Optional<Frame> probe(final Stats stats) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public Optional<Frame> probe(final Query query) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

	}

}
