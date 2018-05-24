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
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.om.ZeroOrOne;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

import static java.lang.Character.isUpperCase;
import static java.lang.Character.toLowerCase;


public final class Downcase extends ExtensionFunctionDefinition implements Function {

	private static final String Prefix="usr";
	private static final String Space=Values.Internal;
	private static final String Name="downcase";


	//// Saxon /////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public StructuredQName getFunctionQName() {
		return new StructuredQName(Prefix, Space, Name);
	}

	@Override public SequenceType[] getArgumentTypes() {
		return new SequenceType[] {SequenceType.OPTIONAL_STRING};
	}

	@Override public SequenceType getResultType(final SequenceType[] args) {
		return SequenceType.OPTIONAL_STRING;
	}

	@Override public ExtensionFunctionCall makeCallExpression() {
		return new ExtensionFunctionCall() {
			@Override public Sequence call(final XPathContext context, final Sequence[] args) throws XPathException {

				final StringValue text=(StringValue)args[0].head();

				return text == null ? ZeroOrOne.<StringValue>empty()
						: new ZeroOrOne<>(new StringValue(downcase(text.getStringValue())));
			}
		};
	}


	//// RDF4J /////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public String getURI() {
		return Space+Name;
	}

	@Override public Value evaluate(final ValueFactory factory, final Value... args) {

		if ( args.length == 1 ) {

			return factory.createLiteral(downcase(args[0].stringValue()));

		} else {

			throw new IllegalArgumentException(String.format("usage %s(<string>)", Name));

		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static String downcase(final CharSequence value) {
		if ( value == null ) { return null; } else {

			final int n=value.length();

			final StringBuilder builder=new StringBuilder(n);

			char p;
			char c=0;

			for (int i=0; i < n; ++i) {

				p=c;
				c=value.charAt(i);

				builder.append(isUpperCase(p) ? toLowerCase(c) : c);

			}

			return builder.toString();
		}
	}

}

