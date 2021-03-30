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

package com.metreeca.gcp.assets;

import com.metreeca.rest.assets.Store;

import com.google.cloud.storage.*;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.file.NoSuchFileException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Google Cloud blob store.
 *
 * <p>Retrieves blobs managed by the Google Cloud Storage service.</p>
 *
 * <p>For both {@linkplain #read(String) read} and {@linkplain #write(String) write} operations, blob identifiers not
 * matching a full GCS URI (i.e. {@code gs://{bucket}/{object}}) or link URL (i.e. {@code
 * https://storage.cloud.google.com/{bucket}/{object}) are interpreted as object names in the default project bucket
 * (i.e. {@code {project}.appspot.com}}.</p>
 *
 * @see <a href="https://cloud.google.com/storage/docs">Google Cloud Plaform - Storage</a>
 */
public final class GCPStore implements Store {

	private static final Pattern IdPattern=Pattern
			.compile("(?:https://storage\\.cloud\\.google\\.com/|gs://)(?<bucket>[^/]+)/(?<object>[^/]+)");


	@FunctionalInterface private static interface Task<R> {

		public R exec(final String bucket, final String object) throws IOException;

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Storage storage;


	public GCPStore() {
		this(StorageOptions.getDefaultInstance());
	}

	public GCPStore(final StorageOptions options) {

		if ( options == null ) {
			throw new NullPointerException("null options");
		}

		this.storage=options.getService();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public InputStream read(final String id) throws IOException {

		if ( id == null ) {
			throw new NullPointerException("null id");
		}

		return exec(id, this::read);
	}

	@Override public OutputStream write(final String id) throws IOException {

		if ( id == null ) {
			throw new NullPointerException("null id");
		}

		return exec(id, this::write);
	}


	private <V> V exec(final String id, final Task<V> task) throws IOException {

		final Matcher matcher=IdPattern.matcher(id);

		return matcher.matches()
				? task.exec(matcher.group("bucket"), matcher.group("object"))
				: task.exec(storage.getOptions().getProjectId()+".appspot.com", id);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public InputStream read(final String bucket, final String object) throws IOException {

		if ( bucket == null ) {
			throw new NullPointerException("null bucket");
		}

		if ( object == null ) {
			throw new NullPointerException("null object");
		}

		try {

			final Blob blob=storage.get(BlobId.of(bucket, object));

			if ( blob == null ) {
				throw new NoSuchFileException(String.format("gs://%s/%s", bucket, object));
			}

			return Channels.newInputStream(blob.reader());

		} catch ( final StorageException e ) {
			throw new IOException(e);
		}
	}

	public OutputStream write(final String bucket, final String object) throws IOException {

		if ( bucket == null ) {
			throw new NullPointerException("null bucket");
		}

		if ( object == null ) {
			throw new NullPointerException("null object");
		}

		try {

			return Channels.newOutputStream(storage.create(BlobInfo.newBuilder(bucket, object).build()).writer());

		} catch ( final StorageException e ) {
			throw new IOException(e);
		}
	}

}
