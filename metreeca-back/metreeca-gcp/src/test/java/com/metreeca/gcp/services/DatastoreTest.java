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

package com.metreeca.gcp.services;

import com.metreeca.rest.Toolbox;

import com.google.cloud.datastore.DatastoreOptions;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.*;
import java.util.Optional;

import static java.lang.String.format;

public final class DatastoreTest {

	private static final String TestProject="metreeca-gcp-test";

	private static final int DatastorePort=8910;


	public static void main(final String... args) {
		datastore();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static boolean datastore() {
		try {

			final String command="gcloud beta emulators datastore start"
					+" --no-store-on-disk"
					+" --host-port localhost:"+DatastorePort
					+" --project "+TestProject;

			final Process emulator=new ProcessBuilder(command.split("\\s+"))
					.redirectOutput(Redirect.INHERIT)
					.redirectError(Redirect.INHERIT)
					.start();

			final Thread thread=new Thread(() -> {
				try { emulator.waitFor(); } catch ( final InterruptedException ignored ) {}
			});

			thread.setDaemon(true);
			thread.start();

			Runtime.getRuntime().addShutdownHook(new Thread(emulator::destroyForcibly));

			return true;

		} catch ( final IOException e ) {

			System.err.printf("no datastore emulator (%s)", Optional.ofNullable(e.getMessage()).orElseGet(e::toString));

			return false;

		}
	}

	public static void datastore(final Runnable... tasks) {

		final DatastoreOptions options=DatastoreOptions.newBuilder()
				.setProjectId(TestProject)
				.setHost("localhost:"+DatastorePort)
				.build();

		try { // reset test datastore

			final URL reset=new URL(format("http://localhost:%d/reset", DatastorePort));
			final HttpURLConnection connection=(HttpURLConnection)reset.openConnection();

			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.connect();

			connection.getResponseCode();

			Thread.sleep(100);

			new Toolbox().set(Datastore.datastore(), () -> new Datastore(options)).exec(tasks).clear();

		} catch ( final ConnectException e ) { // start test datastore and retry

			if ( datastore() ) {
				new Toolbox().set(Datastore.datastore(), () -> new Datastore(options)).exec(tasks).clear();
			}

		} catch ( final InterruptedException ignored ) {

		} catch ( final IOException e ) {

			throw new UncheckedIOException(e);

		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private DatastoreTest() {}

}
