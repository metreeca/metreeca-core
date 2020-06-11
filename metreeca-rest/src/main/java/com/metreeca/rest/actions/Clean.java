package com.metreeca.rest.actions;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Locale;
import java.util.function.UnaryOperator;

/**
 * Text cleaning.
 *
 * <p>Cleans and normalizes text values.</p>
 */
public final class Clean implements UnaryOperator<String> {

	private static final Clean Instance=new Clean().space(true);


	/**
	 * Normalizes spaces.
	 *
	 * @param text the text to be normalized; may be null
	 *
	 * @returna a copy of {@code text} where leading and trailing sequences of control and separator characters are
	 * removed and other sequences replaced with a single space character
	 */
	public static String normalize(final String text) {
		return Instance.apply(text);
	}

	/**
	 * Normalize to lower case.
	 *
	 * @param text the text to be normalized; may be null
	 * @return a copy of {@code text} converted to lower case according to the {@link Locale#ROOT root locale}.
	 */
	public static String lower(final String text) {
		return text == null || text.isEmpty()? text : text.toLowerCase(Locale.ROOT);
	}

	/**
	 * Normalize to upper case.
	 *
	 * @param text the text to be normalized; may be null
	 * @return a copy of {@code text} converted to upper case according to the {@link Locale#ROOT root locale}.
	 */
	public static String upper(final String text) {
		return text == null || text.isEmpty()? text : text.toUpperCase(Locale.ROOT);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private boolean space;
	private boolean marks;
	private boolean smart;


	/**
	 * Configures space cleaning (defaults to {@code false}).
	 *
	 * @param space if {@code true}, leading and trailing sequences of control and separator characters are removed
	 *              and other sequences replaced with a single space character
	 *
	 * @return this action
	 */
	public Clean space(final boolean space) {

		this.space=space;

		return this;
	}

	/**
	 * Configures marks cleaning (defaults to {@code false}).
	 *
	 * @param marks if {@code true}, mark characters are removed
	 *
	 * @return this action
	 */
	public Clean marks(final boolean marks) {

		this.marks=marks;

		return this;
	}

	/**
	 * Configures typographical cleaning (defaults to {@code false}).
	 *
	 * @param smart if {@code true}, typographical characters (curly quotes, guillemets, …) are replaced with
	 *              equivalent plain ASCII characters
	 *
	 * @return this action
	 */
	public Clean smart(final boolean smart) {

		this.smart=smart;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Clean text values.
	 *
	 * @param text the text value to be cleaned; unmodified if null or empty
	 *
	 * @return the clean version of {@code text}
	 */
	@Override public String apply(final String text) {
		if ( text == null || text.isEmpty() ) { return text; } else {

			final char[] chars=Normalizer.normalize(text, marks ? Form.NFD : Form.NFC).toCharArray();

			int r=0;
			int w=0;

			boolean s=false;

			while ( r < chars.length ) {

				final char c=chars[r++];

				switch ( Character.getType(c) ) {

					case Character.CONTROL:
					case Character.SPACE_SEPARATOR:
					case Character.LINE_SEPARATOR:
					case Character.PARAGRAPH_SEPARATOR:

						if ( space ) {
							s=(w > 0);
						} else {
							chars[w++]=smart ? plain(c) : c;
						}

						break;

					case Character.ENCLOSING_MARK:
					case Character.NON_SPACING_MARK:
					case Character.COMBINING_SPACING_MARK:

						if ( !marks ) { chars[w++]=smart ? plain(c) : c; }

						break;

					default:

						if ( s ) {
							chars[w++]=' ';
							s=false;
						}

						chars[w++]=smart ? plain(c) : c;

						break;

				}
			}

			return new String(chars, 0, w);
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private char plain(final char c) {
		switch ( c ) {

			case '\u00A0': // no-break space
			case '\u2002': // en space
			case '\u2003': // em space

				return ' ';

			case '\u2012': // figure dash
			case '\u2013': // en dash
			case '\u2014': // em dash

				return '-';

			case '\u2018': // single smart opening quote
			case '\u2019': // single smart closing quote

				return '\'';

			case '\u201C': // double smart opening quote
			case '\u201D': // double smart closing quote

				return '"';

			case '\u2039': // single opening guillemet
			case '\u00AB': // double opening guillemet

				return '<';

			case '\u203A': // single closing guillemet
			case '\u00BB': // double closing guillemet

				return '>';

			default:

				return c;

		}
	}

}
