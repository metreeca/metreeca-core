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

package com.metreeca.link.services;

import com.metreeca.link.Service;
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.sys.Setup;
import com.metreeca.tray.sys.Trace;

import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.FileSystemException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParserRegistry;

import java.io.*;
import java.nio.file.*;

import static com.metreeca.tray.Tray.tool;
import static com.metreeca.tray.sys.Setup.storage;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static java.util.concurrent.Executors.newSingleThreadExecutor;


/**
 * RDF spool service
 */
public final class Spool implements Service {

	// !!! default format
	// !!! default context

	private static final String IgnoredPrefix="~";
	private static final RDFFormat DefaultFormat=RDFFormat.TURTLE;


	private static boolean ignored(final String name) {
		return name.startsWith(IgnoredPrefix) || name.startsWith(".");
	}


	private final Setup setup=tool(Setup.Factory);
	private final Graph graph=tool(Graph.Factory);
	private final Trace trace=tool(Trace.Factory);


	private final File storage=setup.get("spool.storage", new File(storage(setup), "spool"));

	private final String base=setup.get("spool.base", setup.get(Setup.BaseProperty, ""));


	// !!! breaks on GAE even if excluded with a conditional test

	private final WatchService watcher=tool(() -> { // from tray to have it closed on system shutdown

		try {

			final WatchService service=FileSystems.getDefault().newWatchService();

			if ( storage.mkdirs() ) {
				trace.info(this, "created spooling folder at "+storage);
			}

			Paths.get(storage.toURI()).register(service, ENTRY_CREATE, ENTRY_MODIFY);

			return service;

		} catch ( final IOException e ) {
			throw new UncheckedIOException("unable to start file watch service at "+storage, e);
		}

	});


	@SuppressWarnings("unchecked") @Override public void load() {

		newSingleThreadExecutor().execute(() -> {
			try (final FileObject spool=VFS.getManager().resolveFile(storage.toURI())) {

				for (final FileObject object : spool.getChildren()) { // process initial content
					process(object);
				}

				for (final WatchKey key=watcher.take(); key.reset(); ) { // wait for updates
					for (final WatchEvent<?> event : key.pollEvents()) {
						if ( OVERFLOW.equals(event.kind()) ) {

							trace.error(this, "spooling queue overflow");

						} else {

							process(spool.resolveFile(((WatchEvent<Path>)event).context().toString()));

						}
					}
				}

			} catch ( final FileSystemException e ) {

				trace.error(this, "filesystem error on spooling thread", e);

			} catch ( final InterruptedException e ) {

				trace.error(this, "interrupted spooling thread", e);

			}
		});

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void process(final FileObject file) {
		try {

			final FileName name=file.getName();
			final String base=name.getBaseName();

			try {

				if ( !ignored(base) ) {

					final FileSystemManager manager=file.getFileSystem().getFileSystemManager();

					final FileObject archive // ;(vfs) unable to handle compressed files out of the box…
							=base.endsWith(".tgz") ? manager.resolveFile("tgz:"+name)
							: base.endsWith(".tar.gz") ? manager.resolveFile("tgz:"+name)
							: base.endsWith(".tbz2") ? manager.resolveFile("tbz2:"+name)
							: base.endsWith(".tar.bz2") ? manager.resolveFile("tbz2:"+name)
							: base.endsWith(".gz") ? manager.resolveFile("gz:"+name)
							: base.endsWith(".bz2") ? manager.resolveFile("bz2:"+name)
							: manager.canCreateFileSystem(file) ? manager.createFileSystem(file)
							: file;

					for (final FileObject item : archive.findFiles(Selectors.SELECT_FILES)) { upload(item); }

					trace.info(this, "processed "+file);

					file.deleteAll();
				}

			} catch ( final Exception e ) {

				trace.error(this, "unable to process "+file, e);

				file.moveTo(file.getParent().resolveFile(IgnoredPrefix+base));

			}
		} catch ( final FileSystemException e ) {
			trace.error(this, "filesystem error on spooling thread", e);
		}
	}

	private void upload(final FileObject file) {

		final String name=file.getName().getBaseName();

		if ( !ignored(name) ) {

			trace.info(this, "uploading "+file);

			graph.update(connection -> {
				try (final InputStream input=file.getContent().getInputStream()) {

					final RDFParserRegistry registry=RDFParserRegistry.getInstance();
					final RDFFormat format=registry.getFileFormatForFileName(name).orElse(DefaultFormat);

					connection.add(input, base, format);

					return null;

				} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}
			});
		}
	}

}
