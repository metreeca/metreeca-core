package com.metreeca.rest.wrappers;

import com.metreeca.rest.*;
import com.metreeca.rest.formats._Failure;
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.sys.Trace;

import java.util.regex.Pattern;

import static com.metreeca.tray.Tray.tool;

import static java.lang.String.format;


/**
 * Linked data server.
 *
 * <p>Provides default resource pre/post-processing and error handling; mainly intended as the outermost wrapper
 * returned by gateway loaders.</p>
 */
public final class Server implements Wrapper {

	private static final Pattern CharsetPattern=Pattern.compile(";\\s*charset\\s*=.*$");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Graph graph=tool(Graph.Factory);
	private final Trace trace=tool(Trace.Factory);


	@Override public Handler wrap(final Handler handler) {

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		return request -> graph.query(connection -> { // process the request on a single connection
			try {

				return consumer -> request
						.map(this::preprocessor)
						.map(handler::handle)
						.map(this::postprocessor)
						.accept(consumer);

			} catch ( final RuntimeException e ) {

				trace.error(this, format("%s %s > internal error", request.method(), request.item()), e);

				return request.reply(response -> response.cause(e).body(_Failure.Format, new Failure(Response.InternalServerError,
						"exception-untrapped",
						"unable to process request: see server logs for details"
				)));

			}
		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Request preprocessor(final Request request) {
		return request;
	}

	private Response postprocessor(final Response response) {

		final Request request=response.request();
		final int status=response.status();
		final Throwable cause=response.cause().orElse(null);

		// log response outcome

		trace.entry(status < 400 ? Trace.Level.Info : status < 500 ? Trace.Level.Warning : Trace.Level.Error,
				this, format("%s %s > %d", request.method(), request.item(), status), cause);

		// if no charset is specified, add a default one to prevent the container from adding its own…

		response.header("Content-Type")
				.filter(type -> !CharsetPattern.matcher(type).find())
				.filter(type -> type.startsWith("text/") || type.equals("application/json"))
				.ifPresent(type -> response.header("Content-Type", type+";charset=UTF-8"));

		return response;

	}

}
