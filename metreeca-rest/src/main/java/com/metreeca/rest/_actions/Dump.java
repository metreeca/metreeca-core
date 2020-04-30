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

package com.metreeca.rest._actions;

import com.metreeca.rest.Codecs;
import com.metreeca.rest.services.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.rest.Codecs.writer;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.services.Logger.logger;
import static java.nio.file.StandardOpenOption.*;


public final class Dump<V> implements Function<Stream<V>, Stream<String>>, Consumer<Stream<V>> {

	private final Path path;

	private final Logger logger=service(logger());


	public Dump(final Path path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		this.path=path.toAbsolutePath();
	}


	@Override public Stream<String> apply(final Stream<V> objects) {

		accept(objects);

		return Stream.of(path.toUri().toString());
	}

	@Override public void accept(final Stream<V> objects) {

		logger.info(this, String.format("dumping to <%s>", path));

		try {

			Files.createDirectories(path.getParent());
			FileChannel.open(path, CREATE, WRITE).truncate(0).close();

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}

		objects.forEachOrdered(object -> {

			try ( final Writer writer=writer(Files.newOutputStream(path, WRITE, APPEND)) ) {

				Codecs.text(writer, object.toString());

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}

		});
	}

}
