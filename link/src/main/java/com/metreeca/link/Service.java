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

package com.metreeca.link;


import com.metreeca.link._meta._Server;
import com.metreeca.link._meta.Index;
import com.metreeca.tray.Tray;

import java.util.ServiceLoader;


/**
 * Linked data service.
 *
 * <p>Services enable linked data applications to configure the {@linkplain _Server server} or other shared tools
 * managed by a tool {@linkplain Tray tray}; the most common use case is binding a custom resource {@linkplain Handler
 * handler} to the server {@linkplain Index index}.</p>
 *
 * <p>Custom services listed in the {@code com.metreeca.link.Service} {@linkplain ServiceLoader service loader}
 * provider configuration file in the {@code META-INF/services/} resource directory of an application will be
 * automatically {@linkplain #load() loaded} by server adapters.</p>
 */
@FunctionalInterface public interface Service {

	public void load();

}
