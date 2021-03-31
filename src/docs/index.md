---
---

Metreeca/Link is a model-driven Java framework for rapid REST/JSON-LD backend development.

Its engine automatically converts high-level declarative JSON-LD models into extended REST APIs supporting CRUD
operations, faceted search, data validation and fine‑grained role‑based access control, relieving backend developers from
low-level chores and completely shielding frontend developers from linked data technicalities.

Metreeca/Link is server and storage-agnostic and may be easily connected to your solution of choice.

# Tutorials

- [Publishing Model‑Driven REST/JSON-LD APIs](tutorials/publishing-jsonld-apis.md)
- [Consuming Model‑Driven REST/JSON-LD APIs](tutorials/consuming-jsonld-apis.md)

# How To…

- [Alias Resources](how-to/alias-resources.md)

# References

- [Shape Specification Language](references/spec-language.md)
- [Idiomatic JSON-LD Serialization](references/jsonld-format.md)
- [REST Faceted Search](references/faceted-search.md)

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

This project is licensed under the Apache 2.0 License – see [LICENSE](http://www.apache.org/licenses/LICENSE-2.0.txt) file for details.