/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

package com.metreeca.mill.fn;

import com.metreeca.spec.things.Values;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.*;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;


public final class Format extends ExtensionFunctionDefinition implements Function {

	private static final String Prefix="usr";
	private static final String Space=Values.User;
	private static final String Name="format";


	//// Saxon /////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public StructuredQName getFunctionQName() {
		return new StructuredQName(Prefix, Space, Name);
	}

	@Override public int getMaximumNumberOfArguments() {
		return Integer.MAX_VALUE;
	}

	@Override public SequenceType[] getArgumentTypes() {
		return new SequenceType[] {SequenceType.SINGLE_STRING, SequenceType.OPTIONAL_ATOMIC};
	}

	@Override public SequenceType getResultType(final SequenceType[] args) {
		return SequenceType.SINGLE_STRING;
	}


	@Override public ExtensionFunctionCall makeCallExpression() {
		return new ExtensionFunctionCall() {
			@Override public Sequence call(final XPathContext context, final Sequence[] args) throws XPathException {

				final String format=args[0].head().getStringValue();
				final Object[] objects=new Object[args.length-1];

				for (int i=1; i < args.length; ++i) {

					final Item item=args[i].head();

					objects[i-1]=item == null ? null
							: item instanceof BooleanValue ? ((BooleanValue)item).getBooleanValue()
							: item instanceof DecimalValue ? ((DecimalValue)item).getDecimalValue()
							: item instanceof BigIntegerValue ? ((BigIntegerValue)item).asBigInteger()
							: item instanceof DoubleValue ? ((DoubleValue)item).getDoubleValue()
							: item instanceof FloatValue ? ((FloatValue)item).getFloatValue()
							: item instanceof IntegerValue ? ((IntegerValue)item).longValue()
							: item.getStringValue();

				}

				return new StringValue(format(format, objects));
			}
		};
	}


	//// RDF4J /////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public String getURI() {
		return Space+Name;
	}

	@Override public Value evaluate(final ValueFactory factory, final Value... args) {

		if ( args.length >= 1 ) {

			final String format=args[0].stringValue();
			final Object[] objects=new Object[args.length-1];

			for (int i=1; i < args.length; ++i) {

				final Value value=args[i];

				objects[i-1]=value == null ? null
						: value instanceof Literal ? object((Literal)value)
						: object(value);

			}

			return factory.createLiteral(format(format, objects));

		} else {

			throw new IllegalArgumentException(String.format("usage %s(<format>, <arg>*)", Name));

		}

	}

	private String object(final Value value) {
		return value.stringValue();
	}

	private Object object(final Literal value) {

		final IRI type=value.getDatatype();

		return type.equals(XMLSchema.BOOLEAN) ? value.booleanValue()

				: type.equals(XMLSchema.DECIMAL) ? value.decimalValue()
				: type.equals(XMLSchema.INTEGER) ? value.integerValue()

				: type.equals(XMLSchema.LONG) ? value.longValue()
				: type.equals(XMLSchema.INT) ? value.intValue()
				: type.equals(XMLSchema.SHORT) ? value.shortValue()
				: type.equals(XMLSchema.BYTE) ? value.byteValue()

				: type.equals(XMLSchema.BOOLEAN) ? value.booleanValue()

				: value.stringValue();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static String format(final String format, final Object... args) {

		if ( format == null ) {
			throw new NullPointerException("null format");
		}

		return String.format(format, args);
	}
}

