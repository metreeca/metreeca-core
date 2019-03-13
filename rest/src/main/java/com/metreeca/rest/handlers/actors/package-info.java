/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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
 * LDP action handlers.
 *
 * <p>Provides default handlers for CRUD actions on LDP <a href="https://www.w3.org/TR/ldp/#ldpr">resources</a> and <a
 * href="https://www.w3.org/TR/ldp/#ldpc">containers</a>.</p>
 *
 * <p>Request operating on containers select the operating {@linkplain com.metreeca.form.things.Shapes profile}
 * according to the {@code rdf:type} property of the target, as inferred either from {@linkplain
 * com.metreeca.form.shapes.Meta metadata} annotations or {@linkplain com.metreeca.form.shapes.Field field} constraints
 * in the {@linkplain com.metreeca.rest.Request#shape() shape} associated with the request, defaulting to the
 * <em>Basic</em> profile if no metadata is available:</p>
 *
 * @see <a href="https://www.w3.org/TR/ldp/">Linked Data Platform 1.0</a>
 */

package com.metreeca.rest.handlers.actors;
