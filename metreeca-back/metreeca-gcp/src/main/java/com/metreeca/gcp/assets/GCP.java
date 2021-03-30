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

/**
 * Google Cloud utilities
 */
public final class GCP {

	private static final boolean production=System.getenv().containsKey("GOOGLE_CLOUD_PROJECT");


	/**
	 * Checks if running in the production environment
	 *
	 * @return {@code true} if running in the production environment; {@code false}, otherwise
	 */
	public static boolean production() {
		return production;
	}

	/**
	 * Checks if running in the development environment
	 *
	 * @return {@code true} if running in the development environment; {@code false}, otherwise
	 */
	public static boolean development() {
		return !production;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private GCP() {}

}
