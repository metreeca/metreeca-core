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

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import java.io.Serializable;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.*;

final class ValueComparator implements Comparator<Value>, Serializable {

	private static final long serialVersionUID=1888795007683597342L;


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public int compare(final Value x, final Value y) {
		return Objects.equals(x, y) ? 0

				: x == null ? -1
				: y == null ? 1

				: x.isBNode() ? y.isBNode() ? compare((BNode)x, (BNode)y) : -1
				: y.isBNode() ? 1

				: x.isIRI() ? y.isIRI() ? compare((IRI)x, (IRI)y) : -1
				: y.isIRI() ? 1

				: x.isLiteral() ? y.isLiteral() ? compare((Literal)x, (Literal)y) : -1
				: y.isLiteral() ? 1

				: x.isTriple() ? y.isTriple() ? compare((Triple)x, (Triple)y) : -1
				: y.isTriple() ? 1

				: x.stringValue().compareTo(y.stringValue());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private int compare(final BNode x, final BNode y) {
		return x.getID().compareTo(y.getID());
	}

	private int compare(final IRI x, final IRI y) {
		return x.stringValue().compareTo(y.stringValue());
	}

	private int compare(final Literal x, final Literal y) {
		return isBoolean(x) ? isBoolean(y) ? bool(x, y) : -1
				: isBoolean(y) ? 1

				: isNumeric(x) ? isNumeric(y) ? numeric(x, y) : -1
				: isNumeric(y) ? 1

				: isTemporal(x) ? isTemporal(y) ? temporal(x, y) : -1
				: isTemporal(y) ? 1

				: isDuration(x) ? isDuration(y) ? duration(x, y) : -1
				: isDuration(y) ? 1

				: isPlain(x) ? isPlain(y) ? plain(x, y) : -1
				: isPlain(y) ? 1

				: isTagged(x) ? isTagged(y) ? tagged(x, y) : -1
				: isTagged(y) ? 1

				: typed(x, y);
	}

	private int compare(final Triple x, final Triple y) {
		return chain(compare(x.getSubject(), y.getSubject()), () -> chain(
				compare(x.getPredicate(), y.getPredicate()), () ->
						compare(x.getObject(), y.getObject())
				)
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private boolean isBoolean(final Literal literal) {
		return XSD.BOOLEAN.equals(literal.getDatatype());
	}

	private boolean isNumeric(final Literal literal) {
		return XSD.Datatype.from(literal.getDatatype()).map(XSD.Datatype::isNumericDatatype).orElse(false);
	}

	private boolean isTemporal(final Literal literal) {
		return XSD.Datatype.from(literal.getDatatype()).map(XSD.Datatype::isCalendarDatatype).orElse(false);
	}

	private boolean isDuration(final Literal literal) {
		return XSD.Datatype.from(literal.getDatatype()).map(XSD.Datatype::isDurationDatatype).orElse(false);
	}

	private boolean isPlain(final Literal literal) {
		return XSD.STRING.equals(literal.getDatatype());
	}

	private boolean isTagged(final Literal literal) {
		return RDF.LANGSTRING.equals(literal.getDatatype());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private int bool(final Literal x, final Literal y) {
		return comparable(x, y, Literal::booleanValue);
	}

	private int numeric(final Literal x, final Literal y) {

		final IRI xt=x.getDatatype();
		final IRI yt=y.getDatatype();

		return XSD.DOUBLE.equals(xt) || XSD.DOUBLE.equals(yt) ? comparable(x, y, Literal::doubleValue)
				: XSD.FLOAT.equals(xt) || XSD.FLOAT.equals(yt) ? comparable(x, y, Literal::floatValue)
				: XSD.DECIMAL.equals(xt) || XSD.DECIMAL.equals(yt) ? comparable(x, y, Literal::decimalValue)
				: XSD.INTEGER.equals(xt) || XSD.INTEGER.equals(yt) ? comparable(x, y, Literal::integerValue)
				: comparable(x, y, Literal::longValue);
	}

	private int temporal(final Literal x, final Literal y) {

		final IRI xt=x.getDatatype();
		final IRI yt=y.getDatatype();

		return chain(compare(xt, yt), () -> comparable(x, y, v -> {

			final TemporalAccessor accessor=v.temporalAccessorValue();

			return OffsetDateTime.from(new TemporalAccessor() {

				@Override public boolean isSupported(final TemporalField field) {
					return true;
				}

				@Override public long getLong(final TemporalField field) {
					return accessor.isSupported(field) ? accessor.getLong(field) : field.range().getMinimum();
				}

			});

		}));
	}

	private int duration(final Literal x, final Literal y) {
		return comparable(x, y, v -> Duration.from(v.temporalAmountValue()));
	}

	private int plain(final Literal x, final Literal y) {
		return x.getLabel().compareTo(y.getLabel());
	}

	private int tagged(final Literal x, final Literal y) {
		return chain(x.getLanguage().orElse("").compareTo(y.getLanguage().orElse("")), () -> plain(x, y));
	}

	private int typed(final Literal x, final Literal y) {
		return chain(compare(x.getDatatype(), y.getDatatype()), () -> plain(x, y));
	}

	private <T extends Comparable<T>> int comparable(
			final Literal x, final Literal y, final Function<Literal, T> mapper
	) {

		final T xv=guard(() -> mapper.apply(x));
		final T yv=guard(() -> mapper.apply(y));

		return xv == null && yv == null ? typed(x, y)
				: xv == null ? 1
				: yv == null ? -1
				: xv.compareTo(yv);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private int chain(final int order, final IntSupplier next) {
		return order != 0 ? order : next.getAsInt();
	}

	private <T> T guard(final Supplier<T> supplier) {
		try {

			return supplier.get();

		} catch ( final RuntimeException e ) {

			return null;

		}
	}

}
