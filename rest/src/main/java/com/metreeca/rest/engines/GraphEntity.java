/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.rest.engines;

import com.metreeca.form.Focus;
import com.metreeca.form.Issue;
import com.metreeca.rest.Engine;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.Collection;
import java.util.Optional;

import static com.metreeca.form.Focus.focus;
import static com.metreeca.form.Issue.issue;
import static com.metreeca.rest.engines.Descriptions.description;

import static java.util.stream.Collectors.toList;


abstract class GraphEntity implements Engine { // !!! remove

	Optional<Resource> reserve(final RepositoryConnection connection, final Resource resource) {
		return Optional.ofNullable(connection.hasStatement(resource, null, null, true)
				|| connection.hasStatement(null, null, resource, true) ? null : resource);
	}


	Focus validate(final Resource resource, final Collection<Statement> model) {

		final Collection<Statement> envelope=description(resource, false, model);

		return focus(model.stream()
				.filter(statement -> !envelope.contains(statement))
				.map(outlier -> issue(Issue.Level.Error, "statement outside description envelope "+outlier))
				.collect(toList())
		);
	}

}
