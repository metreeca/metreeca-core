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

package com.metreeca.gcp.services;

import com.metreeca.json.*;
import com.metreeca.rest.Setup;
import com.metreeca.rest.services.Engine;

import java.util.Optional;


/**
 * Model-driven Google Cloud Datastore engine.
 *
 * <p>Handles model-driven CRUD operations on linked data resources stored in the shared {@linkplain Datastore
 * datastore}.</p>
 */
public final class DatastoreEngine extends Setup<DatastoreEngine> implements Engine {

	@Override public Optional<Frame> create(final Frame frame, final Shape shape) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	@Override public Optional<Frame> relate(final Frame frame, final Query query) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	@Override public Optional<Frame> update(final Frame frame, final Shape shape) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	@Override public Optional<Frame> delete(final Frame frame, final Shape shape) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

}
