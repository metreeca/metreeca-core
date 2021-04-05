---
title:      How To Secure Resources
---

# SPARQL Endpoints

```shell
% curl --include localhost:8080/sparql \
	--header "Authorization: Bearer 86fc8ad0-19a0-4df4-a3c9-819f04d671d4" \
	--data-urlencode 'update=insert data { <http://www.example.org/X> <http://www.example.org/Y> <http://www.example.org/Q> .}'
```

```java

import com.metreeca.jse.JSEServer;
import com.metreeca.rdf4j.assets.Graph;

import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;

import java.io.File;

import static com.metreeca.json.Values.uuid;
import static com.metreeca.rdf4j.assets.Graph.graph;
import static com.metreeca.rdf4j.handlers.Graphs.graphs;
import static com.metreeca.rdf4j.handlers.SPARQL.sparql;
import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.assets.Logger.logger;
import static com.metreeca.rest.assets.Vault.vault;
import static com.metreeca.rest.handlers.Router.router;
import static com.metreeca.rest.wrappers.Bearer.bearer;
import static com.metreeca.rest.wrappers.CORS.cors;
import static com.metreeca.rest.wrappers.Gateway.gateway;

import static java.lang.String.format;

public final class Sample {

	private static final File StoragePath=new File("target/data");

	private static final String UpdateRole="Update";
	private static final String UpdateKey=uuid();


	private static String key() {

		final String key=asset(vault()).get("UpdateKey").orElse(UpdateKey);

		asset(logger()).info(Sample.class, format("update key <%s>", UpdateKey));

		return key;
	}


	public static void main(final String... args) {
		new JSEServer()

				.delegate(context -> context

						.set(graph(), () -> new Graph(new SailRepository(new NativeStore(StoragePath))))

						.get(() -> server()

								.with(cors())

								.with(bearer(key(), UpdateRole))

								.wrap(router()

										.path("/sparql", sparql()
												.query()
												.update(UpdateRole)
										)

										.path("/graphs", graphs()
												.query()
												.update(UpdateRole)
										)

								)
						)
				)

				.start();
	}

}
```