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

package com.metreeca.link;


public final class LinkException extends RuntimeException {

	private static final long serialVersionUID=-7447354772817414205L;


	private final int status;
	private final String report;


	public LinkException(final int status) {
		this(status, "", null);
	}

	public LinkException(final int status, final String report) {
		this(status, report, null);
	}

	public LinkException(final int status, final String report, final Throwable cause) {

		super(status+" "+report, cause);

		if ( status < 0 || status > 599 ) {
			throw new IllegalArgumentException("illegal status ["+status+"]");
		}

		if ( report == null ) {
			throw new NullPointerException("null report");
		}

		this.status=status;
		this.report=report;

	}


	public int getStatus() {
		return status;
	}

	public String getReport() {
		return report;
	}

}
