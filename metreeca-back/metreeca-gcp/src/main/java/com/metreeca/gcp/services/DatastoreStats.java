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

import com.metreeca.json.Frame;
import com.metreeca.json.queries.Stats;
import com.metreeca.rest.Config;

import java.util.Optional;

final class DatastoreStats {

	DatastoreStats(final Config config) {}


	Optional<Frame> process(final org.eclipse.rdf4j.model.Value value, final Stats stats) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	//private Function<Response, Response> stats(final String path, final Stats stats) {
	//
	//	final class Range {
	//
	//		private final long count;
	//
	//		private final Value<?> min;
	//		private final Value<?> max;
	//
	//		private Range(final long count, final Value<?> min, final Value<?> max) {
	//
	//			this.count=count;
	//
	//			this.min=min;
	//			this.max=max;
	//		}
	//
	//
	//		private int order(final Range range) {
	//			return -Long.compare(count, range.count);
	//		}
	//
	//		private Range merge(final Range range) {
	//			return new Range(
	//					count+range.count,
	//					compare(min, range.min) <= 0 ? min : range.min,
	//					compare(max, range.max) >= 0 ? max : range.max
	//			);
	//		}
	//
	//		private void set(final BaseEntity.Builder<?, ?> container) {
	//
	//			container.set(DatastoreEngine.count, count);
	//			container.set(DatastoreEngine.min, min);
	//			container.set(DatastoreEngine.max, max);
	//
	//		}
	//
	//	}
	//
	//
	//	final Map<String, Range> ranges=values(stats.shape(), stats.path(), values -> values
	//
	//			.collect(groupingBy(v -> v.getType().toString(), reducing(null, v -> new Range(1, v, v), (x, y) ->
	//					x == null ? y : y == null ? x : x.merge(y)
	//			)))
	//
	//	);
	//
	//	final Entity.Builder container=Entity.newBuilder(datastore.newKeyFactory().setKind(GCP.Resource).newKey(path));
	//
	//	if ( ranges.isEmpty() ) {
	//
	//		container.set(DatastoreEngine.count, 0L);
	//
	//	} else {
	//
	//		ranges.values().stream() // global stats
	//				.reduce(Range::merge)
	//				.orElse(new Range(0, null, null)) // unexpected
	//				.set(container);
	//
	//		container.set(DatastoreEngine.stats, ranges.entrySet().stream() // type-specific stats
	//
	//				.sorted(Map.Entry
	//						.<String, Range>comparingByValue(Range::order) // decreasing count
	//						.thenComparing(comparingByKey()) // increasing datatype
	//				)
	//
	//				.map(entry -> {
	//
	//					final FullEntity.Builder<?> item=FullEntity.newBuilder(
	//							datastore.newKeyFactory().setKind(GCP.Resource).newKey(entry.getKey())
	//					);
	//
	//					entry.getValue().set(item);
	//
	//					return item.build();
	//
	//				})
	//
	//				.map(EntityValue::of)
	//				.collect(toList()));
	//
	//	}
	//
	//	return response -> response
	//			.status(OK)
	//			.set(shape(), DatastoreEngine.StatsShape)
	//			.body(entity, container.build());
	//
	//}

}
