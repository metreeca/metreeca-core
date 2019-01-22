/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.form.engines;

import com.metreeca.form.*;
import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.probes.Pruner;
import com.metreeca.form.probes.Traverser;
import com.metreeca.form.shapes.*;
import com.metreeca.form.shifts.Count;
import com.metreeca.form.shifts.Step;
import com.metreeca.form.shifts.Table;
import com.metreeca.form.things.Values;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import java.util.*;
import java.util.stream.Stream;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.Values.bnode;
import static com.metreeca.form.things.Values.statement;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;


abstract class SPARQL { // ! refactor

	private final Map<Object, Object> ids=new IdentityHashMap<>(); // map objects to unique identifiers


	public abstract Object code();

	public String compile() { return new SPARQLBuilder().text(code()).text(); }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected Object id() {
		return id(new Object());
	}

	protected Object id(final Object object) {

		Object id=ids.get(object);

		if ( id == null ) {
			ids.put(object, id=ids.size());
		}

		return id;
	}


	protected Object link(final Object object, final Object alias) {

		final Object target=id(alias);
		final Object current=ids.put(object, target);

		if ( current != null && !current.equals(target) ) {
			throw new IllegalStateException("object is already linked to a different identifier");
		}

		return target;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected List<String> prefixes() {
		return list(
				"prefix rdfs: <", RDFS.NAMESPACE, ">\n",
				"\n");
	}


	protected Object projection(final Collection<String> variables) {
		return items(variables.stream().map(this::var), " ");
	}

	protected List<String> template(final Shape shape, final Collection<Statement> template) {
		return shape.map(new TemplateProbe(shape))
				.collect(toCollection(() -> template))
				.stream()
				.flatMap(statement -> Stream.of(statement.getSubject(), statement.getObject()))
				.filter(value -> value instanceof BNode)
				.map(value -> ((BNode)value).getID())
				.distinct()
				.sorted()
				.collect(toList());
	}

	protected Object filter(final Shape shape, final Collection<Order> orders, final int offset, final int limit) {

		final Object root=term(shape);

		return shape.equals(and()) ? list() : shape.equals(or()) ? "filter (false)" : list(

				" { select distinct ", root, " {\f",

				roots(shape), "\f",
				filters(shape), "\f",
				offset > 0 || limit > 0 ? sorters(root, orders) : null, "\f",

				"\f}",

				offset > 0 || limit > 0 ? list(" order by ", criteria(root, orders)) : null,
				offset > 0 ? list(" offset ", offset) : null,
				limit > 0 ? list(" limit ", limit) : null,

				" }"
		);
	}

	protected Object pattern(final Shape shape) {
		return shape.map(new PatternProbe(shape));
	}


	protected Object roots(final Shape shape) { // root universal constraints
		return All.all(shape)
				.map(values -> list("\fvalues ", term(shape), " {\n",
						items(values.stream().map(this::term).collect(toList()), "\n"),
						"\n}\f"))
				.orElse(null);
	}

	protected Object filters(final Shape shape) {
		return shape.map(new FilterProbe(shape));
	}

	protected Object sorters(final Object root, final Collection<Order> orders) {
		return orders.stream()
				.filter(order -> !order.getPath().isEmpty()) // root already included
				.map(order -> list(" optional { ", root, path(order.getPath()), var(id(order)), " }\n"));
	}

	protected Object criteria(final Object root, final Collection<Order> orders) {
		return Stream.concat(

				orders.stream().map(order -> list(order.isInverse() ? "desc" : "asc",
						"(", order.getPath().isEmpty() ? root : var(id(order)), ") ")),

				Stream.of(root) // last resort

		);
	}


	private Object edge(final Object source, final Step step, final Object target) {

		final Object link=term(step.getIRI());

		return step.isInverse()
				? list(target, link, source, " .\n")
				: list(source, link, target, " .\n");
	}


	protected Object path(final Collection<Step> path) {
		return items(path.stream().map(this::step).collect(toList()), '/');
	}

	private Object step(final Step step) {
		return step.isInverse() ? list("^", term(step.getIRI())) : term(step.getIRI());
	}


	protected Object term(final Shape shape) {
		return var(id(shape));
	}

	protected Object term(final Value value) {
		return list(" ", Values.format(value));
	}


	protected Object var(final Object id) {
		return list(" ?", id);
	}


	protected Object items(final Stream<?> items, final Object separator) {
		return items(items.collect(toList()), separator);
	}

	protected Object items(final Iterable<?> items, final Object separator) {

		final Collection<Object> list=new ArrayList<>();

		for (final Object item : items) {

			if ( !list.isEmpty() ) { list.add(separator); }

			list.add(item);
		}

		return list;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private boolean isAggregate(final Shift shift) {
		return shift.map(new Shift.Probe<Boolean>() {

			@Override public Boolean probe(final Step step) { return false; }

			@Override public Boolean probe(final Count count) { return true; }

			@Override public Boolean probe(final Table table) { return false; }

		});
	}


	private Object projection(final Shift source, final Object target) {
		return list(" (", source.map(new Shift.Probe<Object>() {

			@Override public Object probe(final Step step) {
				return var(id(source));
			}

			@Override public Object probe(final Table table) {
				return var(id(source));
			}

			@Override public Object probe(final Count count) {
				return list(" count(distinct ", var(id(source)), ")");
			}

		}), " as", target, ")");
	}

	private Object grouping(final Map.Entry<Trait, Shift> field) {
		return var(id(field.getValue()));
	}

	private Object shift(final Shift shift, final boolean required, final Object source, final Object target) {
		return shift.map(new Shift.Probe<Object>() {

			@Override public Object probe(final Step step) {
				return required ? edge(source, step, target)
						: list("\foptional {\f", edge(source, step, target), "\f}\f");
			}

			@Override public Object probe(final Table table) {
				return null;
			}

			@Override public Object probe(final Count count) {
				return count.getShift().map(this);
			}

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final class TemplateProbe extends Traverser<Stream<Statement>> {

		private final Shape focus;


		private TemplateProbe(final Shape focus) {
			this.focus=focus;
		}


		@Override public Stream<Statement> probe(final Shape shape) { return Stream.empty(); }


		@Override public Stream<Statement> probe(final Trait trait) {

			final Step step=trait.getStep();
			final Shape shape=trait.getShape();

			final IRI iri=step.getIRI();

			final BNode source=bnode(id(focus).toString());
			final BNode target=bnode(id(shape).toString());

			return Stream.concat(
					Stream.of(step.isInverse() ? statement(target, iri, source) : statement(source, iri, target)),
					shape.map(new TemplateProbe(shape))
			);

		}

		@Override public Stream<Statement> probe(final Virtual virtual) {
			return virtual.getTrait().map(this); // !!! visit shift to generate additional specific edges
		}


		@Override public Stream<Statement> probe(final And and) {
			return and.getShapes().stream().flatMap(shape -> shape.map(this));
		}

		@Override public Stream<Statement> probe(final Or or) {
			return or.getShapes().stream().flatMap(shape -> shape.map(this));
		}

		@Override public Stream<Statement> probe(final Option option) {
			return Stream.concat(
					option.getPass().map(this),
					option.getFail().map(this)
			);
		}


	}

	private final class FilterProbe implements Shape.Probe<Object> {

		private final Shape source;


		private FilterProbe(final Shape source) {
			this.source=source;
		}


		@Override public Object probe(final Meta meta) {
			return null;
		}

		@Override public Object probe(final When when) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}


		@Override public Object probe(final Datatype datatype) {
			return null;
		}

		@Override public Object probe(final Clazz clazz) {
			return list(term(source), " a/rdfs:subClassOf*", term(clazz.getIRI()), " .\n");
		}

		@Override public Object probe(final MinExclusive minExclusive) {
			return list("\ffilter ( ", term(source), " > ", term(minExclusive.getValue()), " )\f");
		}

		@Override public Object probe(final MaxExclusive maxExclusive) {
			return list("\ffilter ( ", term(source), " < ", term(maxExclusive.getValue()), " )\f");
		}

		@Override public Object probe(final MinInclusive minInclusive) {
			return list("\ffilter ( ", term(source), " >= ", term(minInclusive.getValue()), " )\f");
		}

		@Override public Object probe(final MaxInclusive maxInclusive) {
			return list("\ffilter ( ", term(source), " <= ", term(maxInclusive.getValue()), " )\f");
		}

		@Override public Object probe(final Pattern pattern) {
			return list("\ffilter regex(", term(source), ", '",
					pattern.getText().replace("\\", "\\\\"), "', '", pattern.getFlags(), "')\f");
		}

		@Override public Object probe(final Like like) {
			return list("\ffilter regex(str(", term(source), "), '", like.toExpression().replace("\\", "\\\\"), "')\f");
		}

		@Override public Object probe(final MinLength minLength) {
			return list("\ffilter (strlen(str(", term(source), ")) >= ", minLength.getLimit(), ")\f");
		}

		@Override public Object probe(final MaxLength maxLength) {
			return list("\ffilter (strlen(str(", term(source), ")) <= ", maxLength.getLimit(), ")\f");
		}

		@Override public Object probe(final Custom custom) {
			return null;
		}

		@Override public Object probe(final MinCount minCount) {
			return null;
		}

		@Override public Object probe(final MaxCount maxCount) {
			return null;
		}


		@Override public Object probe(final In in) {
			return null;
		}

		@Override public Object probe(final All all) {
			return list(); // universal constraints handled by trait visitor
		}

		@Override public Object probe(final Any any) {

			// values-based filtering (as opposed to in-based filtering) works also or root terms // !!! performance?

			return list("\fvalues ", term(source), " {\n",
					items(any.getValues().stream().map(SPARQL.this::term).collect(toList()), "\n"),
					"\n}\f");
		}



		@Override public Object probe(final Trait trait) {

			final Step step=trait.getStep();
			final Shape shape=trait.getShape();

			return list(

					shape instanceof All // filtering hook
							? null // ($) only if actually referenced by filters
							: edge(term(source), step, term(shape)),

					All.all(shape) // target universal constraints
							.map(values -> values.stream().map(value -> edge(term(source), step, term(value))))
							.orElse(null),

					filters(shape)
			);
		}

		@Override public Object probe(final Virtual virtual) {

			final Shift shift=virtual.getShift();
			final Shape shape=virtual.getTrait().getShape();

			return list(shift(shift, true, term(source), term(shape)), shape.map(new FilterProbe(shape)));
		}


		@Override public Object probe(final And and) {
			return and.getShapes().stream().map(shape -> shape.map(this));
		}

		@Override public Object probe(final Or or) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public Object probe(final Option option) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

	}

	private final class PatternProbe extends Traverser<Object> {

		// !!! (€) remove optionals if term is required or if exists a filter on the same path

		private final Shape shape;


		private PatternProbe(final Shape shape) {
			this.shape=shape;
		}


		@Override public Object probe(final When when) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}


		@Override public Object probe(final Trait trait) {

			final Step step=trait.getStep();
			final Shape shape=trait.getShape();

			final Object pattern=list(
					edge(term(this.shape), step, term(shape)),
					pattern(shape)
			);

			// !!! (€) optional unless universal constraints are present

			return list("\f", All.all(shape).isPresent() ? pattern : list("\foptional {\f", pattern, "\f}"), "\f");
		}

		@Override public Object probe(final Virtual virtual) {

			final Shift shift=virtual.getShift();
			final Shape shape=virtual.getTrait().getShape();

			if ( isAggregate(shift) ) {

				final boolean singleton=false; // !!!

				final Shape anchor=this.shape // shape is already redacted for filtering mode
						.map(new Pruner())
						.map(new Optimizer());

				link(anchor, this.shape);

				return list(

						"\foptional { select ",

						singleton ? "" : term(this.shape),
						projection(shift, term(shape)),

						" {\f",

						roots(anchor),
						filters(anchor),

						shift(shift, false, term(this.shape), var(id(shift))),

						"\f}",

						singleton ? "" : list(" group by ", term(this.shape)),

						" }\f"

				);

			} else {

				return shift(shift, false, term(this.shape), term(shape));

			}
		}

		//@Override public Object probe(final Table table) {
		//
		//	final Map<Boolean, List<Map.Entry<Trait, Shift>>> roles=table.getFields().entrySet().stream()
		//			.collect(partitioningBy(field -> isAggregate((Object)field)));
		//
		//	final Collection<Map.Entry<Trait, Shift>> aggregates=roles.get(true);
		//	final Collection<Map.Entry<Trait, Shift>> derivates=roles.get(false);
		//
		//	if ( aggregates.isEmpty() ) {
		//
		//		return derivates.stream().map(field -> shift(link(field.getValue(), field.getKey().getShape())));
		//
		//	} else {
		//
		//		return list(
		//
		//				"\f{ select ", var(id(shape)), aggregates.stream().map(this::projection), " {\f",
		//
		//				// !!! insert recoverable constraints from outer shape
		//
		//				aggregates.stream().map(field -> shift(field.getValue())),
		//				derivates.stream().map(field -> shift(field.getValue())),
		//
		//				"\f} group by ", var(id(shape)), derivates.stream().map(this::grouping), " }\f"
		//
		//		);
		//
		//	}
		//}


		@Override public Object probe(final And and) {
			return and.getShapes().stream().map(shape -> shape.map(this));
		}

		@Override public Object probe(final Or or) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public Object probe(final Option option) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

	}

}
