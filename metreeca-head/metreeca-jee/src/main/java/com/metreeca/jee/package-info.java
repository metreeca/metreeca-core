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

/**
 * Java EE adapter.
 *
 * <p>Provides an adapter for deploying apps based on Metreeca/Link as web applications managed by a Servlet 3.1
 * container.</p>
 *
 * <p>To deploy a linked data app as a web application, package it as a {@code war} archive adding:</p>
 *
 * <ul>
 *
 * <li>a runtime dependency from the Java EE module and other required framework components;</li>
 *
 * <li>a provided dependency from the Servlet 3.1 API, unless you want to manually define the app as a web context
 * listener in {@code WEB-INF/web.xml}.</li>
 *
 * </ul>
 *
 * <p>Using maven:</p>
 *
 * <pre>{@code <dependencies>
 *
 *     <dependency>
 *         <groupId>com.metreeca</groupId>
 *         <artifactId>metreeca-jee</artifactId>
 *         <version>${project.version}</version>
 *     </dependency>
 *
 *     <dependency>
 *         <groupId>javax.servlet</groupId>
 *         <artifactId>javax.servlet-api</artifactId>
 *         <version>3.1.0</version>
 *         <scope>provided</scope>
 *     </dependency>
 *
 * </dependencies> }</pre>
 *
 * <p>Then define the app entry point as:</p>
 *
 * <pre>{@code package com.metreeca.demo;
 *
 * import com.metreeca.rest.Context;
 * import com.metreeca.rest.Handler;
 * import com.metreeca.rest.Wrapper;
 * import com.metreeca.rest.wrappers.Gateway;
 * import com.metreeca.jee.JEEFilter;
 *
 * import javax.servlet.annotation.WebFilter;
 *
 * import static com.metreeca.rest.Context.asset;
 *
 * ＠WebFilter("/*") public final class Demo extends JEEFilter { // define the path pattern managed by the app
 *
 * ＠Override protected Handler load(final Context context) {
 *      return context
 *
 *          .set(Asset.factory(), () -> { return new AssetReplacement(); } // customize shared assets
 *
 *          .exec(() -> { asset(Asset.factory()).…; }) // initialize the app using shared assets
 *
 *          .get(() -> new Gateway()
 *
 *               .with(new Wrapper() { … }) // configure system-wide wrappers
 *
 *               .wrap(new Handler() { … }) // configure the app main handler
 *
 *          );
 *     }
 *
 * } }</pre>
 *
 * <p>Standard wrappers and handler building blocks are provided by the {@link com.metreeca.rest.wrappers} and {@link
 * com.metreeca.rest.handlers} packages.</p>
 */

package com.metreeca.jee;
