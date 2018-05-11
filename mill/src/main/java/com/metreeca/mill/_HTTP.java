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

package com.metreeca.mill;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;


public final class _HTTP { // !!! merge into Store to support POST/Query HTTP requests

	private final String url;

	private String method="GET";

	private final Map<String, String> parameters=new LinkedHashMap<>();
	private final Map<String, String> headers=new LinkedHashMap<>();


	public _HTTP(final String url) {

		if ( url == null ) {
			throw new NullPointerException("null url");
		}

		this.url=url;
	}


	public _HTTP method(final String method) {

		if ( method == null ) {
			throw new NullPointerException("null method");
		}

		this.method=method.toUpperCase(Locale.ROOT);

		return this;
	}

	public _HTTP parameter(final String name, final String value) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		parameters.put(name, value); // !!! append if already defined

		return this;
	}

	public _HTTP header(final String name, final String value) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		headers.put(name, value); // !!! append if already defined

		return this;
	}


	//@Override public Stream<Feed> process(final Stream<? extends Feed> feeds, final Tray tray) {
	//	return feeds.map(feed -> {
	//		try {
	//
	//			final StringBuilder query=new StringBuilder(parameters.size()*25);
	//
	//			for (final Map.Entry<String, String> parameter : parameters.entrySet()) {
	//
	//				final String name=parameter.getKey();
	//				final String value=parameter.getValue();
	//
	//				query
	//						.append(query.length() == 0 ? '?' : '&')
	//						.append(URLEncoder.encode(name, "UTF-8"))
	//						.append(value.isEmpty() ? "" : "=")
	//						.append(URLEncoder.encode(value, "UTF-8"));
	//			}
	//
	//			final HttpURLConnection connection=(HttpURLConnection)new URL(url+query).openConnection();
	//
	//			connection.setDoInput(true);
	//			connection.setDoOutput(true);
	//
	//			connection.setRequestMethod(method);
	//
	//			for (final Map.Entry<String, String> header : headers.entrySet()) {
	//				connection.setRequestProperty(header.getKey(), header.getValue());
	//			}
	//
	//			connection.connect();
	//
	//			final long time=time(() -> {
	//				try (
	//						final InputStream input=feed.input();
	//						final OutputStream output=connection.getOutputStream()
	//				) {
	//					data(output, input);
	//				} catch ( final IOException e ) {
	//					throw new UncheckedIOException(e);
	//				}
	//			});
	//
	//
	//			final int code=connection.getResponseCode();
	//			final String message=connection.getResponseMessage();
	//
	//			if ( code/100 == 2 ) {
	//
	//				try (final InputStream response=connection.getInputStream()) {
	//
	//					monitor.info("got response '%d %s' in %,d ms", code, message, time);
	//
	//					return new DataItem(data(response));
	//
	//				}
	//
	//			} else {
	//
	//				try (final InputStream response=connection.getErrorStream()) {
	//
	//					monitor.error("response '%d %s' [%s]", code, message, text(reader(response)));
	//
	//					throw new UncheckedIOException(
	//							new IOException(String.format("failed HTTP request [%d %s]", code, message)));
	//
	//				}
	//
	//			}
	//
	//		} catch ( final IOException e ) {
	//			throw new UncheckedIOException(e);
	//		}
	//
	//	});
	//}

}
