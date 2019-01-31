---
title:      Shape Specification Language Reference
excerpt:    Linked data shape specification language semantics and components
---

Most framework services are driven by declarative linked data models defined using a [SHACL](https://www.w3.org/TR/shacl/)‑based [shape](#shapes) specification language.

Models are [programmaticaly](../tutorials/linked-data-publishing.md) built using a Java‑based DSL and can eventually automate a range of different tasks in the lifecycle of linked data REST APIs.

| task                | shape role                                                   |
| ------------------- | ------------------------------------------------------------ |
| data selection      | RDF data retrieved from [readable](../tutorials/linked-data-interaction.md#read-operations) linked data REST APIs is selected from the underlying graph storage layer by SPARQL queries derived from the associated linked data models |
| data validation     | RDF data submitted to [writable](../tutorials/linked-data-interaction.md#write-operations) linked data REST APIs on resource creation and updating is automatically validated against the associated linked data models |
| faceted search      | [faceted search](../tutorials/linked-data-interaction.md#faceted-search) and ancillary facet-populating queries are managed by the system on the basis of structural and typing constraints specified in linked data models associated with target linked data REST APIs |
| dynamic UI building | Interactive linked data navigators and editing forms could be dynamically created and laid out according to the linked data models associated with linked data REST APIs |
| API documentation   | Human and machine readable docs for linked data REST APIs could be dynamically derived from associated linked data models and published as hypertexts and [OpenAPI](https://www.openapis.org)/[Swagger](https://swagger.io/specification/) specs |
| data ingestion      | RDF data ingested from external data sources could be dynamically mapped to and validated against the linked data models associated with target RDF graphs |

<p class="warning">The details of the specification language are still in flux, especially when it comes to SHACL interoperability.</p>

<p class="warning">Some of the described features aren't (yet;-) supported by the framework.</p>

# Shapes

Linked data [shapes](../javadocs/com/metreeca/form/Shape.html) define the expected structure of RDF resource descriptions in terms of **constraints** imposed on members of a **focus set** of RDF terms and on the  values of their RDF properties.

**Primitive** shapes specify declarative constraints to be meet by the focus set and its member terms. **Composite** shapes recursively assemble other shapes into tree-structured complex constraints.

Linked data selection tasks identify the set of subjects in an RDF graph whose RDF descriptions are consistent with a possibly composite shape. Selection results are reported either as an RDF graph or a structured report according to the choosen [query](#queries) type.

Linked data validation tasks verify that the RDF description of an initial focus set is consistent with the constraints specified by a possibly composite shape. During the process, derived focus sets connected by [structural constraints](#structural-constraints) may be recursively validated. Validation results are reported as structured [focus validation report](../javadocs/com/metreeca/form/Focus.html).

## Annotations

Non-validating shapes documenting presentation-driving metadata, to be used for purposes such as form building or predictable printing of RDF files.

| shape                                                        | value                                                        |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| [meta](../javadocs/com/metreeca/form/shapes/Meta.html)([IRI](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/model/IRI.html), [value](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/model/Value.html)) | the metadata property identified by the given IRI is associated with the given value in the enclosing shape |
| [guard](../javadocs/com/metreeca/form/shapes/Guard.html)([axis](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/model/IRI.html), [value](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/model/Value.html), …) | the focus set is consistent with this shape only if the value of an externally assigned [axis variable](../javadocs/com/metreeca/form/probes/Redactor.html) is included in a given set of target values |

Common metadada annotations are directly available as shorthand shapes.

| shorthand shape                                              | value                                                        |
| :----------------------------------------------------------- | :----------------------------------------------------------- |
| [alias](../javadocs/com/metreeca/form/Meta.html#alias)("alias") | an alternate property name for reporting values for the enclosing shape (e.g. in the [context](idiomatic-json.md#term-properties) of JSON-based RDF serialization results) |
| [label](../javadocs/com/metreeca/form/shapes/Meta.html#label)("label") | a human-readable textual label  for the enclosing shape      |
| [notes](../javadocs/com/metreeca/form/shapes/Meta.html#notes)("notes") | a human-readable textual description of the enclosing shape  |
| [placeholder](../javadocs/com/metreeca/form/shapes/Meta.html#placeholder)("placeholder") | a human-readable textual placeholder for the values of the enclosing shape |
| [default](../javadocs/com/metreeca/form/shapes/Meta.html#default)([value](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/model/Value.html)) | the default for the expected values of the enclosing shape   |
| [hint](../javadocs/com/metreeca/form/shapes/shapes/Meta#hint)([IRI](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/model/IRI.html)) | the IRI of a resource hinting at possible values for the enclosing shape (e.g. an LDP container) |
| [group](../javadocs/com/metreeca/form/shapes/Meta.html#group)([value](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/model/Value.html)) | a client-specific group visualization mode for the enclosing shape |

# Term Constraints

Primitive shapes specifying constraints to be individually met by each RDF term in the focus set.

| shape                                                        | constraint                                                   |
| :----------------------------------------------------------- | :----------------------------------------------------------- |
| [datatype](../javadocs/com/metreeca/form/shapes/Datatype.html)([IRI](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/model/IRI.html)) | each term in the focus set has a given extended RDF datatype IRI; IRI references and blank nodes are considered to be respectively of `http://www.openrdf.org/schema/sesame#iri` ([Values.IRI](../javadocs/com/metreeca/form/things/Values.html#IRIType)) and `http://www.openrdf.org/schema/sesame#bnode` ([Values.BNode](../javadocs/com/metreeca/form/things/Values.html#BNodeType)) datatype |
| [class](../javadocs/com/metreeca/form/shapes/Clazz.html)([IRI](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/model/IRI.html)) | each term in the focus set is an instance of a given RDF class or one of its superclasses |
| [minExclusive](../javadocs/com/metreeca/form/shapes/MinExclusive.html)([value](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/model/Value.html)) | each term in the focus set is strictly greater than a given minum value, according to <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#modOrderBy">SPARQL ordering</a> rules |
| [maxExclusive](../javadocs/com/metreeca/form/shapes/MaxExclusive.html)([value](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/model/Value.html)) | each term in the focus set is strictly less than a given maximum value, according to <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#modOrderBy">SPARQL ordering</a> rules |
| [minInclusive](../javadocs/com/metreeca/form/shapes/MinInclusive.html)([value](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/model/Value.html)) | each term in the focus set is greater than or equal to a given minimum value, according to <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#modOrderBy">SPARQL ordering</a> rules |
| [maxInclusive](../javadocs/com/metreeca/form/shapes/MaxInclusive.html)([value](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/model/Value.html)) | each term in the focus set is less than or equal to a given maximum value, according to <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#modOrderBy">SPARQL ordering</a> rules |
| [minLength](../javadocs/com/metreeca/form/shapes/MinLength.html)(length) | the length of the lexical representation of each term in the focus set is greater than or equal to the given minimum value |
| [maxLength](../javadocs/com/metreeca/form/shapes/MaxLength.html)(length) | the length of the lexical representation of each term in the focus set is less than or equal to the given maximum value |
| [pattern](../javadocs/com/metreeca/form/shapes/Pattern.html)("pattern") | the lexical representation of each term in the focus set matches a given [regular expression](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#sum) pattern |
| [like](../javadocs/com/metreeca/form/shapes/Like.html)("keywords") | the lexical representation of each term in the focus set matches the given full-text keywords |

## Set Constraints

Primitive shapes specifying constraints to be collectively met by the RDF terms in the focus set.

| shape                                    | constraint                               |
| :--------------------------------------- | :--------------------------------------- |
| [minCount](../javadocs/com/metreeca/form/shapes/MinCount.html)(count) | the size of the focus set is greater than or equal to the given minimum value |
| [maxCount](../javadocs/com/metreeca/form/shapes/MaxCount.html)(count) | the size of the focus set is less than or equal to the given maximum value |
| [in](../javadocs/com/metreeca/form/shapes/In.html)([value](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/model/Value.html), …) | the focus set is a subset a given set of target values |
| [all](../javadocs/com/metreeca/form/shapes/All.html)([value](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/model/Value.html), …) | the focus set includes all values from a given set of target values |
| [any](../javadocs/com/metreeca/form/shapes/Any.html)([value](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/model/Value.html), …) | the focus set includes at least one value from a given set of target values |

Common combinations of set constraints are directly available as shorthand shapes.

| shorthand shape                          | equivalent shape                   | constraint                  |
| :--------------------------------------- | :--------------------------------- | --------------------------- |
| [required](../javadocs/com/metreeca/form/Shape.html#required--) | `and(minCount(1), maxCount(1))`    | exactly one                 |
| [optional](../javadocs/com/metreeca/form/Shape.html#optional--) | `maxCount(1)`                      | at most one                 |
| [repeatable](../javadocs/com/metreeca/form/Shape.html#repeatable--) | `minCount(1)`                      | at least one                |
| [multiple](../javadocs/com/metreeca/form/Shape.html#multiple--) | `and()`                            | any number                  |
| [only](../javadocs/com/metreeca/form/Shape.html#only-org.eclipse.rdf4j.model.Value...-)([value](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/model/Value.html), …) | `and(all(value, …), in(value, …))` | constant pre-defined values |

## Structural Constraints

Composite shapes specifying constraints to be met by a derived focus set generated by a path.

| shape                                                        | constraint                                                   |
| :----------------------------------------------------------- | :----------------------------------------------------------- |
| [field](../javadocs/com/metreeca/form/shapes/Field.html)([IRI](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/model/IRI.html), [shape](../javadocs/com/metreeca/form/Shape.html)) | the derived focus set generated by a single step path is consistent with a given shape |

Inverse path steps may be generated using the [Values.inverse(IRI)](../javadocs/com/metreeca/form/things/Values.html#inverse-org.eclipse.rdf4j.model.IRI-) factory method.

## Logical Constraints

Composite shapes specifying logical combinations of shapes.

| shape                                                        | constraint                                                   |
| :----------------------------------------------------------- | :----------------------------------------------------------- |
| [and](../javadocs/com/metreeca/form/shapes/And.html)([shape](../javadocs/com/metreeca/form/Shape.html), …) | the focus set is consistent with all shapes in a given target set |
| [or](../javadocs/com/metreeca/form/shapes/Or.html)([shape](../javadocs/com/metreeca/form/Shape.html), …) | the focus set is consistent with at least one shape in a given target set |
| [when](../javadocs/com/metreeca/form/shapes/When.html)([test](../javadocs/com/metreeca/form/Shape.html),[pass](../javadocs/com/metreeca/form/Shape.html) [, [fail](../javadocs/com/metreeca/form/Shape.html)]) | the focus set is consistent either with a *pass* shape, if consistent also with a *test* shape, or with a *fail* shape, otherwise; if omitted, the `fail` shape defaults to `and()`, that is it's always meet |

<p class="warning">Test shapes for conditional constraints are currently limited to parametric <code>guards</code>.</p>

# Parameters

The combined use of conditional (`when`) and parametric (`guard`)  constraints supports the definition of **parametric** shapes, which specify different sets of constraints according to the current externally-assigned value of parametric **axis** variables.

Parametric axes may be specified for arbitrary custom variables, but the system relies on four pre‑defined parametric variables to support fine‑grained access control rules and role‑dependent read/write resource views.

Common combinations of parametric shapes on these axes are directly available as shorthand shapes.

## Role Axis

Parametric shapes for the [role](../javadocs/com/metreeca/form/Form.html#role) axis [selectively enable](../javadocs/com/metreeca/form/Shape.html#role-java.util.Set-) nested shapes according to the roles of the (possibly authenticated) user performing HTTP/S operations on target linked data resources.

| roles | shorthand | usage context |
|-------|-------|-------------|
| [value](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/model/Value.html), …|[role](../javadocs/com/metreeca/form/Shape.html#role-java.util.Set-com.metreeca.form.Shape...-)(set([value](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/model/Value.html), …), [shape](../javadocs/com/metreeca/form/Shape.html), …)| nested constraints are to be considered only if the sets of [roles](../javadocs/com/metreeca/rest/Request.html#roles--) enabled for the user performing a request contains at least one of the given role values |


## Task Axis

Parametric shapes for the [task](../javadocs/com/metreeca/form/Form.html#task) axis [selectively enable](../javadocs/com/metreeca/form/Shape.html#task-org.eclipse.rdf4j.model.Value-) nested shapes according to the method of the HTTP/S operations performed on target linked data resources.

| task                                                     | shorthand                                                    | HTTP/S method   |   usage context                                                        |
| -------------------------------------------------------- | ------------------------------------------------------------ | --------------- | ------------------------------------------------------------ |
| [create](../javadocs/com/metreeca/form/Form.html#create) | [create](../javadocs/com/metreeca/form/Shape.html#create-com.metreeca.form.Shape...-)([shape](../javadocs/com/metreeca/form/Shape.html), …) | POST            | resource creation                                            |
| [relate](../javadocs/com/metreeca/form/Form.html#relate) | [relate](../javadocs/com/metreeca/form/Shape.html#relate-com.metreeca.form.Shape...-)([shape](../javadocs/com/metreeca/form/Shape.html), …) | GET             | resource retrieval                                           |
| [update](../javadocs/com/metreeca/form/Form.html#update) | [update](../javadocs/com/metreeca/form/Shape.html#update-com.metreeca.form.Shape...-)([shape](../javadocs/com/metreeca/form/Shape.html), …) | PUT             | resource updating                                            |
| [delete](../javadocs/com/metreeca/form/Form.html#delete) | [delete](../javadocs/com/metreeca/form/Shape.html#delete-com.metreeca.form.Shape...-)([shape](../javadocs/com/metreeca/form/Shape.html), …) | DELETE          | resouce deletion                                             |
| [client](../javadocs/com/metreeca/form/Form.html#client) | [client](../javadocs/com/metreeca/form/Shape.html#server-com.metreeca.form.Shape...-)([shape](../javadocs/com/metreeca/form/Shape.html), …) | POST+GET+DELETE | shorthand for client-managed data, specified at creation time, but not updated afterwards |
| [server](../javadocs/com/metreeca/form/Form.html#server) | [server](../javadocs/com/metreeca/form/Shape.html#server-com.metreeca.form.Shape...-)([shape](../javadocs/com/metreeca/form/Shape.html), …) | GET+DELETE      | shorthand for server-managed data, neither specified at creation time, nor updated afterwards |

## View Axis

Parametric shapes for the [view](../javadocs/com/metreeca/form/Form.html#view) axis [selectively enable](../javadocs/com/metreeca/form/Shape.html#view-org.eclipse.rdf4j.model.Value-) nested shapes according to the usage context.

| view                                     | shorthand                                | usage context                            |
| ---------------------------------------- | ---------------------------------------- | ---------------------------------------- |
| [digest](../javadocs/com/metreeca/form/Form.html#digest) | [digest](../javadocs/com/metreeca/form/Shape.html#digest-com.metreeca.form.Shape...-)([shape](../javadocs/com/metreeca/form/Shape.html), …) | short resource description, e.g. inside search result sets |
| [detail](../javadocs/com/metreeca/form/Form.html#detail) | [detail](../javadocs/com/metreeca/form/Shape.html#detail-com.metreeca.form.Shape...-)([shape](../javadocs/com/metreeca/form/Shape.html), …) | full resource description                |

## Mode Axis

Parametric shapes for the [mode](../javadocs/com/metreeca/form/Form.html#mode) axis [selectively enable](../javadocs/com/metreeca/form/Shape.html#mode-org.eclipse.rdf4j.model.Value-) nested shapes according to the usage pattern.

| mode                                     | shorthand                                | usage pattern                            |
| ---------------------------------------- | ---------------------------------------- | ---------------------------------------- |
| [verify](../javadocs/com/metreeca/form/Form.html#verify) | [verify](../javadocs/com/metreeca/form/Shape.html#verify-com.metreeca.form.Shape...-)([shape](../javadocs/com/metreeca/form/Shape.html), …) | nested constraints are to be used only for validating incoming data and not for selecting existing resources to be processed |
| [filter](../javadocs/com/metreeca/form/Form.html#filter) | [filter](../javadocs/com/metreeca/form/Shape.html#filter-com.metreeca.form.Shape...-)([shape](../javadocs/com/metreeca/form/Shape.html), …) | nested constraints are to be used only for selecting existing resources to be processed and not for validating incoming data |

