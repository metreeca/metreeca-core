/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.gate;

import com.metreeca.form.Form;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableSet;


public final class Permit {

	public static Builder permit() { return new Permit().new Builder(); }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private long expiry;

	private IRI user=Form.none;
	private Set<Value> roles=singleton(Form.none);

	private String alias="";
	private String label="";

	private String token="";
	private String issue="";


	Permit() {}


	public boolean valid() {
		return issue.isEmpty();
	}

	public boolean valid(final long time) {
		return valid() && expiry >= time;
	}


	public long expiry() {
		return expiry;
	}


	public IRI user() {
		return user;
	}

	public Set<Value> roles() {
		return unmodifiableSet(roles);
	}


	public String alias() {
		return alias;
	}

	public String label() {
		return label;
	}


	public String token() {
		return token;
	}

	public String issue() {
		return issue;
	}


	public final class Builder {

		private Builder() {}


		public Builder expiry(final long expiry) {

			if ( expiry < 0 ) {
				throw new IllegalArgumentException("negative expiry");
			}

			Permit.this.expiry=expiry;

			return this;
		}


		public Builder user(final IRI user) {

			if ( user == null ) {
				throw new NullPointerException("null user");
			}

			Permit.this.user=user;

			return this;
		}

		public Builder roles(final Value... roles) {

			if ( roles == null ) {
				throw new NullPointerException("null roles");
			}

			return roles(asList(roles));
		}

		public Builder roles(final Collection<Value> roles) {

			if ( roles == null ) {
				throw new NullPointerException("null roles");
			}

			if ( roles.contains(null) ) {
				throw new NullPointerException("null role");
			}

			Permit.this.roles=new LinkedHashSet<>(roles);

			return this;
		}


		public Builder alias(final String alias) {

			if ( alias == null ) {
				throw new NullPointerException("null alias");
			}

			Permit.this.alias=alias;

			return this;
		}

		public Builder label(final String label) {

			if ( label == null ) {
				throw new NullPointerException("null label");
			}

			Permit.this.label=label;

			return this;
		}


		public Builder token(final String token) {

			if ( token == null ) {
				throw new NullPointerException("null token");
			}

			Permit.this.token=token;

			return this;
		}

		public Builder issue(final String issue) {

			if ( issue == null ) {
				throw new NullPointerException("null issue");
			}

			Permit.this.issue=issue;

			return this;
		}


		public Permit done() {
			return Permit.this;
		}

	}

}
