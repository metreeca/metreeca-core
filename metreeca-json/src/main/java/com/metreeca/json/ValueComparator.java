
/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

package com.metreeca.json;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import javax.xml.datatype.*;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Optional;


final class ValueComparator implements Comparator<Value>, Serializable {

	private boolean strict=true;


	public void setStrict(final boolean flag) {
		this.strict=flag;
	}

	public boolean isStrict() {
		return this.strict;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public int compare(final Value o1, final Value o2) {
		// check equality
		if ( o1 == o2 ) {
			return 0;
		}

		// 1. (Lowest) no value assigned to the variable
		if ( o1 == null ) {
			return -1;
		}
		if ( o2 == null ) {
			return 1;
		}

		// 2. Blank nodes
		final boolean b1=o1 instanceof BNode;
		final boolean b2=o2 instanceof BNode;

		if ( b1 && b2 ) {
			return compareBNodes((BNode)o1, (BNode)o2);
		}
		if ( b1 ) {
			return -1;
		}
		if ( b2 ) {
			return 1;
		}

		// 3. IRIs
		final boolean iri1=o1 instanceof IRI;
		final boolean iri2=o2 instanceof IRI;
		if ( iri1 && iri2 ) {
			return compareURIs((IRI)o1, (IRI)o2);
		}
		if ( iri1 ) {
			return -1;
		}
		if ( iri2 ) {
			return 1;
		}

		// 4. Literals
		final boolean l1=o1 instanceof Literal;
		final boolean l2=o2 instanceof Literal;
		if ( l1 && l2 ) {
			return compareLiterals((Literal)o1, (Literal)o2);
		}
		if ( l1 ) {
			return -1;
		}
		if ( l2 ) {
			return 1;
		}

		// 5. RDF* triples
		return compareTriples((Triple)o1, (Triple)o2);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private int compareBNodes(final BNode leftBNode, final BNode rightBNode) {
		return leftBNode.getID().compareTo(rightBNode.getID());
	}

	private int compareURIs(final IRI leftURI, final IRI rightURI) {
		return leftURI.toString().compareTo(rightURI.toString());
	}

	private int compareLiterals(final Literal leftLit, final Literal rightLit) {
		// Additional constraint for ORDER BY: "A plain literal is lower
		// than an RDF literal with type xsd:string of the same lexical
		// form."

		if ( !(isPlainLiteral(leftLit) || isPlainLiteral(rightLit)) ) {
			try {

				return compareLiterals(leftLit, rightLit, CompareOp.LT, strict) ? -1
						: compareLiterals(leftLit, rightLit, CompareOp.EQ, strict) ? 0
						: 1;

			} catch ( final ValueExprEvaluationException e ) {
				// literals cannot be compared using the '<' operator, continue
				// below
			}
		}

		int result=0;

		// FIXME: Confirm these rules work with RDF-1.1
		// Sort by datatype first, plain literals come before datatyped literals

		final IRI leftDatatype=leftLit.getDatatype();
		final IRI rightDatatype=rightLit.getDatatype();

		if ( leftDatatype != null ) {
			if ( rightDatatype != null ) {
				// Both literals have datatypes
				result=compareDatatypes(leftDatatype, rightDatatype);
			} else {
				result=1;
			}
		} else if ( rightDatatype != null ) {
			result=-1;
		}

		if ( result == 0 ) {
			// datatypes are equal or both literals are untyped; sort by language
			// tags, simple literals come before literals with language tags
			final Optional<String> leftLanguage=leftLit.getLanguage();
			final Optional<String> rightLanguage=rightLit.getLanguage();

			if ( leftLanguage.isPresent() ) {
				if ( rightLanguage.isPresent() ) {
					result=leftLanguage.get().compareTo(rightLanguage.get());
				} else {
					result=1;
				}
			} else if ( rightLanguage.isPresent() ) {
				result=-1;
			}
		}

		if ( result == 0 ) {
			// Literals are equal as fas as their datatypes and language tags are
			// concerned, compare their labels
			result=leftLit.getLabel().compareTo(rightLit.getLabel());
		}

		return result;
	}

	/**
	 * Compares two literal datatypes and indicates if one should be ordered after the other. This algorithm ensures
	 * that compatible ordered datatypes (numeric and date/time) are grouped together.
	 */
	private int compareDatatypes(final IRI leftDatatype, final IRI rightDatatype) {
		if ( XMLDatatypeUtil.isNumericDatatype(leftDatatype) ) {
			if ( XMLDatatypeUtil.isNumericDatatype(rightDatatype) ) {
				// both are numeric datatypes
				return compareURIs(leftDatatype, rightDatatype);
			} else {
				return -1;
			}
		} else if ( XMLDatatypeUtil.isNumericDatatype(rightDatatype) ) {
			return 1;
		} else if ( XMLDatatypeUtil.isCalendarDatatype(leftDatatype) ) {
			if ( XMLDatatypeUtil.isCalendarDatatype(rightDatatype) ) {
				// both are calendar datatypes
				return compareURIs(leftDatatype, rightDatatype);
			} else {
				return -1;
			}
		} else if ( XMLDatatypeUtil.isCalendarDatatype(rightDatatype) ) {
			return 1;
		} else {
			// incompatible or unordered datatypes
			return compareURIs(leftDatatype, rightDatatype);
		}
	}

	private int compareTriples(final Triple leftTriple, final Triple rightTriple) {
		int c=compare(leftTriple.getSubject(), rightTriple.getSubject());
		if ( c == 0 ) {
			c=compare(leftTriple.getPredicate(), rightTriple.getPredicate());
			if ( c == 0 ) {
				c=compare(leftTriple.getObject(), rightTriple.getObject());
			}
		}
		return c;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static boolean isSimpleLiteral(final Literal l) {
		return !Literals.isLanguageLiteral(l) && l.getDatatype().equals(XSD.STRING);
	}

	private static boolean isPlainLiteral(final Value v) {
		if ( v instanceof Literal ) {
			final Literal l=(Literal)v;
			return (l.getDatatype().equals(XSD.STRING));
		}
		return false;
	}

	private static boolean compareLiterals(
			final Literal leftLit, final Literal rightLit, final CompareOp operator, final boolean strict
	) throws ValueExprEvaluationException {
		// type precendence:
		// - simple literal
		// - numeric
		// - xsd:boolean
		// - xsd:dateTime
		// - xsd:string
		// - RDF term (equal and unequal only)

		final IRI leftDatatype=leftLit.getDatatype();
		final IRI rightDatatype=rightLit.getDatatype();

		final boolean leftLangLit=Literals.isLanguageLiteral(leftLit);
		final boolean rightLangLit=Literals.isLanguageLiteral(rightLit);

		// for purposes of query evaluation in SPARQL, simple literals and
		// string-typed literals with the same lexical value are considered equal.
		IRI commonDatatype=null;
		if ( isSimpleLiteral(leftLit) && isSimpleLiteral(rightLit) ) {
			commonDatatype=XSD.STRING;
		}

		Integer compareResult=null;

		if ( isSimpleLiteral(leftLit) && isSimpleLiteral(rightLit) ) {
			compareResult=leftLit.getLabel().compareTo(rightLit.getLabel());
		} else if ( (!leftLangLit && !rightLangLit) || commonDatatype != null ) {
			if ( commonDatatype == null ) {
				if ( leftDatatype.equals(rightDatatype) ) {
					commonDatatype=leftDatatype;
				} else if ( XMLDatatypeUtil.isNumericDatatype(leftDatatype)
						&& XMLDatatypeUtil.isNumericDatatype(rightDatatype) ) {
					// left and right arguments have different datatypes, try to find
					// a
					// more general, shared datatype
					if ( leftDatatype.equals(XSD.DOUBLE) || rightDatatype.equals(XSD.DOUBLE) ) {
						commonDatatype=XSD.DOUBLE;
					} else if ( leftDatatype.equals(XSD.FLOAT) || rightDatatype.equals(XSD.FLOAT) ) {
						commonDatatype=XSD.FLOAT;
					} else if ( leftDatatype.equals(XSD.DECIMAL) || rightDatatype.equals(XSD.DECIMAL) ) {
						commonDatatype=XSD.DECIMAL;
					} else {
						commonDatatype=XSD.INTEGER;
					}
				} else if ( !strict && XMLDatatypeUtil.isCalendarDatatype(leftDatatype)
						&& XMLDatatypeUtil.isCalendarDatatype(rightDatatype) ) {
					// We're not running in strict eval mode so we use extended datatype comparsion.
					commonDatatype=XSD.DATETIME;
				} else if ( !strict && XMLDatatypeUtil.isDurationDatatype(leftDatatype)
						&& XMLDatatypeUtil.isDurationDatatype(rightDatatype) ) {
					commonDatatype=XSD.DURATION;
				}
			}

			if ( commonDatatype != null ) {
				try {
					if ( commonDatatype.equals(XSD.DOUBLE) ) {
						compareResult=Double.compare(leftLit.doubleValue(), rightLit.doubleValue());
					} else if ( commonDatatype.equals(XSD.FLOAT) ) {
						compareResult=Float.compare(leftLit.floatValue(), rightLit.floatValue());
					} else if ( commonDatatype.equals(XSD.DECIMAL) ) {
						compareResult=leftLit.decimalValue().compareTo(rightLit.decimalValue());
					} else if ( XMLDatatypeUtil.isIntegerDatatype(commonDatatype) ) {
						compareResult=leftLit.integerValue().compareTo(rightLit.integerValue());
					} else if ( commonDatatype.equals(XSD.BOOLEAN) ) {
						final Boolean leftBool=leftLit.booleanValue();
						final Boolean rightBool=rightLit.booleanValue();
						compareResult=leftBool.compareTo(rightBool);
					} else if ( XMLDatatypeUtil.isCalendarDatatype(commonDatatype) ) {
						final XMLGregorianCalendar left=leftLit.calendarValue();
						final XMLGregorianCalendar right=rightLit.calendarValue();

						compareResult=left.compare(right);

						// Note: XMLGregorianCalendar.compare() returns compatible
						// values
						// (-1, 0, 1) but INDETERMINATE needs special treatment
						if ( compareResult == DatatypeConstants.INDETERMINATE ) {
							// If we compare two xsd:dateTime we should use the specific comparison specified in SPARQL
							// 1.1
							if ( leftDatatype.equals(XSD.DATETIME) && rightDatatype.equals(XSD.DATETIME) ) {
								throw new ValueExprEvaluationException("Indeterminate result for date/time comparison");
							} else {
								// We fallback to the regular RDF term compare
								compareResult=null;
							}

						}
					} else if ( !strict && XMLDatatypeUtil.isDurationDatatype(commonDatatype) ) {
						final Duration left=XMLDatatypeUtil.parseDuration(leftLit.getLabel());
						final Duration right=XMLDatatypeUtil.parseDuration(rightLit.getLabel());
						compareResult=left.compare(right);
						if ( compareResult == DatatypeConstants.INDETERMINATE ) {
							compareResult=null; // We fallback to regular term comparison
						}
					} else if ( commonDatatype.equals(XSD.STRING) ) {
						compareResult=leftLit.getLabel().compareTo(rightLit.getLabel());
					}
				} catch ( final IllegalArgumentException e ) {
					// One of the basic-type method calls failed, try syntactic match
					// before throwing an error
					if ( leftLit.equals(rightLit) ) {
						switch ( operator ) {
							case EQ:
								return true;
							case NE:
								return false;
						}
					}

					throw new ValueExprEvaluationException(e);
				}
			}
		}

		if ( compareResult != null ) {
			// Literals have compatible ordered datatypes
			switch ( operator ) {
				case LT:
					return compareResult.intValue() < 0;
				case LE:
					return compareResult.intValue() <= 0;
				case EQ:
					return compareResult.intValue() == 0;
				case NE:
					return compareResult.intValue() != 0;
				case GE:
					return compareResult.intValue() >= 0;
				case GT:
					return compareResult.intValue() > 0;
				default:
					throw new IllegalArgumentException("Unknown operator: "+operator);
			}
		} else {
			// All other cases, e.g. literals with languages, unequal or
			// unordered datatypes, etc. These arguments can only be compared
			// using the operators 'EQ' and 'NE'. See SPARQL's RDFterm-equal
			// operator

			final boolean literalsEqual=leftLit.equals(rightLit);

			if ( !literalsEqual ) {
				if ( !leftLangLit && !rightLangLit && isSupportedDatatype(leftDatatype)
						&& isSupportedDatatype(rightDatatype) ) {
					// left and right arguments have incompatible but supported
					// datatypes

					// we need to check that the lexical-to-value mapping for both
					// datatypes succeeds
					if ( !XMLDatatypeUtil.isValidValue(leftLit.getLabel(), leftDatatype) ) {
						throw new ValueExprEvaluationException("not a valid datatype value: "+leftLit);
					}

					if ( !XMLDatatypeUtil.isValidValue(rightLit.getLabel(), rightDatatype) ) {
						throw new ValueExprEvaluationException("not a valid datatype value: "+rightLit);
					}
					final boolean leftString=leftDatatype.equals(XSD.STRING);
					final boolean rightString=rightDatatype.equals(XSD.STRING);
					final boolean leftNumeric=XMLDatatypeUtil.isNumericDatatype(leftDatatype);
					final boolean rightNumeric=XMLDatatypeUtil.isNumericDatatype(rightDatatype);
					final boolean leftDate=XMLDatatypeUtil.isCalendarDatatype(leftDatatype);
					final boolean rightDate=XMLDatatypeUtil.isCalendarDatatype(rightDatatype);

					if ( leftString != rightString ) {
						throw new ValueExprEvaluationException("Unable to compare strings with other supported types");
					}
					if ( leftNumeric != rightNumeric ) {
						throw new ValueExprEvaluationException(
								"Unable to compare numeric types with other supported types");
					}
					if ( leftDate != rightDate ) {
						throw new ValueExprEvaluationException(
								"Unable to compare date types with other supported types");
					}
				} else if ( !leftLangLit && !rightLangLit ) {
					// For literals with unsupported datatypes we don't know if their
					// values are equal
					throw new ValueExprEvaluationException("Unable to compare literals with unsupported types");
				}
			}

			switch ( operator ) {
				case EQ:
					return literalsEqual;
				case NE:
					return !literalsEqual;
				case LT:
				case LE:
				case GE:
				case GT:
					throw new ValueExprEvaluationException(
							"Only literals with compatible, ordered datatypes can be compared using <, <=, > and >= "
									+ "operators");
				default:
					throw new IllegalArgumentException("Unknown operator: "+operator);
			}
		}
	}

	private static boolean isSupportedDatatype(final IRI datatype) {
		return (XSD.STRING.equals(datatype) || XMLDatatypeUtil.isNumericDatatype(datatype)
				|| XMLDatatypeUtil.isCalendarDatatype(datatype));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private enum CompareOp {
		/** equal to */
		EQ("="),

		/** not equal to */
		NE("!="),

		/** lower than */
		LT("<"),

		/** lower than or equal to */
		LE("<="),

		/** greater than or equal to */
		GE(">="),

		/** greater than */
		GT(">");

		private String symbol;

		CompareOp(final String symbol) {
			this.symbol=symbol;
		}

		public String getSymbol() {
			return symbol;
		}
	}

	private static final class ValueExprEvaluationException extends Throwable {

		private static final long serialVersionUID=4680955112469932617L;

		public ValueExprEvaluationException(final String message) { super(message);}

		public ValueExprEvaluationException(final IllegalArgumentException e) {
			super(e);
		}
	}

}
