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
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableMap;


/**
 * Setup properties.
 *
 * <p>Provides access to system-wide setup properties.</p>
 */
public final class Setup {


	/**
	 * Setup property for the path of the custom setup properties file ({@value}={path} || {@code ./}{@value}).
	 *
	 * <p>This property is intended to be modified through a java command-line flag as
	 * <code>-D{@value}=&lt;path&gt;</code>.</p>
	 *
	 * <p>Relative paths paths are interpreted with respect to the current working directory of the process as read from
	 * the {@code user.dir} {@linkplain System#getProperty(String) system property}.</p>
	 */
	public static final String SetupProperty="setup.properties";

	/**
	 * Setup property for the path of the default storage folder ({@value}={path} || {@code .}).
	 *
	 * <p>Relative paths are interpreted with respect to the current working directory of the process as read from the
	 * {@code user.dir} {@linkplain System#getProperty(String) system property}.</p>
	 */
	public static final String StorageProperty="setup.storage";

	/**
	 * Setup property for the canonical server base URL ({@value}={URL} || {@code the base URL of the current HTTP
	 * request}).
	 */
	public static final String BaseProperty="setup.base";

	/**
	 * Setup property for the hardwired system administrator password ({@value}={pwd|digest} || empty = no hardwired
	 * system administrator).
	 */
	public static final String KeyProperty="setup.key";


	/**
	 * Setup factory.
	 *
	 * <p>The default setup acquired through this factory includes {@linkplain #system(Setup) system} properties and
	 * properties read from the {@linkplain #custom(Setup) custom} setup file, if defined.</p>
	 */
	public static final Tool<Setup> Tool=tools -> new Setup(asList(Setup::system, Setup::custom));


	/**
	 * Loads system properties.
	 *
	 * @param setup the target setup
	 *
	 * @return the global {@linkplain System#getProperties() system} properties
	 *
	 * @throws IllegalArgumentException if {@code setup} is {@code null}
	 */
	public static Properties system(final Setup setup) {

		if ( setup == null ) {
			throw new NullPointerException("null setup");
		}

		return System.getProperties();
	}

	/**
	 * Loads custom properties.
	 *
	 * @param setup the target setup
	 *
	 * @return custom properties read from a user-defined {@linkplain #SetupProperty setup} file, if defined, or from
	 * the {@value #SetupProperty} file under the default {@linkplain #StorageProperty storage} folder, if found
	 *
	 * @throws IllegalArgumentException if {@code setup} is {@code null}
	 */
	public static Properties custom(final Setup setup) {

		if ( setup == null ) {
			throw new NullPointerException("null setup");
		}

		final Optional<File> global=setup // custom setup file, if defined
				.get(SetupProperty)
				.map(File::new);

		final Optional<File> local=Optional // custom setup file in storage folder, if found
				.of(storage(setup))
				.map(file -> new File(file, SetupProperty))
				.filter(File::isFile);

		final Properties properties=new Properties();

		(global.isPresent() ? global : local).ifPresent(file -> {

			try (final InputStream input=new FileInputStream(file)) {
				properties.load(input);
			} catch ( final IOException e ) {
				throw new UncheckedIOException(format("unable to load custom setup file [%s]", file), e);
			}

		});

		return properties;
	}


	/**
	 * Retrieves the default storage folder.
	 *
	 * @param setup the target setup
	 *
	 * @return the default storage folder
	 *
	 * @throws IllegalArgumentException if {@code setup} is {@code null}
	 */
	public static File storage(final Setup setup) {

		if ( setup == null ) {
			throw new NullPointerException("null setup");
		}

		return setup.get(StorageProperty, new File(System.getProperty("user.dir")));
	}


	private static final Pattern UnderscorePattern=Pattern.compile("_");


	private final Map<String, String> setup=new TreeMap<>();
	private final Map<String, String> cache=new TreeMap<>();


	/**
	 * Creates a new setup.
	 *
	 * @param loaders setup properties loaders in decreasing precedence order
	 *
	 * @throws IllegalArgumentException if either {@code loaders} or one of its elements is {@code null}
	 */
	@SafeVarargs public Setup(final Function<Setup, Properties>... loaders) {
		this(asList(loaders));
	}

	/**
	 * Creates a new setup.
	 *
	 * @param loaders setup properties loaders in decreasing precedence order
	 *
	 * @throws IllegalArgumentException if either {@code loaders} or one of its elements is {@code null}
	 */
	public Setup(final Iterable<Function<Setup, Properties>> loaders) {

		if ( loaders == null ) {
			throw new NullPointerException("null loaders");
		}

		for (final Function<Setup, Properties> loader : loaders) {

			final Map<Object, Object> properties=loader.apply(this);

			for (final Map.Entry<Object, Object> entry : properties.entrySet()) {
				setup.putIfAbsent(entry.getKey().toString(), entry.getValue().toString());
			}
		}
	}


	/**
	 * Retrieves active setup property .
	 *
	 * @return a map containing properties actually retrieved from this setup
	 */
	public Map<String, String> properties() {
		return unmodifiableMap(cache);
	}


	/**
	 * Retrieves the value of a setup property.
	 *
	 * @param property the property to be retrieved
	 *
	 * @return the optional value of the given {@code property} or an empty optional value, if {@code property} is not
	 * defined
	 *
	 * @throws IllegalArgumentException if {@code property} is not {@code null}
	 */
	public Optional<String> get(final String property) {

		if ( property == null ) {
			throw new NullPointerException("null property");
		}

		return Optional.ofNullable(cache.computeIfAbsent(property, setup::get));
	}


	/**
	 * Retrieves the value of a boolean setup property with a fallback value.
	 *
	 * @param property the property to be retrieved
	 * @param fallback the fallback value for {@code property}
	 *
	 * @return the boolean value of the given {@code property} or the {@code fallback} value, if {@code property} is not
	 * defined
	 *
	 * @throws IllegalArgumentException if either {@code property}
	 */
	public boolean get(final String property, final boolean fallback) {

		if ( property == null ) {
			throw new NullPointerException("null property");
		}

		return get(property).map(Boolean::parseBoolean).orElse(fallback);
	}

	/**
	 * Retrieves the value of an integer setup property with a fallback value.
	 *
	 * @param property the property to be retrieved
	 * @param fallback the fallback value for {@code property}
	 *
	 * @return the integer value of the given {@code property} or the {@code fallback} value, if {@code property} is not
	 * defined or its textual value can't be parsed as an integer
	 *
	 * @throws IllegalArgumentException if either {@code property}
	 */
	public int get(final String property, final int fallback) {

		if ( property == null ) {
			throw new NullPointerException("null property");
		}

		return get(property).map(value -> {

			try {
				return Integer.parseInt(UnderscorePattern.matcher(value).replaceAll(""));
			} catch ( final NumberFormatException ignored ) {
				return fallback;
			}

		}).orElse(fallback);

	}

	/**
	 * Retrieves the value of a textual setup property with a fallback value.
	 *
	 * @param property the property to be retrieved
	 * @param fallback the fallback value for {@code property}
	 *
	 * @return the value of the given {@code property} or the {@code fallback} value, if {@code property} is not defined
	 *
	 * @throws IllegalArgumentException if either {@code property} or {@code fallback} is {@code null}
	 */
	public String get(final String property, final String fallback) {

		if ( property == null ) {
			throw new NullPointerException("null property");
		}

		if ( fallback == null ) {
			throw new NullPointerException("null fallback");
		}

		return get(property).orElse(fallback);
	}

	/**
	 * Retrieves the value of a file setup property with a fallback value.
	 *
	 * @param property the property to be retrieved
	 * @param fallback the fallback value for {@code property}
	 *
	 * @return the value of the given {@code property} or the {@code fallback} value, if {@code property} is not defined
	 *
	 * @throws IllegalArgumentException if either {@code property} or {@code fallback} is {@code null}
	 */
	public File get(final String property, final File fallback) {

		if ( property == null ) {
			throw new NullPointerException("null property");
		}

		if ( fallback == null ) {
			throw new NullPointerException("null fallback");
		}

		return get(property).map(File::new).orElse(fallback);
	}

}
