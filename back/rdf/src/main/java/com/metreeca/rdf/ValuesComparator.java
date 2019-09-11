/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package com.metreeca.rdf;

import org.eclipse.rdf4j.common.lang.ObjectUtil;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

import java.util.Comparator;
import java.util.Optional;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * !!! copied from query algebra evaluation mmodule to avoid dependencies on repository API !!!
 *
 * A comparator that compares values according the SPARQL value ordering as specified in
 * <A href="http://www.w3.org/TR/rdf-sparql-query/#modOrderBy">SPARQL Query Language for RDF</a>.
 *
 * @author james
 * @author Arjohn Kampman
 */
final class ValuesComparator implements Comparator<Value> {

	@Override
	public int compare(final Value x, final Value y) {
		// check equality
		if ( ObjectUtil.nullEquals(x, y) ) {
			return 0;
		}

		// 1. (Lowest) no value assigned to the variable
		if ( x == null ) {
			return -1;
		}
		if ( y == null ) {
			return 1;
		}

		// 2. Blank nodes
		final boolean b1=x instanceof BNode;
		final boolean b2=y instanceof BNode;
		if ( b1 && b2 ) {
			return compareBNodes((BNode)x, (BNode)y);
		}
		if ( b1 ) {
			return -1;
		}
		if ( b2 ) {
			return 1;
		}

		// 3. IRIs
		final boolean u1=x instanceof IRI;
		final boolean u2=y instanceof IRI;
		if ( u1 && u2 ) {
			return compareURIs((IRI)x, (IRI)y);
		}
		if ( u1 ) {
			return -1;
		}
		if ( u2 ) {
			return 1;
		}

		// 4. RDF literals
		return compareLiterals((Literal)x, (Literal)y);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private int compareBNodes(final BNode x, final BNode y) {
		return x.getID().compareTo(y.getID());
	}

	private int compareURIs(final IRI x, final IRI y) {
		return x.toString().compareTo(y.toString());
	}

	private int compareLiterals(final Literal x, final Literal y) {
		// Additional constraint for ORDER BY: "A plain literal is lower
		// than an RDF literal with type xsd:string of the same lexical
		// form."
		if ( !(isPlainLiteral(x) || isPlainLiteral(y)) ) {
			try {
				final boolean isSmaller=compareLiterals(x, y, CompareOp.LT);

				if ( isSmaller ) {
					return -1;
				} else {
					final boolean isEquivalent=compareLiterals(x, y, CompareOp.EQ);
					if ( isEquivalent ) {
						return 0;
					}
					return 1;
				}
			} catch ( final ValueExprEvaluationException e ) {
				// literals cannot be compared using the '<' operator, continue
				// below
			}
		}

		int result=0;

		// FIXME: Confirm these rules work with RDF-1.1
		// Sort by datatype first, plain literals come before datatyped literals
		final IRI leftDatatype=x.getDatatype();
		final IRI rightDatatype=y.getDatatype();

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
			final Optional<String> leftLanguage=x.getLanguage();
			final Optional<String> rightLanguage=y.getLanguage();

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
			result=x.getLabel().compareTo(y.getLabel());
		}

		return result;
	}

	/**
	 * Compares two literal datatypes and indicates if one should be ordered after the other. This algorithm ensures
	 * that compatible ordered datatypes (numeric and date/time) are grouped together so that {@link
	 * ValuesComparator#compareLiterals(Literal, Literal, CompareOp)} is used in consecutive ordering steps.
	 */
	private int compareDatatypes(final IRI x, final IRI rightdatatype) {
		if ( XMLDatatypeUtil.isNumericDatatype(x) ) {
			if ( XMLDatatypeUtil.isNumericDatatype(rightdatatype) ) {
				// both are numeric datatypes
				return compareURIs(x, rightdatatype);
			} else {
				return -1;
			}
		} else if ( XMLDatatypeUtil.isNumericDatatype(rightdatatype) ) {
			return 1;
		} else if ( XMLDatatypeUtil.isCalendarDatatype(x) ) {
			if ( XMLDatatypeUtil.isCalendarDatatype(rightdatatype) ) {
				// both are calendar datatypes
				return compareURIs(x, rightdatatype);
			} else {
				return -1;
			}
		} else if ( XMLDatatypeUtil.isCalendarDatatype(rightdatatype) ) {
			return 1;
		} else {
			// incompatible or unordered datatypes
			return compareURIs(x, rightdatatype);
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Compares the supplied {@link Literal} arguments using the supplied operator.
	 *
	 * @param leftLit  the left literal argument of the comparison.
	 * @param rightLit the right literal argument of the comparison.
	 * @param operator the comparison operator to use.
	 * @return {@code true} if execution of the supplied operator on the supplied arguments succeeds, {@code false}
	 * otherwise.
	 *
	 * @throws ValueExprEvaluationException if a type error occurred.
	 */
	private static boolean compareLiterals(final Literal leftLit, final Literal rightLit, final CompareOp operator)
			throws ValueExprEvaluationException {
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
			commonDatatype=XMLSchema.STRING;
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
					if ( leftDatatype.equals(XMLSchema.DOUBLE) || rightDatatype.equals(XMLSchema.DOUBLE) ) {
						commonDatatype=XMLSchema.DOUBLE;
					} else if ( leftDatatype.equals(XMLSchema.FLOAT) || rightDatatype.equals(XMLSchema.FLOAT) ) {
						commonDatatype=XMLSchema.FLOAT;
					} else if ( leftDatatype.equals(XMLSchema.DECIMAL) || rightDatatype.equals(XMLSchema.DECIMAL) ) {
						commonDatatype=XMLSchema.DECIMAL;
					} else {
						commonDatatype=XMLSchema.INTEGER;
					}
				} else {
					if ( false ) {
						// We're not running in strict eval mode so we use extended datatype comparsion.
						commonDatatype=XMLSchema.DATETIME;
					} else {
						if ( false ) {
							commonDatatype=XMLSchema.DURATION;
						}
					}
				}
			}

			if ( commonDatatype != null ) {
				try {
					if ( commonDatatype.equals(XMLSchema.DOUBLE) ) {
						compareResult=Double.compare(leftLit.doubleValue(), rightLit.doubleValue());
					} else if ( commonDatatype.equals(XMLSchema.FLOAT) ) {
						compareResult=Float.compare(leftLit.floatValue(), rightLit.floatValue());
					} else if ( commonDatatype.equals(XMLSchema.DECIMAL) ) {
						compareResult=leftLit.decimalValue().compareTo(rightLit.decimalValue());
					} else if ( XMLDatatypeUtil.isIntegerDatatype(commonDatatype) ) {
						compareResult=leftLit.integerValue().compareTo(rightLit.integerValue());
					} else if ( commonDatatype.equals(XMLSchema.BOOLEAN) ) {
						final Boolean leftBool=Boolean.valueOf(leftLit.booleanValue());
						final Boolean rightBool=Boolean.valueOf(rightLit.booleanValue());
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
							if ( leftDatatype.equals(XMLSchema.DATETIME) && rightDatatype.equals(XMLSchema.DATETIME) ) {
								throw new ValueExprEvaluationException("Indeterminate result for date/time comparison");
							} else {
								// We fallback to the regular RDF term compare
								compareResult=null;
							}

						}
					} else if ( commonDatatype.equals(XMLSchema.STRING) ) {
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
					return compareResult < 0;
				case LE:
					return compareResult <= 0;
				case EQ:
					return compareResult == 0;
				case NE:
					return compareResult != 0;
				case GE:
					return compareResult >= 0;
				case GT:
					return compareResult > 0;
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
					final boolean leftString=leftDatatype.equals(XMLSchema.STRING);
					final boolean rightString=rightDatatype.equals(XMLSchema.STRING);
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
							"Only literals with compatible, ordered datatypes can be compared using <, <=, > and >= operators");
				default:
					throw new IllegalArgumentException("Unknown operator: "+operator);
			}
		}
	}

	/**
	 * Checks whether the supplied value is a "plain literal". A "plain literal" is a literal with no datatype and
	 * optionally a language tag.
	 *
	 * @see <a href="http://www.w3.org/TR/2004/REC-rdf-concepts-20040210/#dfn-plain-literal">RDF Literal
	 * Documentation</a>
	 */
	private static boolean isPlainLiteral(final Value v) {
		if ( v instanceof Literal ) {
			final Literal l=(Literal)v;
			return l.getDatatype().equals(XMLSchema.STRING);
		}
		return false;
	}

	/**
	 * Checks whether the supplied literal is a "simple literal". A "simple literal" is a literal with no language tag
	 * and the datatype {@link XMLSchema#STRING}.
	 *
	 * @see <a href="http://www.w3.org/TR/sparql11-query/#simple_literal">SPARQL Simple Literal Documentation</a>
	 */
	private static boolean isSimpleLiteral(final Literal l) {
		return !Literals.isLanguageLiteral(l) && l.getDatatype().equals(XMLSchema.STRING);
	}

	private static boolean isSupportedDatatype(final IRI datatype) {
		return XMLSchema.STRING.equals(datatype)
				|| XMLDatatypeUtil.isNumericDatatype(datatype)
				|| XMLDatatypeUtil.isCalendarDatatype(datatype);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private enum CompareOp {

		/**
		 * equal to
		 */
		EQ(),

		/**
		 * not equal to
		 */
		NE(),

		/**
		 * lower than
		 */
		LT(),

		/**
		 * lower than or equal to
		 */
		LE(),

		/**
		 * greater than or equal to
		 */
		GE(),

		/**
		 * greater than
		 */
		GT();

	}


	private static final class ValueExprEvaluationException extends RuntimeException {

		private static final long serialVersionUID=-3633440570594631529L;


		ValueExprEvaluationException(final String message) {
			super(message);
		}

		ValueExprEvaluationException(final Throwable t) {
			super(t);
		}

	}

}
