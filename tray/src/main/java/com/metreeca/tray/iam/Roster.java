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

package com.metreeca.tray.iam;

import com.metreeca.form.Spec;
import com.metreeca.tray.iam.rosters.KeyRoster;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableSet;


/**
 * User roster.
 */
public interface Roster {

	public static Supplier<Roster> Factory=KeyRoster::new;


	public static long MinuteDuration=60*1000L;
	public static long HourDuration=60*MinuteDuration;
	public static long DayDuration=24*HourDuration;


	public static String CredentialsRejected="credentials-rejected"; // unknown user or invalid secret
	public static String CredentialsDisabled="credentials-disabled";
	public static String CredentialsPending="credentials-pending";

	public static String CredentialsExpired="credentials-expired";
	public static String CredentialsIllegal="credentials-illegal"; // secret unacceptable by policy
	public static String CredentialsLocked="credentials-locked"; // account locked by system


	public static Permit.Builder permit() { return new Permit().new Builder(); }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Permit profile(final String alias);


	public Permit acquire(final String alias, final String secret);

	public Permit acquire(final String alias, final String secret, final String update);


	public Permit refresh(final String alias);

	public Permit release(final String alias);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final class Permit {

		private long expiry;

		private IRI user=Spec.none;
		private Set<Value> roles=singleton(Spec.none);

		private String alias="";
		private String label="";

		private String token="";
		private String issue="";


		private Permit() {}


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

}
