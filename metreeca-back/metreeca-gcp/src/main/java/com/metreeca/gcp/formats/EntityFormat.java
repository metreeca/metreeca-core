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

package com.metreeca.gcp.formats;

import com.metreeca.gcp.GCP;
import com.metreeca.gcp.services.Datastore;
import com.metreeca.rest.*;
import com.metreeca.tree.Shape;
import com.metreeca.tree.probes.Optimizer;
import com.metreeca.tree.probes.Redactor;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.JsonValue;

import static com.metreeca.gcp.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.formats.JSONFormat.json;


public final class EntityFormat implements Format<Entity> {

	private static final Pattern StepPattern=Pattern.compile("(?:^|[./])([:\\w]+)");


	/**
	 * Creates an entity body format for the shared datastore.
	 *
	 * <p><strong>Warning</strong> / Must be invoked only by tasks {@linkplain Context#exec(Runnable...) running inside}
	 * a service context</p>
	 *
	 * @return a new entity body format instance for the {@linkplain Datastore#datastore() shared datastore}
	 */
	public static EntityFormat entity() {
		return entity(service(datastore()));
	}

	/**
	 * Creates an entity body format for a target datastore.
	 *
	 * @param datastore the datastore where entities are expected to be stored
	 *
	 * @return a new entity body format instance for the target {@code datastore}
	 *
	 * @throws NullPointerException if {@code datastore} is nulll
	 */
	public static EntityFormat entity(final Datastore datastore) {

		if ( datastore == null ) {
			throw new NullPointerException("null datastore");
		}

		return new EntityFormat(datastore);
	}


	//// Casts /////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Value<?> value(final Object object) {
		return object == null ? NullValue.of()
				: object instanceof Value ? (Value<?>)object
				: object instanceof String ? StringValue.of((String)object)
				: object instanceof FullEntity ? EntityValue.of((FullEntity<?>)object)
				: object instanceof Key ? KeyValue.of((Key)object)
				: object instanceof Double ? DoubleValue.of((Double)object)
				: object instanceof Float ? DoubleValue.of((Float)object)
				: object instanceof Number ? LongValue.of(((Number)object).longValue())
				: object instanceof Boolean ? BooleanValue.of((Boolean)object)
				: object instanceof Timestamp ? TimestampValue.of((Timestamp)object)
				: object instanceof Date ? TimestampValue.of(Timestamp.ofTimeMicroseconds(((Date)object).getTime()*1000))
				: object instanceof Blob ? BlobValue.of((Blob)object)
				: object instanceof byte[] ? BlobValue.of(Blob.copyFrom((byte[])object))
				: object instanceof LatLng ? LatLngValue.of((LatLng)object)
				: unsupported("unsupported object type {"+object.getClass().getName()+"}");
	}


	private static <T> T unsupported(final String message) {
		throw new UnsupportedOperationException(message);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Datastore datastore;

	private final EntityDecoder decoder;
	private final EntityEncoder encoder;


	private EntityFormat(final Datastore datastore) {

		this.datastore=datastore;

		this.decoder=new EntityDecoder(this.datastore);
		this.encoder=new EntityEncoder();
	}


	@Override public List<String> path(final String base, final Shape shape, final String path) {

		final List<String> steps=new ArrayList<>();
		final Matcher matcher=StepPattern.matcher(path);

		int last=0;

		while ( matcher.lookingAt() ) {

			steps.add(matcher.group(1));
			matcher.region(last=matcher.end(), path.length());

		}

		if ( last != path.length() ) {
			throw new IllegalArgumentException("malformed path ["+path+"]");
		}

		return steps;
	}

	@Override public Object value(final String base, final Shape shape, final JsonValue value) {
		return decoder.decode(value, shape);
	}


	@Override public Result<Entity, Failure> get(final Message<?> message) {
		return message.body(json()).value(json -> {

			final FullEntity<?> entity=decoder.decode(json, driver(message.shape()));

			return entity instanceof Entity ? (Entity)entity : Entity.newBuilder(

					datastore.newKeyFactory()
							.setKind(Optional.ofNullable(entity.getKey()).map(BaseKey::getKind).orElse(GCP.Resource))
							.newKey(message.request().path()),

					entity

			).build();

		});
	}

	@Override public <M extends Message<M>> M set(final M message, final Entity value) {
		return message.body(json(),
				encoder.encode(value, driver(message.shape()))
		);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof EntityFormat
				&& datastore.equals(((EntityFormat)object).datastore);
	}

	@Override public int hashCode() {
		return datastore.hashCode();
	}


	private Shape driver(final Shape shape) { // !!! caching
		return shape

				.map(new Redactor(Shape.Role, values -> true))
				.map(new Redactor(Shape.Task, values -> true))
				.map(new Redactor(Shape.Area, values -> true))
				.map(new Redactor(Shape.Mode, Shape.Convey)) // remove internal filtering shapes

				.map(new EntityInferencer()) // infer implicit constraints to drive json shorthands
				.map(new Optimizer());

	}

}