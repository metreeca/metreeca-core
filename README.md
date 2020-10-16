[![Maven Central](https://img.shields.io/maven-central/v/com.metreeca/metreeca-link.svg)](https://search.maven.org/artifact/com.metreeca/metreeca-link/)

# Metreeca/Link

Metreeca/Link is a lightweight and server-agnostic Java framework enabling rapid development of model-driven REST/JSON APIs.

Its engine automatically converts high-level declarative JSON-LD models into extended REST APIs supporting CRUD operations, faceted search, data validation and fine‑grained role‑based access control, relieving back-end developers from low-level chores and completely shielding front‑end developers from linked data technicalities.

# Getting started

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

        <dependency> <!-- server adapter -->
            <groupId>com.metreeca</groupId>
            <artifactId>metreeca-jse</artifactId>
        </dependency>

        <dependency> <!-- backend adapter -->
            <groupId>com.metreeca</groupId>
            <artifactId>metreeca-rdf4j</artifactId>
        </dependency>

    </dependencies>

</project>
```

2. Write your first server and launch it

```java
import com.metreeca.jse.Server;

import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.formats.TextFormat.text;

public final class Hello {

  public static void main(final String... args) {
    new Server()

        .handler(context -> request ->
            request.reply(response -> response
                .status(OK)
                .body(text(), "Hello world!")
            ))

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

|         |                                                              |                                        |
| ------: | ------------------------------------------------------------ | -------------------------------------- |
|    core | [metreeca‑json](https://javadoc.io/doc/com.metreeca/metreeca-json) | shape-based JSON modelling framework   |
|         | [metreeca‑rest](https://javadoc.io/doc/com.metreeca/metreeca-rest) | model-driven REST publishing framework |
|    data | [metreeca‑xml](https://javadoc.io/doc/com.metreeca/metreeca-xml) | XML/HTML codecs and utilities          |
|         | [metreeca‑rdf](https://javadoc.io/doc/com.metreeca/metreeca-rdf) | RDF codecs and utilities               |
|  server | [metreeca‑jse](https://javadoc.io/doc/com.metreeca/metreeca-jse) | Jave SE  HTTP server                   |
|         | [metreeca‑jee](https://javadoc.io/doc/com.metreeca/metreeca-jee) | Servlet 3.1 containers                 |
| storage | [metreeca‑rdf4j](https://javadoc.io/doc/com.metreeca/metreeca-rdf4j) | RDF4J-based SPARQL repositories        |

# Support

- open an [issue](https://github.com/metreeca/link/issues) to report a problem or to suggest a new feature
- post to [Stack Overflow](https://stackoverflow.com/questions/ask?tags=metreeca) using the `metreeca` tag to ask how-to questions
- post to [groups.google.com/d/forum/metreeca](https://groups.google.com/d/forum/metreeca) to start open-ended discussions

# License

This project is licensed under the Apache 2.0 License – see [LICENSE](LICENSE) file for details.