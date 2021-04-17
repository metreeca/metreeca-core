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

import com.metreeca.rest.Toolbox;
import com.metreeca.rest.services.EngineTest;

import static com.metreeca.gcp.services.Datastore.datastore;
import static com.metreeca.rest.services.Engine.engine;

final class DatastoreEngineTest extends EngineTest {

	protected void exec(final Runnable... tasks) {
		DatastoreTest.exec(datastore -> new Toolbox()

				.set(datastore(), () -> datastore)
				.set(engine(), DatastoreEngine::new)
				.exec(tasks)
				.clear()
		);
	}

}
