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

package com.metreeca.gcp.services;

import com.metreeca.core.assets.Vault;

import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.secretmanager.v1beta1.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;


/**
 * Google Cloud secret vault.
 *
 * <p>Retrieves secrets managed by the Google Cloud Platform secret manager service.</p>
 *
 * <p><strong>Warning</strong> / Only letters, numbers, underscores and hyphens are supported in secret {@linkplain
 * #get(String) ids} </p>
 *
 * @see <a href="https://cloud.google.com/secret-manager/docs">Google Cloud Plaform - Secret Manager</a>
 */
public final class GCPVault implements Vault, AutoCloseable {

	/**
	 * The GCP project identifier.
	 */
	private static final String Project=System.getenv("GOOGLE_CLOUD_PROJECT");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final SecretManagerServiceClient client;


	/**
	 * Creates a new Google Cloud secret vault.
	 */
	public GCPVault() {
		try {

			this.client=SecretManagerServiceClient.create();

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}


	@Override public Optional<String> get(final String id) {

		if ( id == null ) {
			throw new NullPointerException("null id");
		}

		final AccessSecretVersionRequest request=AccessSecretVersionRequest.newBuilder()
				.setName(SecretVersionName.of(Project, id, "latest").toString())
				.build();

		try {

			return Optional.of(client.accessSecretVersion(request).getPayload().getData().toStringUtf8());

		} catch ( final NotFoundException e ) {

			return Optional.empty();

		}
	}

	@Override public void close() {
		client.close();
	}

}
