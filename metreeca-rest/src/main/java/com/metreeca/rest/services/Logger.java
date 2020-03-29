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

package com.metreeca.rest.services;


import java.io.*;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.logging.*;
import java.util.regex.Pattern;


/**
 * System logger.
 *
 * <p>Provides access to system-specific logging facilities.</p>
 */
public abstract class Logger {

	/**
	 * Severity level for log entries.
	 */
	public enum Level {

		Debug(java.util.logging.Level.FINE),
		Info(java.util.logging.Level.INFO),
		Warning(java.util.logging.Level.WARNING),
		Error(java.util.logging.Level.SEVERE);

		private final java.util.logging.Level level;

		Level(final java.util.logging.Level level) {
			this.level=level;
		}

		private java.util.logging.Level level() {
			return level;
		}

	}


	private static final int NameLengthLimit=80; // log args length limit


	/**
	 * Retrieves the default trace factory.
	 *
	 * @return the default trace factory, which logs trace records through the the standard {@linkplain LogManager Java
	 * logging} facilities
	 */
	public static Supplier<Logger> logger() {
		return () -> {

			// logging not configured: reset and load compact console configuration ;(unless on GAE)

			if ( System.getProperty("java.util.logging.config.file") == null
					&& System.getProperty("java.util.logging.config.class") == null
					&& !"Production".equals(System.getProperty("com.google.appengine.runtime.environment")) ) {

				final java.util.logging.Logger logger=java.util.logging.Logger.getLogger("");
				final java.util.logging.Level level=logger.getLevel(); // preserve log level

				LogManager.getLogManager().reset();

				final ConsoleHandler handler=new ConsoleHandler();

				handler.setLevel(level);
				handler.setFormatter(new ConsoleFormatter());

				logger.setLevel(level);
				logger.addHandler(handler);

			}

			return new SystemLogger();

		};
	}


	/**
	 * Clips the textual representation of an object
	 *
	 * @param object the object whose textual representation is to be clipped
	 *
	 * @return the  textual representation of {@code object} clipped to a maximum length limit, or {@code null} if
	 * {@code object} is null
	 */
	public static String clip(final Object object) {
		return clip(object == null ? null : object.toString());
	}

	/**
	 * Clips a string.
	 *
	 * @param string the string to be clipped
	 *
	 * @return the input {@code string} clipped to a maximum length limit, or {@code null} if {@code string} is null
	 */
	public static String clip(final String string) {
		return string == null || string.isEmpty() ? "?"
				: string.indexOf('\n') >= 0 ? clip(string.substring(0, string.indexOf('\n')))
				: string.length() > NameLengthLimit ? string.substring(0, NameLengthLimit/2)+" … "+string.substring(string.length()-NameLengthLimit/2)
				: string;
	}


	/**
	 * Times the execution of a task.
	 *
	 * @param task the task whose execution is to be timed
	 *
	 * @return the {@code task} execution time in milliseconds
	 */
	public static long time(final Runnable task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		final long start=System.currentTimeMillis();

		task.run();

		final long stop=System.currentTimeMillis();

		return Math.max(stop-start, 1);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Adds an error trace entry.
	 *
	 * @param source  the source object for the trace entry or {@code null} for global trace entries
	 * @param message the message for the trace entry
	 */
	public final void error(final Object source, final String message) { error(source, () -> message); }

	/**
	 * Adds an error trace entry.
	 *
	 * @param source  the source object for the trace entry or {@code null} for global trace entries
	 * @param message the message supplier for the trace entry
	 */
	public final void error(final Object source, final Supplier<String> message) {
		entry(Level.Error, source, message, null);
	}

	/**
	 * Adds an exceptional error trace entry.
	 *
	 * @param source  the source object for the trace entry or {@code null} for global trace entries
	 * @param message the message for the trace entry
	 * @param cause   the throwable that caused the traced exceptional condition
	 */
	public final void error(final Object source, final String message, final Throwable cause) { error(source, () -> message, cause); }

	/**
	 * Adds an exceptional error trace entry.
	 *
	 * @param source  the source object for the trace entry or {@code null} for global trace entries
	 * @param message the message supplier for the trace entry
	 * @param cause   the throwable that caused the traced exceptional condition
	 */
	public final void error(final Object source, final Supplier<String> message, final Throwable cause) {
		entry(Level.Error, source, message, cause);
	}


	/**
	 * Adds a warning trace entry.
	 *
	 * @param source  the source object for the trace entry or {@code null} for global trace entries
	 * @param message the message for the trace entry
	 */
	public final void warning(final Object source, final String message) { warning(source, () -> message); }

	/**
	 * Adds a warning trace entry.
	 *
	 * @param source  the source object for the trace entry or {@code null} for global trace entries
	 * @param message the message supplier for the trace entry
	 */
	public final void warning(final Object source, final Supplier<String> message) {
		entry(Level.Warning, source, message, null);
	}

	/**
	 * Adds an exceptional warning trace entry.
	 *
	 * @param source  the source object for the trace entry or {@code null} for global trace entries
	 * @param message the message for the trace entry
	 * @param cause   the throwable that caused the traced exceptional condition
	 */
	public final void warning(final Object source, final String message, final Throwable cause) { warning(source, () -> message, cause); }

	/**
	 * Adds an exceptional warning trace entry.
	 *
	 * @param source  the source object for the trace entry or {@code null} for global trace entries
	 * @param message the message supplier for the trace entry
	 * @param cause   the throwable that caused the traced exceptional condition
	 */
	public final void warning(final Object source, final Supplier<String> message, final Throwable cause) {
		entry(Level.Warning, source, message, cause);
	}


	/**
	 * Adds an info trace entry.
	 *
	 * @param source  the source object for the trace entry or {@code null} for global trace entries
	 * @param message the message for the trace entry
	 */
	public final void info(final Object source, final String message) { info(source, () -> message); }

	/**
	 * Adds an info trace entry.
	 *
	 * @param source  the source object for the trace entry or {@code null} for global trace entries
	 * @param message the message supplier for the trace entry
	 */
	public final void info(final Object source, final Supplier<String> message) {
		entry(Level.Info, source, message, null);
	}


	/**
	 * Adds a debug trace entry.
	 *
	 * @param source  the source object for the trace entry or {@code null} for global trace entries
	 * @param message the message for the trace entry
	 */
	public final void debug(final Object source, final String message) { debug(source, () -> message); }

	/**
	 * Adds a debug trace entry.
	 *
	 * @param source  the source object for the trace entry or {@code null} for global trace entries
	 * @param message the message supplier for the trace entry
	 */
	public final void debug(final Object source, final Supplier<String> message) {
		entry(Level.Debug, source, message, null);
	}


	/**
	 * Adds a trace entry.
	 *
	 * @param level   the logging level for the trace entry
	 * @param source  the source object for the trace entry or {@code null} for global trace entries; may be a
	 *                human-readable string label
	 * @param message the message supplier for the trace entry
	 * @param cause   the throwable that caused the traced exceptional condition or {@code null} if immaterial
	 */
	public abstract void entry(final Level level,
			final Object source, final Supplier<String> message, final Throwable cause);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class ConsoleFormatter extends Formatter {

		private static final Pattern EOLPattern=Pattern.compile("\n");


		@Override public String format(final LogRecord record) {

			return String.format(Locale.ROOT, "%3s %-12s %s%s\n",
					level(record.getLevel()),
					name(record.getLoggerName()),
					message(record.getMessage()),
					trace(record.getThrown()));

		}


		private String level(final java.util.logging.Level level) {
			return level.equals(java.util.logging.Level.SEVERE) ? "!!!"
					: level.equals(java.util.logging.Level.WARNING) ? "!!"
					: level.equals(java.util.logging.Level.INFO) ? "!"
					: level.equals(java.util.logging.Level.FINE) ? "?"
					: level.equals(java.util.logging.Level.FINER) ? "??"
					: level.equals(java.util.logging.Level.FINEST) ? "???"
					: "";
		}

		private String name(final String name) {
			return name == null ? "<global>" : name.substring(name.lastIndexOf('.')+1);
		}

		private String message(final CharSequence message) {
			return message == null ? "" : EOLPattern.matcher(message).replaceAll("\n    ");
		}

		private String trace(final Throwable cause) {
			if ( cause == null ) { return ""; } else {
				try (
						final StringWriter writer=new StringWriter();
						final PrintWriter printer=new PrintWriter(writer.append(' '))
				) {

					printer.append("caused by ");

					cause.printStackTrace(printer);

					return writer.toString();

				} catch ( final IOException unexpected ) {
					throw new UncheckedIOException(unexpected);
				}
			}
		}

	}

	private static final class SystemLogger extends Logger {

		@Override public void entry(final Level level,
				final Object source, final Supplier<String> message, final Throwable cause) {

			final String logger=(source == null) ? ""
					: source instanceof String ? source.toString()
					: source instanceof Class ? ((Class<?>)source).getName()
					: source.getClass().getName();

			final LogRecord record=new LogRecord(level.level(), message.get());

			record.setLoggerName(logger);
			record.setSourceClassName(logger);
			//record.setSourceMethodName(???); // !!! support
			record.setThrown(cause);

			java.util.logging.Logger.getLogger(logger).log(record);

		}
	}

}
