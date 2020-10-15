/*
 * Copyright © 2013-2020 Metreeca srl
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

import com.metreeca.rest.assets.Vault;

import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.secretmanager.v1.*;

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
