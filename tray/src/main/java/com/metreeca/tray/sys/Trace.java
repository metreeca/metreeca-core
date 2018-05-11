/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.tray.sys;


import com.metreeca.tray.Tool;

import java.io.*;
import java.util.Locale;
import java.util.logging.*;


/**
 * Execution trace log.
 *
 * <p>Concrete implementations must be thread-safe.</p>
 */
public abstract class Trace {

	public enum Level {

		Debug(java.util.logging.Level.FINE), Info(java.util.logging.Level.INFO), Warning(java.util.logging.Level.WARNING), Error(java.util.logging.Level.SEVERE);

		private final java.util.logging.Level level;

		Level(final java.util.logging.Level level) {
			this.level=level;
		}

		private java.util.logging.Level level() {
			return level;
		}
	}


	private static final int NameLengthLimit=80; // log args length limit


	public static final Tool<Trace> Tool=tools -> {

		// logging not configured: reset and load compact console configuration

		if ( System.getProperty("java.util.logging.config.file") == null
				&& System.getProperty("java.util.logging.config.class") == null ) {

			LogManager.getLogManager().reset();

			final ConsoleHandler handler=new ConsoleHandler();

			handler.setLevel(java.util.logging.Level.INFO);
			handler.setFormatter(new ConsoleFormatter());

			final Logger logger=Logger.getLogger("");

			logger.setLevel(java.util.logging.Level.INFO);
			logger.addHandler(handler);

		}

		return new Trace() {
			@Override public void entry(final Level level,
					final Object source, final String message, final Throwable cause) {

				final String logger=(source == null) ? ""
						: source instanceof String ? source.toString()
						: source instanceof Class ? ((Class<?>)source).getName()
						: source.getClass().getName();

				final LogRecord record=new LogRecord(level.level(), message);

				record.setSourceClassName(logger);
				record.setSourceMethodName("class"); // !!! support
				record.setThrown(cause);

				Logger.getLogger(logger).log(record);

			}
		};
	};


	public static String clip(final Object arg) {
		return clip(arg == null ? null : arg.toString());
	}

	public static String clip(final String arg) {
		return arg == null || arg.isEmpty() ? "?"
				: arg.indexOf('\n') >= 0 ? clip(arg.substring(0, arg.indexOf('\n')))
				: arg.length() > NameLengthLimit ? arg.substring(0, NameLengthLimit/2)+" … "+arg.substring(arg.length()-NameLengthLimit/2)
				: arg;
	}


	public static long time(final Runnable runnable) {

		if ( runnable == null ) {
			throw new NullPointerException("null runnable");
		}

		final long start=System.currentTimeMillis();

		runnable.run();

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
	public void error(final Object source, final String message) {
		entry(Level.Error, source, message, null);
	}

	/**
	 * Adds an exceptional error trace entry.
	 *
	 * @param source  the source object for the trace entry or {@code null} for global trace entries
	 * @param message the message for the trace entry
	 * @param cause   the throwable that caused the traced exceptional condition
	 */
	public final void error(final Object source, final String message, final Throwable cause) {
		entry(Level.Error, source, message, cause);
	}

	/**
	 * Adds a warning trace entry.
	 *
	 * @param source  the source object for the trace entry or {@code null} for global trace entries
	 * @param message the message for the trace entry
	 */
	public final void warning(final Object source, final String message) {
		entry(Level.Warning, source, message, null);
	}

	/**
	 * Adds an exceptional warning trace entry.
	 *
	 * @param source  the source object for the trace entry or {@code null} for global trace entries
	 * @param message the message for the trace entry
	 * @param cause   the throwable that caused the traced exceptional condition
	 */
	public final void warning(final Object source, final String message, final Throwable cause) {
		entry(Level.Warning, source, message, cause);
	}

	/**
	 * Adds an info trace entry.
	 *
	 * @param source  the source object for the trace entry or {@code null} for global trace entries
	 * @param message the message for the trace entry
	 */
	public final void info(final Object source, final String message) {
		entry(Level.Info, source, message, null);
	}

	/**
	 * Adds a debug trace entry.
	 *
	 * @param source  the source object for the trace entry or {@code null} for global trace entries
	 * @param message the message for the trace entry
	 */
	public final void debug(final Object source, final String message) {
		entry(Level.Debug, source, message, null);
	}


	/**
	 * Adds a trace entry.
	 *
	 * @param level   the logging level for the trace entry
	 * @param source  the source object for the trace entry or {@code null} for global trace entries; may be a
	 *                human-readable string label
	 * @param message the message for the trace entry
	 * @param cause   the throwable that caused the traced exceptional condition or {@code null} if immaterial
	 */
	public abstract void entry(final Level level, final Object source, final String message, final Throwable cause);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class ConsoleFormatter extends Formatter {

		@Override public String format(final LogRecord record) {

			return String.format(Locale.ROOT, "%3s %s: %s%s\n",
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

		private String message(final String message) {
			return message != null ? message.replaceAll("\n", "\n    ") : "";
		}

		private String trace(final Throwable cause) {
			if ( cause == null ) { return ""; } else {
				try (
						final StringWriter writer=new StringWriter();
						final PrintWriter printer=new PrintWriter(writer.append(' '))
				) {

					cause.printStackTrace(printer);

					return writer.toString();

				} catch ( final IOException unexpected ) {
					throw new UncheckedIOException(unexpected);
				}
			}
		}

	}

}
