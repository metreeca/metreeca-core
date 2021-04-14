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

import com.metreeca.json.shifts.*;

import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.function.Function.identity;

public final class ShiftEvaluator extends Shift.Probe<Stream<Frame>> {

	private final Frame frame;


	ShiftEvaluator(final Frame frame) {
		this.frame=frame;
	}


	@Override public Stream<Frame> probe(final Step step) {
		return frame.frames(step.iri());
	}

	@Override public Stream<Frame> probe(final Seq seq) {

		Function<Stream<Frame>, Stream<Frame>> pipe=identity();

		for (final Path path : seq.paths()) {
			pipe=pipe.andThen(frames -> frames.flatMap(frame1 -> path.map(new ShiftEvaluator(frame1))));
		}

		return pipe.apply(Stream.of(frame));
	}

	@Override public Stream<Frame> probe(final Alt alt) {
		return alt.paths().stream().flatMap(path -> path.map(this));
	}


	@Override public Stream<Frame> probe(final Shift shift) {
		throw new UnsupportedOperationException(shift.toString());
	}

}
