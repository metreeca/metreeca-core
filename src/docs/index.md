---
---

Metreeca/Link is a lightweight and server-agnostic Java framework enabling rapid development of model-driven REST/JSON APIs.

Its engine automatically converts high-level declarative JSON-LD models into extended REST APIs supporting CRUD operations, faceted search, data validation and fine‑grained role‑based access control, relieving back-end developers from low-level chores and completely shielding front‑end developers from linked data technicalities.

# Tutorials

- [Publishing Model‑Driven REST/JSON-LD APIs](tutorials/publishing-jsonld-apis.md)
- [Consuming Model‑Driven REST/JSON-LD APIs](tutorials/consuming-jsonld-apis.md)

# How To…

- [Alias Resources](how-to/alias-resources.md)

# References

- [Java API Reference](javadocs/index.html)
- [Shape Specification Language](references/spec-language.md)
- [REST Faceted Search](references/faceted-search.md)
- [Idiomatic JSON-LD Serialization](references/jsonld-format.md)

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

This project is licensed under the Apache 2.0 License – see [LICENSE](http://www.apache.org/licenses/LICENSE-2.0.txt) file for details.