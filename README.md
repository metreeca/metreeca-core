[![Maven Central](https://img.shields.io/maven-central/v/com.metreeca/metreeca-link.svg)](https://search.maven.org/artifact/com.metreeca/metreeca-link/)

# Metreeca/Link

Metreeca/Link is a model-driven Java framework for rapid REST/JSON-LD backend development.

Its engine automatically converts high-level declarative JSON-LD models into extended REST APIs supporting CRUD
operations, faceted search, data validation and fine‑grained role‑based access control, relieving backend developers from
low-level chores and completely shielding frontend developers from linked data technicalities.

Metreeca/Link is server and storage-agnostic and may be easily connected to your solution of choice.

# Getting Started

1. Add the framework to your Maven configuration

```xml
<project>

    <dependencyManagement>
        <dependencies>

            <dependency>
                <groupId>com.metreeca</groupId>
                <artifactId>metreeca-link</artifactId>
                <version>${link.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

        </dependencies>
    </dependencyManagement>

    <dependencies>

        <dependency> <!-- server connector -->
            <groupId>com.metreeca</groupId>
            <artifactId>metreeca-jse</artifactId>
        </dependency>

        <dependency> <!-- storage connector -->
            <groupId>com.metreeca</groupId>
            <artifactId>metreeca-rdf4j</artifactId>
        </dependency>

    </dependencies>

</project>
```

2. Write your first server and launch it

```java
import com.metreeca.jse.JSEServer;

import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.formats.TextFormat.text;

public final class Hello {

  public static void main(final String... args) {
    new JSEServer()

        .delegate(context -> request ->
            request.reply(response -> response
                .status(OK)
                .body(text(), "Hello world!")
            )
        )

        .start();
  }

}
```

3. Access you API

```shell
% curl -i http://localhost:8080/

HTTP/1.1 200 OK
Content-Type: text/plain
Content-Length: 12

Hello world!
```

4. Delve into the the [docs](https://metreeca.github.io/link/) to learn how to [publish](http://metreeca.github.io/link/tutorials/publishing-jsonld-apis) and [consume](https://metreeca.github.io/link/tutorials/consuming-jsonld-apis) your data as model-driven REST/JSON‑LD APIs…

# Modules

|    area | javadocs                                                     | description                             |
| ------: | :----------------------------------------------------------- | :-------------------------------------- |
|    core | [metreeca‑json](https://javadoc.io/doc/com.metreeca/metreeca-json) | shape-based JSON modelling framework    |
|         | [metreeca‑rest](https://javadoc.io/doc/com.metreeca/metreeca-rest) | model-driven REST publishing framework  |
|    data | [metreeca‑xml](https://javadoc.io/doc/com.metreeca/metreeca-xml) | XML/HTML codecs and utilities           |
|         | [metreeca‑rdf](https://javadoc.io/doc/com.metreeca/metreeca-rdf) | RDF codecs and utilities                |
|  server | [metreeca‑jse](https://javadoc.io/doc/com.metreeca/metreeca-jse) | Jave SE  HTTP server connector          |
|         | [metreeca‑jee](https://javadoc.io/doc/com.metreeca/metreeca-jee) | Servlet 3.1 containers connector        |
| storage | [metreeca‑rdf4j](https://javadoc.io/doc/com.metreeca/metreeca-rdf4j) | RDF4J-based SPARQL repository connector |

# Support

- open an [issue](https://github.com/metreeca/link/issues) to report a problem or to suggest a new feature
- start a [conversation](https://github.com/metreeca/link/discussions) to ask a how-to question or to share an open-ended
  idea

# License

This project is licensed under the Apache 2.0 License – see [LICENSE](LICENSE) file for details.