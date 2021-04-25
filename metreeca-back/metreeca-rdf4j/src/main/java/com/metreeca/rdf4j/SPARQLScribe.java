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

import org.eclipse.rdf4j.model.*;

import java.util.Collection;
import java.util.stream.Stream;

import static com.metreeca.json.Values.quote;
import static com.metreeca.json.Values.traverse;
import static com.metreeca.rest.Scribe.*;

import static java.util.Arrays.asList;

/**
 * SPARQL query composer.
 */
public final class SPARQLScribe {

	public static Scribe comment(final String text) {
		return space(text("# %s", text));
	}


	public static Scribe base(final String base) {
		return space(text("base <%s>", base));
	}

	public static Scribe prefix(final Namespace namespace) {
		return prefix(namespace.getPrefix(), namespace.getName());
	}

	public static Scribe prefix(final String prefix, final String name) {
		return line(text("prefix %s: <%s>", prefix, name));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Scribe select(final Scribe... vars) {
		return select(false, vars);
	}

	public static Scribe select(final boolean distinct, final Scribe... vars) {
		return list(text("\rselect"),
				distinct ? text(" distinct") : nothing(),
				vars.length == 0 ? text(" *") : list(vars)
		);
	}


	public static Scribe construct(final Scribe... patterns) {
		return list(text("\rconstruct"), list(patterns));
	}


	public static Scribe where(final Scribe... pattern) {
		return list(text("\rwhere"), block(pattern));
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

	public static Scribe limit(final int limit) {
		return limit(limit, 0);
	}

	public static Scribe limit(final int limit, final int sampling) {
		return limit > 0 ? text(" limit %d", sampling > 0 ? Math.min(limit, sampling) : limit)
				: sampling > 0 ? text(" limit %d", sampling)
				: nothing();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Scribe union(final Scribe... patterns) {
		return union(asList(patterns));
	}

	public static Scribe union(final Collection<Scribe> patterns) {
		return list(patterns.stream().flatMap(pattern -> Stream.of(text(" union "), pattern)).skip(1));
	}

	public static Scribe optional(final Scribe... pattern) {
		return list(text("optional"), block(pattern));
	}

	public static Scribe values(final Scribe var, final Collection<Value> values) {
		return list(text("\rvalues"), var, block(list(
				values.stream().map(Values::format).map(Scribe::text).map(Scribe::line)
		)));
	}

	public static Scribe filter(final Scribe... expressions) {
		return list(text(" filter ( "), list(expressions), text(" )"));
	}

	public static Scribe bind(final String id, final Scribe expression) {
		return line(list(text(" bind"), as(id, expression)));
	}

	public static Scribe as(final String id, final Scribe expression) {
		return list(text(" ("), expression, text(" as "), var(id), text(')'));
	}

	public static Scribe var(final String id) {
		return text(" ?%s", id);
	}

	public static Scribe string(final String text) {
		return text(quote(text));
	}


	public static Scribe edge(final Scribe source, final String path, final Scribe target) {
		return edge(source, text(path), target);
	}

	public static Scribe edge(final Scribe source, final IRI path, final Scribe target) {
		return traverse(path,
				iri -> edge(source, text(iri), target),
				iri -> edge(target, text(iri), source)
		);
	}

	public static Scribe edge(final Scribe source, final Scribe path, final Scribe target) {
		return list(text(' '), source, text(' '), path, text(' '), target, text(" ."));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Scribe min(final Scribe expression) {
		return list(text(" min("), expression, text(")"));
	}

	public static Scribe max(final Scribe expression) {
		return list(text(" max("), expression, text(")"));
	}

	public static Scribe count(final Scribe expression) {
		return count(false, expression);
	}

	public static Scribe count(final boolean distinct, final Scribe expression) {
		return list(text(" count("), distinct ? text("distinct ") : nothing(), expression, text(")"));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Scribe is(final Scribe test, final Scribe pass, final Scribe fail) {
		return list(text(" if("), test, text(", "), pass, text(", "), fail, text(")"));
	}

	public static Scribe isBlank(final Scribe expression) {
		return function("isBlank", expression);
	}

	public static Scribe isIRI(final Scribe expression) {
		return function("isIRI", expression);
	}

	public static Scribe isLiteral(final Scribe expression) {
		return function("isLiteral", expression);
	}

	public static Scribe bound(final Scribe expression) {
		return function("bound", expression);
	}

	public static Scribe lang(final Scribe expression) {
		return function("lang", expression);
	}

	public static Scribe datatype(final Scribe expression) {
		return function("datatype", expression);
	}

	public static Scribe str(final Scribe expression) {
		return function("str", expression);
	}

	public static Scribe strlen(final Scribe expression) {
		return function("strlen", expression);
	}

	public static Scribe strstarts(final Scribe expression, final Scribe prefix) {
		return function("strstarts", expression, prefix);
	}

	public static Scribe regex(final Scribe expression, final Scribe pattern) {
		return function("regex", expression, pattern);
	}

	public static Scribe regex(final Scribe expression, final Scribe pattern, final Scribe flags) {
		return function("regex", expression, pattern, flags);
	}


	public static Scribe or(final Scribe... expressions) {
		return list(" || ", expressions);
	}

	public static Scribe and(final Scribe... expressions) {
		return list(" && ", expressions);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Scribe eq(final Scribe x, final Scribe y) {
		return op(x, "=", y);
	}

	public static Scribe neq(final Scribe x, final Scribe y) {
		return op(x, "!=", y);
	}

	public static Scribe lt(final Scribe x, final Scribe y) {
		return op(x, "<", y);
	}

	public static Scribe gt(final Scribe x, final Scribe y) {
		return op(x, ">", y);
	}

	public static Scribe lte(final Scribe x, final Scribe y) {
		return op(x, "<=", y);
	}

	public static Scribe gte(final Scribe x, final Scribe y) {
		return op(x, ">=", y);
	}

	public static Scribe in(final Scribe expression, final Stream<Scribe> expressions) {
		return list(expression, text(" in ("), list(", ", expressions), text(')'));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static Scribe function(final String name, final Scribe... args) {
		return list(text(' '), text(name), text('('), list(", ", args), text(')'));
	}

	private static Scribe op(final Scribe x, final String name, final Scribe y) {
		return list(x, text(' '), text(name), text(' '), y);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private SPARQLScribe() {}

}
