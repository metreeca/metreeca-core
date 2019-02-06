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

package com.metreeca.rest;

import com.metreeca.form.Focus;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import java.util.Collection;
import java.util.Optional;


/**
 * Resource engine {thread-safe}.
 *
 * <p>Manages CRUD operations on linked data resources.</p>
 *
 * <p><strong>Warning</strong> / Implementations must be thread-safe.</p>
 */
public interface Engine {

	/**
	 * Retrieves a resource.
	 *
	 * @param resource the IRI identifying the resource whose description is to be retrieved
	 *
	 * @return the optional description of {@code resource}; empty if a description for {@code resource} was not found
	 *
	 * @throws NullPointerException          if {@code resource} is null
	 * @throws UnsupportedOperationException if resource retrieval is not supported by this engine
	 */
	public Optional<Collection<Statement>> relate(final IRI resource);

	/**
	 * Creates a related resource.
	 *
	 * @param resource the IRI identifying the owning resource for the related resource to be created
	 * @param related  the IRI to be assigned to the new related resource
	 * @param model    the description for the new related resource owned by {@code resource}; must describe the related
	 *                 resource using {@code related} as subject
	 *
	 * @return an optional validation report for the operation; empty a description for {@code resource} is already
	 * present
	 *
	 * @throws NullPointerException          if any argument is null or if {@code model} contains null values
	 * @throws UnsupportedOperationException if resource creation is not supported by this engine
	 */
	public Optional<Focus> create(final IRI resource, final IRI related, final Collection<Statement> model);

	/**
	 * Updates a resource.
	 *
	 * @param resource the IRI identifying the resource whose description is to be updated
	 * @param model    the updated description for {@code resource}
	 *
	 * @return an optional validation report for the operation; empty if a description for {@code resource} was not
	 * found
	 *
	 * @throws NullPointerException          if either {@code resource} or {@code model} is null or if {@code model}
	 *                                       contains null values
	 * @throws UnsupportedOperationException if resource updating is not supported by this engine
	 */
	public Optional<Focus> update(final IRI resource, final Collection<Statement> model);

	/**
	 * Deletes a resource.
	 *
	 * @param resource the IRI identifying the resource whose description is to be deleted
	 *
	 * @return an optional IRI identifying the deleted resource; empty if a description for {@code resource} was not
	 * found
	 *
	 * @throws NullPointerException          if {@code resource} is {@code null}
	 * @throws UnsupportedOperationException if resource deletion is not supported by this engine
	 */
	public Optional<IRI> delete(final IRI resource);

}
