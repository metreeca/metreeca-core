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
 * Entity description manager.
 *
 * <p>Manages CRUD lifecycle operations on entity descriptions.</p>
 */
public interface Flavor {

	/**
	 * Retrieves an entity description.
	 *
	 * @param entity the entity whose description is to be retrieved
	 *
	 * @return the optional description of {@code entity}; empty if {@code entity} is not known
	 *
	 * @throws NullPointerException          if {@code entity} is null
	 * @throws UnsupportedOperationException if description retrieval is not supported by this entity flavor
	 */
	public Optional<Collection<Statement>> relate(final IRI entity) throws UnsupportedOperationException;

	/**
	 * Creates a connected entity description.
	 *
	 * @param entity the owning entity for the connected entity to be created
	 * @param model  the description for the newly created connected entity owned by {@code entity}
	 *
	 * @return a validation report for the operation
	 *
	 * @throws NullPointerException          if either {@code entity} or {@code model} is null or if {@code model}
	 *                                       contains null values
	 * @throws UnsupportedOperationException if description creation is not supported by this entity flavor
	 */
	public Focus create(final IRI entity, final Collection<Statement> model) throws UnsupportedOperationException;

	/**
	 * Updates an entity description.
	 *
	 * @param entity the entity whose description is to be updated
	 * @param model  the updated description for {@code entity}
	 *
	 * @return a validation report for the operation
	 *
	 * @throws NullPointerException          if either {@code entity} or {@code model} is null or if {@code model}
	 *                                       contains null values
	 * @throws UnsupportedOperationException if description updating is not supported by this entity flavor
	 */
	public Focus update(final IRI entity, final Collection<Statement> model) throws UnsupportedOperationException;

	/**
	 * Deletes a resource description.
	 *
	 * @param entity the focus entity for the description to be deleted
	 *
	 * @return {@code true} if a description for {@code entity} was present and actually deleted; {@code false} otherwise
	 *
	 * @throws NullPointerException          if {@code entity} is {@code null}
	 * @throws UnsupportedOperationException if description deletion is not supported by this entity flavor
	 */
	public boolean delete(final IRI entity) throws UnsupportedOperationException;

}
