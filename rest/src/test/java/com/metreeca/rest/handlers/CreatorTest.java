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

package com.metreeca.rest.handlers;

import com.metreeca.rest.*;
import com.metreeca.rest.services.Engine;
import com.metreeca.tree.Shape;

import java.util.function.Supplier;

import static com.metreeca.rest.services.Engine.engine;


final class CreatorTest {

	private void exec(final Runnable... tasks) {
		new Context()
				.set(engine(), () -> new Engine() {

					@Override public <R> R exec(final Supplier<R> task) {
						return task.get();
					}

					@Override public Shape container(final Shape shape) {
						throw new UnsupportedOperationException("to be implemented"); // !!! tbi
					}

					@Override public Shape resource(final Shape shape) {
						throw new UnsupportedOperationException("to be implemented"); // !!! tbi
					}

					@Override public <M extends Message<M>> Result<M, Failure> trim(final M message) {
						throw new UnsupportedOperationException("to be implemented"); // !!! tbi
					}

					@Override public <M extends Message<M>> Result<M, Failure> validate(final M message) {
						throw new UnsupportedOperationException("to be implemented"); // !!! tbi
					}

					@Override public Future<Response> handle(final Request request) {
						throw new UnsupportedOperationException("to be implemented"); // !!! tbi
					}

				})
				.exec(tasks)
				.clear();
	}


	//@Test void test() {
	//	exec(() -> new Creator( )
	//
	//			.handle(new Request())
	//
	//			.accept(response -> assertThat(response)
	//			)
	//	);
	//}

}
