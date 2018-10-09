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

package com.metreeca.gate.rosters;

// Legacy key-based authentication (used by data and demo apps)
// Salvage key handling and logout to KeyRoster


public final class BasicRoster {

	//private static final String SysAdm="admin"; // !!! configurable?
	//
	//private static final Pattern BasicPattern=Pattern.compile("(?i:Basic\\s+(?<token>.*)\\s*)");
	//
	//
	//private final String key;
	//
	//
	//public BasicRoster() {
	//
	//
	//	final Setup setup=tool(Setup.Factory);
	//
	//	this.key=setup.get(Setup.KeyProperty, "");
	//}
	//
	//
	////@Override public void authorize(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink) {
	////
	////	if ( response.getStatus() != 0 ) { sink.accept(request, response); } else {
	////		final String authorization=request.getHeader("Authorization").orElse("");
	////
	////		final Matcher matcher=BasicPattern.matcher(authorization);
	////
	////		if ( matcher.matches() ) {
	////
	////			final String token=new String(Base64.getDecoder()
	////					.decode(matcher.group("token")), Codecs.UTF8);
	////
	////			final int colon=max(token.indexOf(':'), 0);
	////
	////			final String usr=token.isEmpty() ? token : token.substring(0, colon);
	////			final String pwd=token.isEmpty() ? token : token.substring(colon+1);
	////
	////			if ( usr.equals(SysAdm) && !key.isEmpty() && pwd.equals(key) ) {
	////				request.setUser(iri(request.getBase())).setRoles(singleton(Form.root)); // !!! review usr name/iri
	////			} else if ( !usr.equals("-") || !pwd.equals("-") ) { // ignore fake sign out credentials
	////				response.setStatus(_Response.Unauthorized);
	////			}
	////
	////		} else if ( !authorization.isEmpty() ) {
	////			response.setStatus(_Response.Unauthorized).setText("unsupported authorization method");
	////		}
	////
	////		sink.accept(request, response);
	////	}
	////}
	//
	////@Override public void authenticate(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink) {
	////
	////	if ( response.getStatus() == _Response.Unauthorized ) {
	////
	////		sink.accept(request, response.addHeader("WWW-Authenticate", "Basic realm=\"metreeca\""));
	////
	////	} else {
	////
	////		sink.accept(request, response);
	////
	////	}
	////}

}
