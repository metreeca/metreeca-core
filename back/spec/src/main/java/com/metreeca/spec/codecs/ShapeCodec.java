/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.spec.codecs;

import com.metreeca.jeep.rdf.Values;
import com.metreeca.spec.Shape;
import com.metreeca.spec.Shift;
import com.metreeca.spec.Spec;
import com.metreeca.spec.shapes.*;
import com.metreeca.spec.shifts.Count;
import com.metreeca.spec.shifts.Step;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.metreeca.jeep.rdf.Cell.Missing;
import static com.metreeca.jeep.rdf.Values.bnode;
import static com.metreeca.jeep.rdf.Values.integer;
import static com.metreeca.jeep.rdf.Values.statement;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


public final class ShapeCodec {

	private static final RDFFormat Format=RDFFormat.NTRIPLES;


	public Resource encode(final Shape shape, final Collection<Statement> model) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		return shape(shape, model);
	}

	public Shape decode(final Resource root, final Collection<Statement> model) {

		if ( root == null ) {
			throw new NullPointerException("null root");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		return shape(root, model);
	}

	public Shape decode(final String spec) { // !!! review/remove

		if ( spec == null ) {
			throw new NullPointerException("null spec");
		}

		try {

			final Model model=Rio.parse(new StringReader(spec), "", Format);

			final Resource root=model.subjects().stream()
					.filter(subject -> !model.contains(null, null, subject))
					.findFirst().orElseThrow(Missing);

			return decode(root, model);

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}


	//// Shapes ////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Resource shape(final Shape shape, final Collection<Statement> model) {
		return shape.accept(new Shape.Probe<Resource>() {

			@Override public Resource visit(final Datatype datatype) { return datatype(datatype, model); }

			@Override public Resource visit(final Clazz clazz) { return clazz(clazz, model); }

			@Override public Resource visit(final MinExclusive minExclusive) { return minExclusive(minExclusive, model); }

			@Override public Resource visit(final MaxExclusive maxExclusive) { return maxExclusive(maxExclusive, model); }

			@Override public Resource visit(final MinInclusive minInclusive) { return minInclusive(minInclusive, model); }

			@Override public Resource visit(final MaxInclusive maxInclusive) { return maxInclusive(maxInclusive, model); }

			@Override public Resource visit(final Pattern pattern) { return pattern(pattern, model); }

			@Override public Resource visit(final Like like) { return like(like, model); }

			@Override public Resource visit(final MinLength minLength) { return minLength(minLength, model); }

			@Override public Resource visit(final MaxLength maxLength) { return maxLength(maxLength, model); }


			@Override public Resource visit(final MinCount minCount) {
				return minCount(minCount, model);
			}

			@Override public Resource visit(final MaxCount maxCount) { return maxCount(maxCount, model); }

			@Override public Resource visit(final In in) { return in(in, model); }

			@Override public Resource visit(final All all) { return all(all, model); }

			@Override public Resource visit(final Any any) { return any(any, model); }


			@Override public Resource visit(final Trait trait) {
				return trait(trait, model);
			}

			@Override public Resource visit(final Virtual virtual) { return virtual(virtual, model); }


			@Override public Resource visit(final And and) {
				return and(and, model);
			}

			@Override public Resource visit(final Or or) { return or(or, model); }

			@Override public Resource visit(final Test test) { return test(test, model); }

			@Override public Resource visit(final When when) { return when(when, model); }


			@Override public Resource visit(final Alias alias) {
				return alias(alias, model);
			}

			@Override public Resource visit(final Label label) { return label(label, model); }

			@Override public Resource visit(final Notes notes) { return notes(notes, model); }

			@Override public Resource visit(final Placeholder placeholder) { return placeholder(placeholder, model); }

			@Override public Resource visit(final Default dflt) { return dflt(dflt, model); }

			@Override public Resource visit(final Hint hint) { return hint(hint, model); }

			@Override public Resource visit(final Group group) {
				return group(group, model);
			}


			@Override protected Resource fallback(final Shape shape) {
				throw new UnsupportedOperationException("unsupported shape ["+shape+"]");
			}

		});
	}

	private Shape shape(final Resource root, final Collection<Statement> model) {

		final Set<Value> types=types(root, model);

		return types.contains(Spec.Datatype) ? datatype(root, model)
				: types.contains(Spec.Class) ? clazz(root, model)
				: types.contains(Spec.MinExclusive) ? minExclusive(root, model)
				: types.contains(Spec.MaxExclusive) ? maxExclusive(root, model)
				: types.contains(Spec.MinInclusive) ? minInclusive(root, model)
				: types.contains(Spec.MaxInclusive) ? maxInclusive(root, model)
				: types.contains(Spec.Pattern) ? pattern(root, model)
				: types.contains(Spec.Like) ? like(root, model)
				: types.contains(Spec.MinLength) ? minLength(root, model)
				: types.contains(Spec.MaxLength) ? maxLength(root, model)

				: types.contains(Spec.MinCount) ? minCount(root, model)
				: types.contains(Spec.MaxCount) ? maxCount(root, model)
				: types.contains(Spec.In) ? in(root, model)
				: types.contains(Spec.All) ? all(root, model)
				: types.contains(Spec.Any) ? any(root, model)

				: types.contains(Spec.required) ? required(root, model)
				: types.contains(Spec.optional) ? optional(root, model)
				: types.contains(Spec.repeatable) ? repeatable(root, model)
				: types.contains(Spec.multiple) ? multiple(root, model)
				: types.contains(Spec.only) ? only(root, model)

				:  types.contains(Spec.Trait) ? trait(root, model)
				: types.contains(Spec.Virtual) ? virtual(root, model)

				: types.contains(Spec.And) ? and(root, model)
				: types.contains(Spec.Or) ? or(root, model)
				: types.contains(Spec.Test) ? test(root, model)
				: types.contains(Spec.When) ? when(root, model)

				: types.contains(Spec.Alias) ? alias(root, model)
				: types.contains(Spec.Label) ? label(root, model)
				: types.contains(Spec.Notes) ? notes(root, model)
				: types.contains(Spec.Placeholder) ? placeholder(root, model)
				: types.contains(Spec.Default) ? dflt(root, model)
				: types.contains(Spec.Hint) ? hint(root, model)
				: types.contains(Spec.Group) ? group(root, model)

				: types.contains(Spec.create) ? create(root, model)
				: types.contains(Spec.relate) ? relate(root, model)
				: types.contains(Spec.update) ? update(root, model)
				: types.contains(Spec.delete) ? delete(root, model)

				: types.contains(Spec.client) ? client(root, model)
				: types.contains(Spec.server) ? server(root, model)

				: types.contains(Spec.digest) ? digest(root, model)
				: types.contains(Spec.detail) ? detail(root, model)

				: types.contains(Spec.verify) ? verify(root, model)
				: types.contains(Spec.filter) ? filter(root, model)

				: error("unknown shape type "+types);
	}


	private Resource datatype(final Datatype datatype, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.Datatype));
		model.add(statement(node, Spec.iri, datatype.getIRI()));

		return node;
	}

	private Shape datatype(final Resource root, final Collection<Statement> model) {
		return Datatype.datatype(iri(root, Spec.iri, model));
	}


	private Resource clazz(final Clazz clazz, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.Class));
		model.add(statement(node, Spec.iri, clazz.getIRI()));

		return node;
	}

	private Shape clazz(final Resource root, final Collection<Statement> model) {
		return Clazz.clazz(iri(root, Spec.iri, model));
	}


	private Resource minExclusive(final MinExclusive minExclusive, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.MinExclusive));
		model.add(statement(node, Spec.value, minExclusive.getValue()));

		return node;
	}

	private Shape minExclusive(final Resource root, final Collection<Statement> model) {
		return MinExclusive.minExclusive(value(root, Spec.value, model));
	}


	private Resource maxExclusive(final MaxExclusive maxExclusive, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.MaxExclusive));
		model.add(statement(node, Spec.value, maxExclusive.getValue()));

		return node;
	}

	private Shape maxExclusive(final Resource root, final Collection<Statement> model) {
		return MaxExclusive.maxExclusive(value(root, Spec.value, model));
	}


	private Resource minInclusive(final MinInclusive minInclusive, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.MinInclusive));
		model.add(statement(node, Spec.value, minInclusive.getValue()));

		return node;
	}

	private Shape minInclusive(final Resource root, final Collection<Statement> model) {
		return MinInclusive.minInclusive(value(root, Spec.value, model));
	}


	private Resource maxInclusive(final MaxInclusive maxInclusive, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.MaxInclusive));
		model.add(statement(node, Spec.value, maxInclusive.getValue()));

		return node;
	}

	private Shape maxInclusive(final Resource root, final Collection<Statement> model) {
		return MaxInclusive.maxInclusive(value(root, Spec.value, model));
	}


	private Resource pattern(final Pattern pattern, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.Pattern));
		model.add(statement(node, Spec.text, Values.literal(pattern.getText())));
		model.add(statement(node, Spec.flags, Values.literal(pattern.getFlags())));

		return node;
	}

	private Shape pattern(final Resource root, final Collection<Statement> model) {

		return Pattern.pattern(
				literal(root, Spec.text, model).stringValue(),
				literal(root, Spec.flags, model).stringValue());
	}


	private Resource like(final Like like, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.Like));
		model.add(statement(node, Spec.text, Values.literal(like.getText())));

		return node;
	}

	private Shape like(final Resource root, final Collection<Statement> model) {
		return Like.like(literal(root, Spec.text, model).stringValue());
	}


	private Resource minLength(final MinLength minLength, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.MinLength));
		model.add(statement(node, Spec.limit, Values.literal(integer(minLength.getLimit()))));

		return node;
	}

	private Shape minLength(final Resource root, final Collection<Statement> model) {
		return MinLength.minLength(literal(root, Spec.limit, model).integerValue().intValue());
	}


	private Resource maxLength(final MaxLength maxLength, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.MaxLength));
		model.add(statement(node, Spec.limit, Values.literal(integer(maxLength.getLimit()))));

		return node;
	}

	private Shape maxLength(final Resource root, final Collection<Statement> model) {
		return MaxLength.maxLength(literal(root, Spec.limit, model).integerValue().intValue());
	}


	private Resource minCount(final MinCount minCount, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.MinCount));
		model.add(statement(node, Spec.limit, Values.literal(integer(minCount.getLimit()))));

		return node;
	}

	private Shape minCount(final Resource root, final Collection<Statement> model) {
		return MinCount.minCount(literal(root, Spec.limit, model).integerValue().intValue());
	}


	private Resource maxCount(final MaxCount maxCount, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.MaxCount));
		model.add(statement(node, Spec.limit, Values.literal(integer(maxCount.getLimit()))));

		return node;
	}

	private Shape maxCount(final Resource root, final Collection<Statement> model) {
		return MaxCount.maxCount(literal(root, Spec.limit, model).integerValue().intValue());
	}


	private Resource in(final In in, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.In));
		model.add(statement(node, Spec.values, values(in.getValues(), model)));

		return node;
	}

	private Shape in(final Resource root, final Collection<Statement> model) {
		return In.in(values(resource(root, Spec.values, model), model));
	}


	private Resource all(final All all, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.All));
		model.add(statement(node, Spec.values, values(all.getValues(), model)));

		return node;
	}

	private Shape all(final Resource root, final Collection<Statement> model) {
		return All.all(values(resource(root, Spec.values, model), model));
	}


	private Resource any(final Any any, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.Any));
		model.add(statement(node, Spec.values, values(any.getValues(), model)));

		return node;
	}

	private Shape any(final Resource root, final Collection<Statement> model) {
		return Any.any(values(resource(root, Spec.values, model), model));
	}


	private Shape required(final Resource root, final Collection<Statement> model) {
		return Shape.required();
	}

	private Shape optional(final Resource root, final Collection<Statement> model) {
		return Shape.optional();
	}

	private Shape repeatable(final Resource root, final Collection<Statement> model) {
		return Shape.repeatable();
	}

	private Shape multiple(final Resource root, final Collection<Statement> model) {
		return Shape.multiple();
	}

	private Shape only(final Resource root, final Collection<Statement> model) {
		return Shape.only(values(resource(root, Spec.values, model), model));
	}


	private Resource trait(final Trait trait, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.Trait));
		model.add(statement(node, Spec.step, step(trait.getStep(), model)));
		model.add(statement(node, Spec.shape, shape(trait.getShape(), model)));

		return node;
	}

	private Trait trait(final Resource root, final Collection<Statement> model) {
		return Trait.trait(
				step(resource(root, Spec.step, model), model),
				shape(resource(root, Spec.shape, model), model)
		);
	}


	private Resource virtual(final Virtual virtual, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.Virtual));
		model.add(statement(node, Spec.trait, shape(virtual.getTrait(), model)));
		model.add(statement(node, Spec.shift, shift(virtual.getShift(), model)));

		return node;
	}

	private Shape virtual(final Resource root, final Collection<Statement> model) {
		return Virtual.virtual(
				trait(resource(root, Spec.trait, model), model),
				shift(resource(root, Spec.shift, model), model)
		);
	}


	private Resource and(final And and, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.And));
		model.add(statement(node, Spec.shapes, shapes(and.getShapes(), model)));

		return node;
	}

	private Shape and(final Resource root, final Collection<Statement> model) {

		final Resource shapes=resource(root, Spec.shapes, model);

		return shapes.equals(RDF.NIL) ? And.and() : And.and(shapes(shapes, model));
	}


	private Resource or(final Or or, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.Or));
		model.add(statement(node, Spec.shapes, shapes(or.getShapes(), model)));

		final Collection<Shape> shapes=or.getShapes();

		return node;
	}

	private Shape or(final Resource root, final Collection<Statement> model) {

		final Resource shapes=resource(root, Spec.shapes, model);

		return shapes.equals(RDF.NIL) ? Or.or() : Or.or(shapes(shapes, model));
	}


	private Resource test(final Test test, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.Test));
		model.add(statement(node, Spec.test, shape(test.getTest(), model)));
		model.add(statement(node, Spec.pass, shape(test.getPass(), model)));
		model.add(statement(node, Spec.fail, shape(test.getFail(), model)));

		return node;
	}

	private Shape test(final Resource root, final Collection<Statement> model) {
		return Test.test(
				shape(resource(root, Spec.test, model), model),
				shape(resource(root, Spec.pass, model), model),
				shape(resource(root, Spec.fail, model), model)
		);
	}


	private Resource when(final When when, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.When));
		model.add(statement(node, Spec.iri, when.getIRI()));
		model.add(statement(node, Spec.values, values(when.getValues(), model)));

		return node;
	}

	private Shape when(final Resource root, final Collection<Statement> model) {
		return When.when(
				iri(root, Spec.iri, model),
				values(resource(root, Spec.values, model), model)
		);
	}


	private Resource alias(final Alias alias, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.Alias));
		model.add(statement(node, Spec.text, Values.literal(alias.getText())));

		return node;
	}

	private Shape alias(final Resource root, final Collection<Statement> model) {
		return Alias.alias(literal(root, Spec.text, model).stringValue());
	}


	private Resource label(final Label label, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.Label));
		model.add(statement(node, Spec.text, Values.literal(label.getText())));

		return node;
	}

	private Shape label(final Resource root, final Collection<Statement> model) {
		return Label.label(literal(root, Spec.text, model).stringValue());
	}


	private Resource notes(final Notes notes, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.Notes));
		model.add(statement(node, Spec.text, Values.literal(notes.getText())));

		return node;
	}

	private Shape notes(final Resource root, final Collection<Statement> model) {
		return Notes.notes(literal(root, Spec.text, model).stringValue());
	}


	private Resource placeholder(final Placeholder placeholder, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.Placeholder));
		model.add(statement(node, Spec.text, Values.literal(placeholder.getText())));

		return node;
	}

	private Shape placeholder(final Resource root, final Collection<Statement> model) {
		return Placeholder.placeholder(literal(root, Spec.text, model).stringValue());
	}


	private Resource dflt(final Default dflt, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.Default));
		model.add(statement(node, Spec.value, dflt.getValue()));

		return node;
	}

	private Shape dflt(final Resource root, final Collection<Statement> model) {
		return Default.dflt(value(root, Spec.value, model));
	}


	private Resource hint(final Hint hint, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.Hint));
		model.add(statement(node, Spec.iri, hint.getIRI()));

		return node;
	}

	private Shape hint(final Resource root, final Collection<Statement> model) {
		return Hint.hint(iri(root, Spec.iri, model));
	}


	private Resource group(final Group group, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.Group));
		model.add(statement(node, Spec.shape, shape(group.getShape(), model)));


		return node;
	}

	private Shape group(final Resource root, final Collection<Statement> model) {
		return Group.group(shape(resource(root, Spec.shape, model), model));
	}


	private Shape create(final Resource root, final Collection<Statement> model) {
		return Shape.create(shapes(resource(root, Spec.shapes, model), model));
	}

	private Shape relate(final Resource root, final Collection<Statement> model) {
		return Shape.relate(shapes(resource(root, Spec.shapes, model), model));
	}

	private Shape update(final Resource root, final Collection<Statement> model) {
		return Shape.update(shapes(resource(root, Spec.shapes, model), model));
	}

	private Shape delete(final Resource root, final Collection<Statement> model) {
		return Shape.delete(shapes(resource(root, Spec.shapes, model), model));
	}


	private Shape client(final Resource root, final Collection<Statement> model) {
		return Shape.client(shapes(resource(root, Spec.shapes, model), model));
	}

	private Shape server(final Resource root, final Collection<Statement> model) {
		return Shape.server(shapes(resource(root, Spec.shapes, model), model));
	}


	private Shape digest(final Resource root, final Collection<Statement> model) {
		return Shape.digest(shapes(resource(root, Spec.shapes, model), model));
	}

	private Shape detail(final Resource root, final Collection<Statement> model) {
		return Shape.detail(shapes(resource(root, Spec.shapes, model), model));
	}


	private Shape verify(final Resource root, final Collection<Statement> model) {
		return Shape.verify(shapes(resource(root, Spec.shapes, model), model));
	}

	private Shape filter(final Resource root, final Collection<Statement> model) {
		return Shape.filter(shapes(resource(root, Spec.shapes, model), model));
	}


	private Shape error(final String message) {
		throw new UnsupportedOperationException(message);
	}


	//// Shifts ////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Resource shift(final Shift shift, final Collection<Statement> model) {
		return shift.accept(new Shift.Probe<Resource>() {

			@Override protected Resource fallback(final Shift shift) {
				throw new UnsupportedOperationException("unsupported shift ["+shift+"]");
			}


			@Override public Resource visit(final Step step) {
				return step(step, model);
			}

			@Override public Resource visit(final Count count) { return count(count, model); }

		});
	}

	private Shift shift(final Resource root, final Collection<Statement> model) {

		final Set<Value> types=types(root, model);

		return types.contains(Spec.Step) ? step(root, model)

				: types.contains(Spec.Count) ? count(root, model)

				: shift("unknown shape type "+types);
	}


	private Resource step(final Step step, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.Step));
		model.add(statement(node, Spec.iri, step.getIRI()));
		model.add(statement(node, Spec.inverse, Values.literal(step.isInverse())));

		return node;
	}

	private Step step(final Resource root, final Collection<Statement> model) {
		return Step.step(
				iri(root, Spec.iri, model),
				literal(root, Spec.inverse, model).booleanValue()
		);
	}


	private Resource count(final Count count, final Collection<Statement> model) {

		final Resource node=bnode();

		model.add(statement(node, RDF.TYPE, Spec.Count));
		model.add(statement(node, Spec.shift, shift(count.getShift(), model)));

		return node;
	}

	private Count count(final Resource root, final Collection<Statement> model) {
		return Count.count(shift(root, model));
	}


	private Shift shift(final String message) {
		throw new UnsupportedOperationException(message);
	}


	//// Shape Lists ///////////////////////////////////////////////////////////////////////////////////////////////////

	private Value shapes(final Collection<Shape> shapes, final Collection<Statement> model) {
		return values(shapes.stream()
				.map(s -> shape(s, model))
				.collect(toList()), model);
	}

	private Collection<Shape> shapes(final Resource shapes, final Collection<Statement> model) {
		return values(shapes, model)
				.stream()
				.filter(v -> v instanceof Resource) // !!! error reporting
				.map(v -> (Resource)v)
				.map(v -> decode(v, model))
				.collect(toList());
	}


	//// Value Lists ///////////////////////////////////////////////////////////////////////////////////////////////////

	private Value values(final Collection<Value> values, final Collection<Statement> model) {
		if ( values.isEmpty() ) {

			return RDF.NIL;

		} else {

			final Resource items=bnode();

			RDFCollections.asRDF(values, items, model);

			return items;

		}
	}

	private Collection<Value> values(final Resource items, final Collection<Statement> model) {
		return RDFCollections.asValues(new LinkedHashModel(model), items, new ArrayList<>()); // !!! avoid model construction
	}


	//// Decoding //////////////////////////////////////////////////////////////////////////////////////////////////////

	private Set<Value> types(final Resource root, final Collection<Statement> model) {
		return objects(root, RDF.TYPE, model).collect(toSet());
	}


	private IRI iri(final Resource subject, final IRI predicate, final Collection<Statement> model) {
		return objects(subject, predicate, model)
				.filter(v -> v instanceof IRI)
				.map(v -> (IRI)v)
				.findFirst()
				.orElseThrow(missing(subject, predicate));
	}

	private Resource resource(final Resource subject, final IRI predicate, final Collection<Statement> model) {
		return objects(subject, predicate, model)
				.filter(v -> v instanceof Resource)
				.map(v -> (Resource)v)
				.findFirst()
				.orElseThrow(missing(subject, predicate));
	}

	private Literal literal(final Resource subject, final IRI predicate, final Collection<Statement> model) {
		return objects(subject, predicate, model)
				.filter(v -> v instanceof Literal)
				.map(v -> (Literal)v)
				.findFirst()
				.orElseThrow(missing(subject, predicate));
	}

	private Value value(final Resource subject, final IRI predicate, final Collection<Statement> model) {
		return objects(subject, predicate, model)
				.findFirst()
				.orElseThrow(missing(subject, predicate));
	}


	private Stream<Value> objects(final Resource subject, final IRI predicate, final Collection<Statement> model) {
		return model.stream()
				.filter(s -> s.getSubject().equals(subject) && s.getPredicate().equals(predicate))
				.map(Statement::getObject);
	}


	private Supplier<IllegalArgumentException> missing(final Resource subject, final IRI predicate) {
		return () -> new IllegalArgumentException(
				"missing "+Values.format(predicate)+" property ["+subject+"]");
	}

}
