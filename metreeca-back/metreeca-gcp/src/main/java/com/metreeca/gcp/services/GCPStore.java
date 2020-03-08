/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.gcp.services;

import com.metreeca.gcp.GCP;
import com.metreeca.rest.services.Store;

import com.google.cloud.storage.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.function.BiFunction;
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

	@Override public InputStream read(final String id) {

		if ( id == null ) {
			throw new NullPointerException("null id");
		}

		return exec(id, this::read);
	}

	@Override public OutputStream write(final String id) {

		if ( id == null ) {
			throw new NullPointerException("null id");
		}

		return exec(id, this::write);
	}


	private <V> V exec(final String id, final BiFunction<String, String, V> task) {

		final Matcher matcher=IdPattern.matcher(id);

		return matcher.matches()
				? task.apply(matcher.group("bucket"), matcher.group("object"))
				: task.apply(GCP.Project+".appspot.com", id);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public InputStream read(final String bucket, final String object) {

		if ( bucket == null ) {
			throw new NullPointerException("null bucket");
		}

		if ( object == null ) {
			throw new NullPointerException("null object");
		}

		return Channels.newInputStream(storage.get(BlobId.of(bucket, object)).reader());
	}

	public OutputStream write(final String bucket, final String object) {

		if ( bucket == null ) {
			throw new NullPointerException("null bucket");
		}

		if ( object == null ) {
			throw new NullPointerException("null object");
		}

		return Channels.newOutputStream(storage.create(BlobInfo.newBuilder(bucket, object).build()).writer());
	}

}
