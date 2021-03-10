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

package com.metreeca.rdf4j;

import com.metreeca.json.Values;
import com.metreeca.rest.Scribe;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Value;

import java.util.Collection;
import java.util.stream.Stream;

import static com.metreeca.rest.Scribe.*;

import static java.util.Arrays.stream;

/**
 * SPARQL query composer.
 */
public final class SPARQLScribe {

	public static Scribe comment(final String text) {
		return form(text("# %s", text));
	}


	public static Scribe base(final String base) {
		return form(text("base <%s>", base));
	}

	public static Scribe prefix(final Namespace namespace) {
		return prefix(namespace.getPrefix(), namespace.getName());
	}

	public static Scribe prefix(final String prefix, final String name) {
		return line(text("prefix %s: <%s>", prefix, name));
	}


	public static Scribe select(final Scribe... vars) {
		return select(false, vars);
	}

	public static Scribe select(final boolean distinct, final Scribe... vars) {
		return list(text(" select ", distinct ? text(" distinct ") : nothing()), list(vars));
	}

	public static Scribe where(final Scribe... pattern) {
		return list(text(" where"), block(pattern));
	}


	public static Scribe union(final Scribe... patterns) {
		return list(stream(patterns).flatMap(pattern -> Stream.of(text(" union "), pattern)).skip(1));
	}

	public static Scribe optional(final Scribe... pattern) {
		return list(text(" optional"), block(pattern));
	}

	public static Scribe values(final String anchor, final Collection<Value> values) {
		return form(list(text(" values"), var(anchor), block(list(
				values.stream().map(Values::format).map(Scribe::text).map(Scribe::line)
		))));
	}

	public static Scribe bind(final String id, final Scribe expression) {
		return line(list(text("bind"), as(id, expression)));
	}


	public static Scribe edge(final Scribe subject, final String predicate, final Scribe object) {
		return list(subject, text(" "), text(predicate), text(" "), object);
	}

	public static Scribe filter(final Scribe... expressions) {
		return list(text(" filter ( "), list(expressions), text(" )"));
	}

	public static Scribe var(final String id) {
		return text(" ?%s", id);
	}

	public static Scribe as(final String id, final Scribe expression) {
		return list(text("("), expression, text(" as "), var(id), text(")"));
	}


	public static Scribe group(final Scribe... expressions) {
		return list(text(" group by"), list(expressions));
	}

	public static Scribe having(final Scribe... expressions) {
		return list(text(" having ( "), list(expressions), text(" )"));
	}

	public static Scribe order(final Scribe... expressions) {
		return list(text(" order by"), list(expressions));
	}

	public static Scribe sort(final boolean inverse, final Scribe expression) {
		return inverse ? desc(expression) : asc(expression);
	}

	public static Scribe asc(final Scribe expression) {
		return list(text(" asc("), expression, text(")"));
	}

	public static Scribe desc(final Scribe expression) {
		return list(text(" desc("), expression, text(")"));
	}

	public static Scribe offset(final int offset) {
		return offset > 0 ? text(" offset %d", offset) : nothing();
	}

	public static Scribe limit(final int limit, final int sampling) {
		return limit == 0 && sampling == 0 ? nothing()
				: text(" limit %d", limit > 0 ? Math.min(limit, sampling) : sampling);
	}


	public static Scribe min(final Scribe expression) {
		return list(text("min("), expression, text(")"));
	}

	public static Scribe max(final Scribe expression) {
		return list(text("max("), expression, text(")"));
	}

	public static Scribe count(final Scribe expression) {
		return count(false, expression);
	}

	public static Scribe count(final boolean distinct, final Scribe expression) {
		return list(text("count(", distinct ? text("distinct") : nothing()), expression, text(")"));
	}


	public static Scribe is(final Scribe test, final Scribe pass, final Scribe fail) {
		return list(text(" if("), test, text(", "), pass, text(", "), fail, text(")"));
	}

	public static Scribe isBlank(final Scribe expression) {
		return list(text(" isBlank("), expression, text(")"));
	}

	public static Scribe isIRI(final Scribe expression) {
		return list(text(" isIRI("), expression, text(")"));
	}

	public static Scribe datatype(final Scribe expression) {
		return list(text(" datatype("), expression, text(")"));
	}


	public static Scribe gt(final Scribe x, final Scribe y) {
		return list(x, text(" > "), y);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private SPARQLScribe() {}

}
