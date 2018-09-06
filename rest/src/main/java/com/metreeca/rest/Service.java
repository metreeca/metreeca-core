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

package com.metreeca.rest;

import com.metreeca.tray._Tray;

import java.util.ServiceLoader;
import java.util.function.Supplier;


/**
 * Linked data service.
 *
 * <p>Services enable linked data applications to configure the {@linkplain Server server} or other shared tools; the
 * most common use case is binding a custom resource {@linkplain Handler handler} to the server {@linkplain Index
 * index}.</p>
 *
 * <p>Custom services listed in the {@code com.metreeca.link.Service} {@linkplain ServiceLoader service loader}
 * provider configuration file in the {@code META-INF/services/} resource directory of an application will be
 * automatically {@linkplain #load() loaded} by server adapters inside a {@linkplain _Tray#lookup(Runnable) read-only
 * task} on a shared tool tray.</p>
 */
@FunctionalInterface public interface Service {

	/**
	 * Loads a custom service.
	 *
	 * <p>Required tools may be retrieved from the shared tool tray using the static {@linkplain _Tray#tool(Supplier)
	 * tool lookup} method.</p>
	 */
	public void load();

}
