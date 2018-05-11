

/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.jeep.rdf;


import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;


/**
 * Constants for <a href="https://www.w3.org/TR/ldp/">Linked Data Platform 1.0</a>.
 *
 * @see <a href="https://www.w3.org/TR/ldp/">Linked Data Platform 1.0</a>
 */
public final class LDP {

	// !!! to be replaced with native implementation as soon as https://github.com/eclipse/rdf4j/issues/326 is resolved

	private LDP() {} // a utility class


	/**
	 * The LDP namespace: http://www.w3.org/ns/ldp#
	 */
	public static final String NAMESPACE="http://www.w3.org/ns/ldp#";

	/**
	 * Recommended prefix for the LDP namespace: "ldp"
	 */
	public static final String PREFIX="ldp";

	/**
	 * An immutable {@link Namespace} constant that represents the LDP namespace.
	 */
	public static final Namespace NS=new SimpleNamespace(PREFIX, NAMESPACE);


	private static IRI term(final String name) {
		return SimpleValueFactory.getInstance().createIRI(NAMESPACE, name);
	}


	/**
	 * http://www.w3.org/ns/ldp#Resource
	 */
	public static final IRI RESOURCE=term("Resource");

	/**
	 * http://www.w3.org/ns/ldp#member
	 */
	public static final IRI MEMBER=term("member");

	/**
	 * http://www.w3.org/ns/ldp#constrainedBy
	 */
	public static final IRI CONSTRAINEDBY=term("constrainedBy");

	/**
	 * http://www.w3.org/ns/ldp#RDFSource
	 */
	public static final IRI RDFSOURCE=term("RDFSource");

	/**
	 * http://www.w3.org/ns/ldp#Container
	 */
	public static final IRI CONTAINER=term("Container");

	/**
	 * http://www.w3.org/ns/ldp#hasMemberRelation
	 */
	public static final IRI HASMEMBERRELATION=term("hasMemberRelation");

	/**
	 * http://www.w3.org/ns/ldp#isMemberOfRelation
	 */
	public static final IRI ISMEMBEROFRELATION=term("isMemberOfRelation");

	/**
	 * http://www.w3.org/ns/ldp#membershipResource
	 */
	public static final IRI MEMBERSHIPRESOURCE=term("membershipResource");

	/**
	 * http://www.w3.org/ns/ldp#insertedContentRelation
	 */
	public static final IRI INSERTEDCONTENTRELATION=term("insertedContentRelation");

	/**
	 * http://www.w3.org/ns/ldp#contains
	 */
	public static final IRI CONTAINS=term("contains");

	/**
	 * http://www.w3.org/ns/ldp#BasicContainer
	 */
	public static final IRI BASICCONTAINER=term("BasicContainer");

	/**
	 * http://www.w3.org/ns/ldp#DirectContainer
	 */
	public static final IRI DIRECTCONTAINER=term("DirectContainer");

	/**
	 * http://www.w3.org/ns/ldp#IndirectContainer
	 */
	public static final IRI INDIRECTCONTAINER=term("IndirectContainer");

	/**
	 * http://www.w3.org/ns/ldp#NonRDFSource
	 */
	public static final IRI NONRDFSOURCE=term("NonRDFSource");

	/**
	 * http://www.w3.org/ns/ldp#MemberSubject
	 */
	public static final IRI MEMBERSUBJECT=term("MemberSubject");

	/**
	 * http://www.w3.org/ns/ldp#PreferContainment
	 */
	public static final IRI PREFERCONTAINMENT=term("PreferContainment");

	/**
	 * http://www.w3.org/ns/ldp#PreferMembership
	 */
	public static final IRI PREFERMEMBERSHIP=term("PreferMembership");

	/**
	 * http://www.w3.org/ns/ldp#PreferEmptyContainer
	 *
	 * @deprecated use {@link #PREFERMINIMALCONTAINER}
	 */
	public static final IRI PREFEREMPTYCONTAINER=term("PreferEmptyContainer");

	/**
	 * http://www.w3.org/ns/ldp#PreferMinimalContainer
	 */
	public static final IRI PREFERMINIMALCONTAINER=term("PreferMinimalContainer");

	/**
	 * http://www.w3.org/ns/ldp#Page
	 */
	public static final IRI PAGE=term("Page");

	/**
	 * http://www.w3.org/ns/ldp#pageSortCriteria
	 */
	public static final IRI PAGESORTCRITERIA=term("pageSortCriteria");

	/**
	 * http://www.w3.org/ns/ldp#PageSortCriterion
	 */
	public static final IRI PAGESORTCRITERION=term("PageSortCriterion");

	/**
	 * http://www.w3.org/ns/ldp#pageSortPredicate
	 */
	public static final IRI PAGESORTPREDICATE=term("pageSortPredicate");

	/**
	 * http://www.w3.org/ns/ldp#pageSortOrder
	 */
	public static final IRI PAGESORTORDER=term("pageSortOrder");

	/**
	 * http://www.w3.org/ns/ldp#Ascending
	 */
	public static final IRI ASCENDING=term("Ascending");

	/**
	 * http://www.w3.org/ns/ldp#Descending
	 */
	public static final IRI DESCENDING=term("Descending");

	/**
	 * http://www.w3.org/ns/ldp#pageSortCollation
	 */
	public static final IRI PAGESORTCOLLATION=term("pageSortCollation");

	/**
	 * http://www.w3.org/ns/ldp#pageSequence
	 */
	public static final IRI PAGESEQUENCE=term("pageSequence");

}
