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

package com.metreeca.rest.assets;


import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.logging.Formatter;
import java.util.logging.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.rest.assets.Logger.Level.*;

import static java.util.Arrays.stream;


/**
 * Event logger.
 *
 * <p>Provides access to system-specific logging facilities.</p>
 */
public abstract class Logger {

	private static final int NameLengthLimit=80; // log args length limit

	private static final Map<String, java.util.logging.Logger> loggers=new HashMap<>(); // retain to prevent gc


	/**
	 * Logging levels.
	 */
	public enum Level {

		debug(java.util.logging.Level.FINE),
		info(java.util.logging.Level.INFO),
		warning(java.util.logging.Level.WARNING),
		error(java.util.logging.Level.SEVERE);

		private final java.util.logging.Level level;

		Level(final java.util.logging.Level level) {
			this.level=level;
		}


		/**
		 * Configures subsystem logging level.
		 *
		 * @param names fully-qualified names of packages/classes whose logging level is to be configured; empty for
		 *                    root
		 *              logger
		 *
		 * @return this logging level
		 *
		 * @throws NullPointerException if {@code names} is null or contains null elements
		 */
		public Level log(final String... names) {

			if ( names == null || stream(names).anyMatch(Objects::isNull) ) {
				throw new NullPointerException("null names");
			}

			(names.length > 0 ? stream(names) : Stream.of("")).forEach(name ->
					loggers.computeIfAbsent(name, java.util.logging.Logger::getLogger).setLevel(level)
			);

			return this;
		}

	}

	/**
	 * Retrieves the default logger factory.
	 *
	 * @return the default logger factory, which logs events through the the standard {@linkplain LogManager Java
	 * logging} facilities
	 */
	public static Supplier<Logger> logger() {
		return () -> {

			// logging not configured: reset and load compact console configuration ;(unless on GAE)

			if ( System.getProperty("java.util.logging.config.file") == null
					&& System.getProperty("java.util.logging.config.class") == null
					&& !"Production".equals(System.getProperty("com.google.appengine.runtime.environment"))
			) {

				final java.util.logging.Logger logger=java.util.logging.Logger.getLogger("");

				for (final Handler h : logger.getHandlers()) { logger.removeHandler(h); } // clear existing handlers

				logger.setLevel(java.util.logging.Level.INFO);

				final ConsoleHandler handler=new ConsoleHandler();

				handler.setLevel(java.util.logging.Level.ALL); // enable detailed reporting from children loggers
				handler.setFormatter(new ConsoleFormatter());

				logger.addHandler(handler);

			}

			return new SystemLogger();

		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Clips the textual representation of an object
	 *
	 * @param object the object whose textual representation is to be clipped
	 *
	 * @return the  textual representation of {@code object} clipped to a maximum length limit, or {@code null} if
	 * {@code
	 * object} is null
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
				: string.length() > NameLengthLimit ?
				string.substring(0, NameLengthLimit/2)+" … "+string.substring(string.length()-NameLengthLimit/2)
				: string;
	}


	/**
	 * Times the execution of a task.
	 *
	 * @param task the task whose execution is to be timed
	 *
	 * @return a function taking as argument a timing consumer and returning null; the timing consumer takes as
	 * arguments
	 * the {@code task} execution time in milliseconds
	 *
	 * @throws NullPointerException if {@code task} is null
	 */
	public static Function<LongConsumer, Void> time(final Runnable task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		return consumer -> time(() -> {

			task.run();

			return (Void)null;

		}).apply((t, v) -> consumer.accept(t));
	}

	/**
	 * Times the execution of a task.
	 *
	 * @param <V>  the type of the value generated by {@code task}
	 * @param task the task whose execution is to be timed
	 *
	 * @return a function taking as argument a timing consumer and returning the value generated by {@code task}; the
	 * timing consumer takes as arguments the {@code task} execution time in milliseconds and its generated value
	 *
	 * @throws NullPointerException if {@code task} is null
	 */
	public static <V> Function<BiConsumer<Long, V>, V> time(final Supplier<V> task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		return consumer -> {

			final long start=System.currentTimeMillis();

			final V value=task.get();

			final long stop=System.currentTimeMillis();

			consumer.accept(Math.max(stop-start, 1), value);

			return value;

		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Adds an error log entry.
	 *
	 * @param source  the source object for the log entry or {@code null} for global log entries
	 * @param message the message for the log entry
	 *
	 * @return this logger
	 */
	public final Logger error(final Object source, final String message) {
		return error(source, () -> message);
	}

	/**
	 * Adds an error log entry.
	 *
	 * @param source  the source object for the log entry or {@code null} for global log entries
	 * @param message the message supplier for the log entry
	 *
	 * @return this logger
	 */
	public final Logger error(final Object source, final Supplier<String> message) {
		return entry(error, source, message, null);
	}

	/**
	 * Adds an exceptional error log entry.
	 *
	 * @param source  the source object for the log entry or {@code null} for global log entries
	 * @param message the message for the log entry
	 * @param cause   the throwable that caused the traced exceptional condition
	 *
	 * @return this logger
	 */
	public final Logger error(final Object source, final String message, final Throwable cause) {
		return error(source, () -> message, cause);
	}

	/**
	 * Adds an exceptional error log entry.
	 *
	 * @param source  the source object for the log entry or {@code null} for global log entries
	 * @param message the message supplier for the log entry
	 * @param cause   the throwable that caused the traced exceptional condition
	 *
	 * @return this logger
	 */
	public final Logger error(final Object source, final Supplier<String> message, final Throwable cause) {
		return entry(error, source, message, cause);
	}


	/**
	 * Adds a warning log entry.
	 *
	 * @param source  the source object for the log entry or {@code null} for global log entries
	 * @param message the message for the log entry
	 *
	 * @return this logger
	 */
	public final Logger warning(final Object source, final String message) {
		return warning(source, () -> message);
	}

	/**
	 * Adds a warning log entry.
	 *
	 * @param source  the source object for the log entry or {@code null} for global log entries
	 * @param message the message supplier for the log entry
	 *
	 * @return this logger
	 */
	public final Logger warning(final Object source, final Supplier<String> message) {
		return entry(warning, source, message, null);
	}

	/**
	 * Adds an exceptional warning log entry.
	 *
	 * @param source  the source object for the log entry or {@code null} for global log entries
	 * @param message the message for the log entry
	 * @param cause   the throwable that caused the traced exceptional condition
	 *
	 * @return this logger
	 */
	public final Logger warning(final Object source, final String message, final Throwable cause) {
		return warning(source, () -> message, cause);
	}

	/**
	 * Adds an exceptional warning log entry.
	 *
	 * @param source  the source object for the log entry or {@code null} for global log entries
	 * @param message the message supplier for the log entry
	 * @param cause   the throwable that caused the traced exceptional condition
	 *
	 * @return this logger
	 */
	public final Logger warning(final Object source, final Supplier<String> message, final Throwable cause) {
		return entry(warning, source, message, cause);
	}


	/**
	 * Adds an info log entry.
	 *
	 * @param source  the source object for the log entry or {@code null} for global log entries
	 * @param message the message for the log entry
	 *
	 * @return this logger
	 */
	public final Logger info(final Object source, final String message) {
		return info(source, () -> message);
	}

	/**
	 * Adds an info log entry.
	 *
	 * @param source  the source object for the log entry or {@code null} for global log entries
	 * @param message the message supplier for the log entry
	 *
	 * @return this logger
	 */
	public final Logger info(final Object source, final Supplier<String> message) {
		return entry(info, source, message, null);
	}


	/**
	 * Adds a debug log entry.
	 *
	 * @param source  the source object for the log entry or {@code null} for global log entries
	 * @param message the message for the log entry
	 *
	 * @return this logger
	 */
	public final Logger debug(final Object source, final String message) {
		return debug(source, () -> message);
	}

	/**
	 * Adds a debug log entry.
	 *
	 * @param source  the source object for the log entry or {@code null} for global log entries
	 * @param message the message supplier for the log entry
	 *
	 * @return this logger
	 */
	public final Logger debug(final Object source, final Supplier<String> message) {
		return entry(debug, source, message, null);
	}


	/**
	 * Adds a log entry.
	 *
	 * @param level   the logging level for the log entry
	 * @param source  the source object for the log entry or {@code null} for global log entries; may be a
	 *                   human-readable
	 *                string label
	 * @param message the message supplier for the log entry
	 * @param cause   the throwable that caused the traced exceptional condition or {@code null} if immaterial
	 *
	 * @return this logger
	 */
	public abstract Logger entry(final Level level,
			final Object source, final Supplier<String> message,
			final Throwable cause
	);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class ConsoleFormatter extends Formatter {

		private static final Pattern EOLPattern=Pattern.compile("\n");


		@Override public String format(final LogRecord record) {

			return String.format(Locale.ROOT, "%3s %-15s %s%s\n",
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

		@Override public Logger entry(final Level level,
				final Object source, final Supplier<String> message, final Throwable cause
		) {

			final String logger=(source == null) ? ""
					: source instanceof String ? source.toString()
					: source instanceof Class ? ((Class<?>)source).getName()
					: source.getClass().getName();

			final LogRecord record=new LogRecord(level.level, message.get());

			record.setLoggerName(logger);
			record.setSourceClassName(logger);
			//record.setSourceMethodName(???); // !!! support
			record.setThrown(cause);

			java.util.logging.Logger.getLogger(logger).log(record);

			return this;
		}
	}

}
