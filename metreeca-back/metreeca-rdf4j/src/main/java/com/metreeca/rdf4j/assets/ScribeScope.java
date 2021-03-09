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

package com.metreeca.rdf4j.assets;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static java.lang.String.format;

final class ScribeScope implements BiFunction<Object, Object[], Integer> {

	private final Map<Object, Integer> ids=new IdentityHashMap<>();


	@Override public Integer apply(final Object object, final Object[] aliases) {

		if ( object == null ) {
			throw new NullPointerException("null object");
		}

		if ( aliases == null ) {
			throw new NullPointerException("null aliases");
		}

		final Integer id=ids.computeIfAbsent(object, x -> ids.size());

		for (final Object alias : aliases) {

			if ( alias == null ) {
				throw new NullPointerException("null alias");
			}

			if ( !id.equals(ids.computeIfAbsent(alias, x -> id)) ) {
				throw new IllegalStateException(format(
						"alias <%s> is already linked to a different identifier", alias
				));
			}

		}

		return id;

	}

}
