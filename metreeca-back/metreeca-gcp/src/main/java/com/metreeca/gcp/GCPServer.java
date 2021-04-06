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

package com.metreeca.gcp;

import com.metreeca.gcp.services.GCPStore;
import com.metreeca.gcp.services.GCPVault;
import com.metreeca.jse.JSEServer;
import com.metreeca.rest.Handler;
import com.metreeca.rest.Toolbox;

import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.Forbidden;
import static com.metreeca.rest.Toolbox.storage;
import static com.metreeca.rest.services.Store.store;
import static com.metreeca.rest.services.Vault.vault;

import static java.lang.String.format;

/**
 * Google Cloud Platform server connector.
 *
 * <p>Delegates a {@link JSEServer} pre-configured with Google Cloud Platform {@linkplain com.metreeca.gcp.services
 * services}.</p>
 */
public final class GCPServer {

	private static final String ProjectVariable="GOOGLE_CLOUD_PROJECT";
	private static final String ServiceVariable="GAE_SERVICE";
	private static final String AddressVariable="PORT";

	private static final String UnknownProject="unknown";
	private static final String DefaultService="default";


	/**
	 * Checks if running in the development environment
	 *
	 * @return {@code true} if running in the development environment; {@code false}, otherwise
	 */
	public static boolean development() {
		return project().equals(UnknownProject);
	}

	/**
	 * Checks if running in the production environment
	 *
	 * @return {@code true} if running in the production environment; {@code false}, otherwise
	 */
	public static boolean production() {
		return !development();
	}


	/**
	 * Retrieves the project name.
	 *
	 * @return the Google Cloud Platform project name or an empty string if unknown
	 */
	public static String project() {
		return System.getenv().getOrDefault(ProjectVariable, UnknownProject);
	}

	/**
	 * Retrieves the service name.
	 *
	 * @return the Google App Engine service name
	 */
	public static String service() {
		return System.getenv().getOrDefault(ServiceVariable, DefaultService);
	}


	/**
	 * Restricts access to a cron handler.
	 *
	 * @param handler the cron handler
	 *
	 * @return an access control handler restricting {@code handler} to requests issued by the Google App Engine cron
	 * service
	 *
	 * @throws NullPointerException if {@code handler} is null
	 */
	public static Handler cron(final Handler handler) {

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		return request -> request.headers("X-Appengine-Cron").contains("true")
				? handler.handle(request)
				: request.reply(status(Forbidden));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final JSEServer delegate=new JSEServer()

			.context(Optional.of(service())
					.filter(service -> !service.equals(DefaultService))
					.map(service -> format("/%s/", service))
					.orElse("/")
			)

			.address(System.getenv().getOrDefault(AddressVariable, ""));


	public GCPServer context(final String context) {

		if ( context == null ) {
			throw new NullPointerException("null context");
		}

		delegate.context(context);

		return this;
	}

	public GCPServer delegate(final Function<Toolbox, Handler> factory) {

		if ( factory == null ) {
			throw new NullPointerException("null factory");
		}

		delegate.delegate(context -> factory.apply(context

				.set(storage(), () -> Paths.get("/tmp"))

				.set(vault(), production() ? GCPVault::new : vault())
				.set(store(), production() ? GCPStore::new : store())

		));

		return this;
	}


	public void start() {
		delegate.start();
	}

}
