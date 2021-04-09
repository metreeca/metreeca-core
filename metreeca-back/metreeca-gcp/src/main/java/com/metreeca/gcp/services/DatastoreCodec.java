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
import com.metreeca.json.shapes.Field;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.*;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.stream.Stream;

import static com.metreeca.json.Values.BNodeType;
import static com.metreeca.json.Values.IRIType;
import static com.metreeca.json.Values.ResourceType;
import static com.metreeca.json.Values.bnode;
import static com.metreeca.json.Values.format;
import static com.metreeca.json.Values.iri;
import static com.metreeca.json.Values.literal;
import static com.metreeca.json.Values.type;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.labels;
import static com.metreeca.json.shapes.Lang.langs;
import static com.metreeca.json.shapes.Lang.tagged;
import static com.metreeca.json.shapes.Localized.localized;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.*;

final class DatastoreCodec {

	private static final String Context="Context";
	private static final String Resource="Resource";

	private static final String Value="@value";
	private static final String Type="@type";
	private static final String Language="@language";

	private static final BigInteger LongMin=BigInteger.valueOf(Long.MIN_VALUE);
	private static final BigInteger LongMax=BigInteger.valueOf(Long.MAX_VALUE);

	private static final BigDecimal DoubleMin=BigDecimal.valueOf(Double.MIN_VALUE);
	private static final BigDecimal DoubleMax=BigDecimal.valueOf(Double.MAX_VALUE);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String base;

	private final Datastore datastore;
	private final PathElement context;


	DatastoreCodec(final IRI base, final Datastore datastore) {

		this.base=base.stringValue();

		this.datastore=datastore;
		this.context=PathElement.of(Context, base.stringValue());
	}


	FullEntity<?> decode(final Frame frame, final Shape shape) {
		return entity(frame, labels(shape)).get();
	}

	Frame encode(final BaseEntity<? extends IncompleteKey> entity, final Shape shape) {
		return frame(entity, shape);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Value<?> resource(final Resource resource, final Shape shape) {

		final IRI datatype=datatype(shape).orElse(null);

		return Values.resource(datatype)
				? StringValue.of(id(resource))
				: KeyValue.of(key(resource));
	}

	private Key key(final Resource resource) {
		return datastore.newKeyFactory()
				.setKind(Resource)
				.addAncestor(context)
				.newKey(id(resource));
	}

	private String id(final Resource resource) {

		final String id=resource.stringValue();

		return resource.isBNode() ? id
				: resource.isIRI() ? id.startsWith(base) ? id.substring(base.length()-1) : id
				: unsupported(resource.getClass());
	}


	private Resource resource(final IncompleteKey key) {
		return key instanceof Key ? resource((Key)key) : bnode();
	}

	private Resource resource(final Key key) {
		return resource(key.getName());
	}

	private Resource resource(final String id) {
		return id.startsWith("/") ? iri(base, id)
				: id.indexOf(':') >= 0 ? iri(id)
				: bnode(id);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Value<?> entity(final Frame frame, final Shape shape) {

		final Map<String, Field> labels=labels(shape);

		return labels.isEmpty() ? resource(frame.focus(), shape) : entity(frame, labels);
	}

	private EntityValue entity(final Frame frame, final Map<String, Field> labels) { // !!! refactor

		final Entity.Builder builder=Entity.newBuilder(key(frame.focus()));

		labels.forEach((label, field) -> {

			final Shape shape=field.shape();

			if ( localized(shape) ) {

				final Map<String, StringValue> languages=frame.values(field.iri())

						.filter(value -> RDF.LANGSTRING.equals(type(value))
								|| this.<Boolean>malformed("unexpected untagged value %s", format(value))
						)

						.map(Literal.class::cast)

						.collect(groupingBy(
								value -> value.getLanguage().orElse(""),
								mapping(literal -> StringValue.of(literal.stringValue()), reducing(null, (x, y) ->
										x == null ? y : y == null ? x : x.equals(y) ? x
												: malformed("unexpected multiple values %s / %s", x.get(), y.get())
								))
						));

				final Set<String> langs=langs(shape).orElse(emptySet());

				if ( langs.size() == 1 ) {

					if ( languages.size() == 1 ) {

						final String lang=langs.iterator().next();

						languages.forEach((language, value) -> {

							if ( language.equals(lang) ) {

								builder.set(label, value);

							} else {

								malformed("unexpected language <%s>", language);

							}

						});

					} else if ( languages.size() > 1 ) {

						malformed("unexpected multiple languages {%s}", languages);
					}

				} else {

					final FullEntity.Builder<IncompleteKey> dictionary=FullEntity.newBuilder();

					languages.forEach(dictionary::set);

					builder.set(label, dictionary.build());

				}

			} else if ( tagged(shape) ) {

				final Set<String> langs=langs(shape).orElse(emptySet());

				if ( langs.size() == 1 ) {

					final String lang=langs.iterator().next();

					builder.set(label, frame.values(field.iri())

							.filter(value -> RDF.LANGSTRING.equals(type(value))
									|| this.<Boolean>malformed("unexpected untagged value %s", format(value))
							)

							.map(Literal.class::cast)

							.filter(literal -> literal.getLanguage().orElse("").equals(lang)
									|| this.<Boolean>malformed("unexpected language %s", format(literal))
							)

							.map(literal -> StringValue.of(literal.stringValue()))

							.collect(toList())
					);

				} else {

					final FullEntity.Builder<IncompleteKey> dictionary=FullEntity.newBuilder();

					frame.values(field.iri())

							.filter(value -> RDF.LANGSTRING.equals(type(value))
									|| this.<Boolean>malformed("unexpected untagged value %s", format(value))
							)

							.map(Literal.class::cast)

							.collect(groupingBy(
									value -> value.getLanguage().orElse(""),
									mapping(literal -> StringValue.of(literal.stringValue()), toList())
							))

							.forEach(dictionary::set);

					builder.set(label, dictionary.build());

				}

			} else {

				final List<Value<?>> values=Stream.concat(

						frame.frames(field.iri()).map(f -> entity(f, shape)),
						frame.literals(field.iri()).map(v -> value(v, shape))

				).collect(toList());

				if ( values.size() == 1 ) {

					builder.set(label, values.get(0));

				} else if ( values.size() > 1 ) {

					builder.set(label, ListValue.of(values));

				}

			}

		});

		return EntityValue.of(builder.build());
	}


	private Frame frame(final BaseEntity<?> entity, final Shape shape) {

		Frame frame=Frame.frame(resource(entity.getKey()));

		for (final Map.Entry<String, Field> entry : labels(shape).entrySet()) { // !!! refactor

			final String label=entry.getKey();
			final Field field=entry.getValue();

			if ( entity.contains(label) ) {

				final Value<?> value=entity.getValue(label);

				if ( localized(field.shape()) ) {

					final Set<String> langs=langs(field.shape()).orElse(emptySet());

					if ( langs.size() == 1 ) {

						final String lang=langs.iterator().next();

						if ( value instanceof StringValue ) {

							frame=frame.value(field.iri(),
									literal(((StringValue)value).get(), lang)
							);

						} else {

							throw new IllegalStateException(format(
									"expected string value, got %s", value.getClass().getName()
							));

						}

					} else {

						if ( value instanceof EntityValue ) {

							frame=frame.values(field.iri(),
									((EntityValue)value).get().getProperties().entrySet().stream().map(e ->

											literal(((StringValue)e.getValue()).get(), e.getKey()) // !!! check casts

									).collect(toList()));

						} else {

							throw new IllegalStateException(format(
									"expected entity value, got %s", value.getClass().getName()
							));

						}

					}

				} else if ( tagged(field.shape()) ) {

					final Set<String> langs=langs(field.shape()).orElse(emptySet());

					if ( langs.size() == 1 ) {

						final String lang=langs.iterator().next();

						if ( value instanceof ListValue ) {

							frame=frame.values(field.iri(), ((ListValue)value).get().stream().map(v ->
									literal(((StringValue)v).get(), lang) // !!! check casts
							));

						} else {

							throw new IllegalStateException(format(
									"expected list value, got %s", value.getClass().getName()
							));

						}

					} else {

						if ( value instanceof EntityValue ) {

							frame=frame.values(field.iri(),
									((EntityValue)value).get().getProperties().entrySet().stream().flatMap(e ->
											((ListValue)e.getValue()).get().stream().map(v -> // !!! check casts
													literal(((StringValue)v).get(), e.getKey())
											)
									).collect(toList()));

						} else {

							throw new IllegalStateException(format(
									"expected entity value, got %s", value.getClass().getName()
							));

						}

					}

				} else {

					final Stream<? extends Value<?>> values=value instanceof ListValue
							? ((ListValue)value).get().stream()
							: Stream.of(value);

					frame=frame.objects(field.iri(), values.map(v -> entity(v)
							.map(e -> (Object)frame(e, field.shape()))
							.orElseGet(() -> value(v, field.shape()))
					));

				}

			}
		}

		return frame;
	}

	private Optional<? extends FullEntity<?>> entity(final Value<?> value) {
		return Optional.of(value)
				.filter(EntityValue.class::isInstance)
				.map(EntityValue.class::cast)
				.map(com.google.cloud.datastore.Value::get)
				.filter(e -> !e.contains(Value));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Value<?> value(final Literal value, final Shape shape) {

		final IRI datatype=value.getDatatype();

		try {

			return datatype.equals(XSD.BOOLEAN) ? value(value.booleanValue())

					: datatype.equals(XSD.INTEGER) ? value(value.integerValue())
					: datatype.equals(XSD.DECIMAL) ? value(value.decimalValue())

					: datatype.equals(XSD.STRING) ? value(value.stringValue())
					: datatype.equals(RDF.LANGSTRING) ? value(value.stringValue(), value.getLanguage().orElse(""))

					: datatype.equals(XSD.DATETIME) ? value(value.temporalAccessorValue())

					: datatype(shape).isPresent() ? value(value.stringValue())

					: entity(value.stringValue(), datatype);

		} catch ( final IllegalArgumentException e ) { // malformed literal

			return entity(value.stringValue(), datatype);

		}
	}

	private BooleanValue value(final boolean value) {
		return BooleanValue.of(value);
	}

	private Value<?> value(final BigInteger value) {
		return value.compareTo(LongMin) >= 0 && value.compareTo(LongMax) <= 0 ?
				LongValue.of(value.longValue()) : entity(value.toString(), XSD.INTEGER);
	}

	private Value<?> value(final BigDecimal value) {
		return value.compareTo(DoubleMin) >= 0 && value.compareTo(DoubleMax) <= 0 ?
				DoubleValue.of(value.doubleValue()) : entity(value.toPlainString(), XSD.DECIMAL);
	}

	private Value<?> value(final TemporalAccessor value) {
		return TimestampValue.of(Timestamp.ofTimeMicroseconds(Instant.from(value).toEpochMilli()*1000));
	}

	private StringValue value(final String value) {
		return StringValue.of(value);
	}

	private EntityValue entity(final String value, final IRI datatype) {
		return EntityValue.of(FullEntity.newBuilder()
				.set(Value, value)
				.set(Type, datatype.stringValue())
				.build()
		);
	}

	private Value<?> value(final String value, final String lang) {
		return lang.isEmpty() ? StringValue.of(value) : EntityValue.of(FullEntity.newBuilder()
				.set(Value, value)
				.set(Language, lang)
				.build()
		);
	}


	private org.eclipse.rdf4j.model.Value value(final Value<?> value, final Shape shape) {
		return value instanceof KeyValue ? value(((KeyValue)value))

				: value instanceof BooleanValue ? value(((BooleanValue)value))

				: value instanceof LongValue ? value(((LongValue)value))
				: value instanceof DoubleValue ? value(((DoubleValue)value))

				: value instanceof StringValue ? value(((StringValue)value).get(), datatype(shape).orElse(null))
				: value instanceof EntityValue ? value(((EntityValue)value))

				: value instanceof TimestampValue ? value(((TimestampValue)value))

				: unsupported(value != null ? value.getClass() : Void.class);
	}

	private org.eclipse.rdf4j.model.Value value(final String value, final IRI datatype) {
		return ResourceType.equals(datatype) ? resource(value)

				: BNodeType.equals(datatype) ? resource(value)
				: IRIType.equals(datatype) ? resource(value)

				: datatype != null ? literal(value, datatype)

				: literal(value);
	}

	private Resource value(final KeyValue value) {
		return resource(value.get().getName());
	}

	private Literal value(final BooleanValue value) {
		return literal(value.get());
	}

	private Literal value(final LongValue value) {
		return literal(value.get(), false);
	}

	private Literal value(final DoubleValue value) {
		return literal(value.get(), false);
	}

	private Literal value(final TimestampValue value) {
		return literal(OffsetDateTime.ofInstant(
				Instant.ofEpochSecond(value.get().getSeconds(), value.get().getNanos()),
				ZoneId.of("UTC")
		));
	}

	private Literal value(final EntityValue value) {

		final FullEntity<?> entity=value.get();

		if ( entity.contains(Value) && entity.contains(Type) ) {

			return literal(entity.getString(Value), iri(entity.getString(Type)));

		} else if ( entity.contains(Value) && entity.contains(Language) ) {

			return literal(entity.getString(Value), entity.getString(Language));

		} else {

			throw new UnsupportedOperationException("to be implemented"); // !!! tbi

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private <V> V malformed(final String format, final Object... args) {
		throw new IllegalArgumentException(format(format, args));
	}

	private <V> V unsupported(final Class<?> clazz) {
		throw new UnsupportedOperationException(format("unsupported type <%s>", clazz.getName()));
	}

}
