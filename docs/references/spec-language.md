---
title:      Shape Specification Language Reference
excerpt:    Linked data shape specification language semantics and components
---

Most framework services are driven by declarative linked data models defined using a [SHACL](https://www.w3.org/TR/shacl/)‑based [shape](#shapes) specification language.

Models are [programmaticaly](../tutorials/publishing-ldp-apis) built using a Java‑based DSL and can eventually automate a range of different tasks in the lifecycle of linked data REST APIs.

| task                | shape role                                                   |
| ------------------- | ------------------------------------------------------------ |
| data selection      | RDF data retrieved from [readable](../tutorials/interacting-with-ldp-apis#read-operations) linked data REST APIs is selected from the underlying graph storage layer by SPARQL queries derived from the associated linked data models |
| data validation     | RDF data submitted to [writable](../tutorials/interacting-with-ldp-apis#write-operations) linked data REST APIs on resource creation and updating is automatically validated against the associated linked data models |
| faceted search      | [faceted search](../tutorials/interacting-with-ldp-apis#faceted-search) and ancillary facet-populating queries are managed by the system on the basis of structural and typing constraints specified in linked data models associated with target linked data REST APIs |
| dynamic UI building | Interactive linked data navigators and editing forms could be dynamically created and laid out according to the linked data models associated with linked data REST APIs |
| API documentation   | Human and machine readable docs for linked data REST APIs could be dynamically derived from associated linked data models and published as hypertexts and [OpenAPI](https://www.openapis.org)/[Swagger](https://swagger.io/specification/) specs |
| data ingestion      | RDF data ingested from external data sources could be dynamically mapped to and validated against the linked data models associated with target RDF graphs |

<p class="warning">The details of the specification language are still in flux, especially when it comes to SHACL interoperability.</p>
<p class="warning">Some of the described features aren't (yet ;-) supported by the framework.</p>
# Shapes

Linked data [shapes](../javadocs/com/metreeca/tree/Shape.html) define the expected structure of RDF resource descriptions in terms of **constraints** imposed on members of a **focus set** of RDF terms and on the  values of their RDF properties.

**Primitive** shapes specify declarative constraints to be meet by the focus set and its member terms. **Composite** shapes recursively assemble other shapes into tree-structured complex constraints.

Linked data selection tasks identify the set of subjects in an RDF graph whose RDF descriptions are consistent with a possibly composite shape. Selection results are reported either as an RDF graph or a structured report according to the choosen [query](#queries) type.

Linked data validation tasks verify that the RDF description of an initial focus set is consistent with the constraints specified by a possibly composite shape. During the process, derived focus sets connected by [structural constraints](#structural-constraints) may be recursively validated. Validation results are reported as a structured [focus validation trace](../javadocs/com/metreeca/tree/Trace.html).

## Annotations

Non-validating shapes documenting shape metadata, to be used for purposes such as shape redaction, form building or predictable encoding of RDF files.

| shape                                                        | value                                                        |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| [meta](../javadocs/com/metreeca/tree/shapes/Meta.html)(tag, value, …) | the metadata property identified by the given tag is associated with the given value in the enclosing shape |
| [guard](../javadocs/com/metreeca/tree/shapes/Guard.html)(axis, value, …) | the focus set is consistent with this shape only if the value of an externally assigned [axis variable](../javadocs/com/metreeca/tree/probes/Redactor.html) is included in a given set of target values |

Common metadata annotations are directly available as shorthand shapes.

| shorthand shape                                              | value                                                        |
| :----------------------------------------------------------- | :----------------------------------------------------------- |
| [alias](../javadocs/com/metreeca/tree/shapes/Meta.html#alias-java.lang.String-)("alias") | an alternate property name for reporting values for the enclosing shape (e.g. in the [context](idiomatic-json#term-properties) of JSON-based RDF serialization results) |
| [label](../javadocs/com/metreeca/tree/shapes/Meta.html#label-java.lang.String-l)("label") | a human-readable textual label  for the enclosing shape      |
| [notes](../javadocs/com/metreeca/tree/shapes/Meta.html#notes-java.lang.String-)("notes") | a human-readable textual description of the enclosing shape  |
| [placeholder](../javadocs/com/metreeca/tree/shapes/Meta.html#placeholder-java.lang.String-)("placeholder") | a human-readable textual placeholder for the values of the enclosing shape |
| [dflt](../javadocs/com/metreeca/tree/shapes/Meta.html#dflt-java.lang.Object-)(value) | the default for the expected values of the enclosing shape   |
| [hint](../javadocs/com/metreeca/tree/shapes/Meta.html#hint-java.lang.String-)("IRI") | the IRI of a resource hinting at possible values for the enclosing shape (e.g. an LDP container) |
| [group](../javadocs/com/metreeca/tree/shapes/Meta.html#group-java.lang.String-)("value") | a client-specific group visualization mode for the enclosing shape |
| [index](com/metreeca/tree/shapes/Meta.html#index-boolean-)(true\|false) | an indexing hint: `true` if the value of the enclosing shape should be indexed in the storage backed, `false` otherwise |

## Term Constraints

Primitive shapes specifying constraints to be individually met by each RDF term in the focus set.

| shape                                                        | constraint                                                   |
| :----------------------------------------------------------- | :----------------------------------------------------------- |
| [datatype](../javadocs/com/metreeca/tree/shapes/Datatype.html)(type) | each term in the focus set has a given extended RDF datatype IRI; IRI references and blank nodes are considered to be respectively of `app:/terms#iri` ([Values.IRI](../javadocs/com/metreeca/rdf/Values.html#IRIType)) and `app:/terms#bnode` ([Values.BNode](../javadocs/com/metreeca/rdf/Values.html#BNodeType)) datatype |
| [class](../javadocs/com/metreeca/tree/shapes/Clazz.html)(name) | each term in the focus set is an instance of a given RDF class or one of its superclasses |
| [minExclusive](../javadocs/com/metreeca/tree/shapes/MinExclusive.html)(value) | each term in the focus set is strictly greater than a given minum value, according to <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#modOrderBy">SPARQL ordering</a> rules |
| [maxExclusive](../javadocs/com/metreeca/tree/shapes/MaxExclusive.html)(value) | each term in the focus set is strictly less than a given maximum value, according to <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#modOrderBy">SPARQL ordering</a> rules |
| [minInclusive](../javadocs/com/metreeca/tree/shapes/MinInclusive.html)(value) | each term in the focus set is greater than or equal to a given minimum value, according to <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#modOrderBy">SPARQL ordering</a> rules |
| [maxInclusive](../javadocs/com/metreeca/tree/shapes/MaxInclusive.html)(value) | each term in the focus set is less than or equal to a given maximum value, according to <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#modOrderBy">SPARQL ordering</a> rules |
| [minLength](../javadocs/com/metreeca/tree/shapes/MinLength.html)(length) | the length of the lexical representation of each term in the focus set is greater than or equal to the given minimum value |
| [maxLength](../javadocs/com/metreeca/tree/shapes/MaxLength.html)(length) | the length of the lexical representation of each term in the focus set is less than or equal to the given maximum value |
| [pattern](../javadocs/com/metreeca/tree/shapes/Pattern.html)("pattern") | the lexical representation of each term in the focus set matches a given [regular expression](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#sum) pattern |
| [like](../javadocs/com/metreeca/tree/shapes/Like.html)("keywords") | the lexical representation of each term in the focus set matches the given full-text keywords |

## Set Constraints

Primitive shapes specifying constraints to be collectively met by the RDF terms in the focus set.

| shape                                                        | constraint                                                   |
| :----------------------------------------------------------- | :----------------------------------------------------------- |
| [minCount](../javadocs/com/metreeca/tree/shapes/MinCount.html)(count) | the size of the focus set is greater than or equal to the given minimum value |
| [maxCount](../javadocs/com/metreeca/tree/shapes/MaxCount.html)(count) | the size of the focus set is less than or equal to the given maximum value |
| [in](../javadocs/com/metreeca/tree/shapes/In.html)(value, …) | the focus set is a subset a given set of target values       |
| [all](../javadocs/com/metreeca/tree/shapes/All.html)(value, …) | the focus set includes all values from a given set of target values |
| [any](../javadocs/com/metreeca/tree/shapes/Any.html)(value, …) | the focus set includes at least one value from a given set of target values |

Common combinations of set constraints are directly available as shorthand shapes.

| shorthand shape                                              | equivalent shape                   | constraint                  |
| :----------------------------------------------------------- | :--------------------------------- | --------------------------- |
| [required](../javadocs/com/metreeca/tree/Shape.html#required--) | `and(minCount(1), maxCount(1))`    | exactly one                 |
| [optional](../javadocs/com/metreeca/tree/Shape.html#optional--) | `maxCount(1)`                      | at most one                 |
| [repeatable](../javadocs/com/metreeca/tree/Shape.html#repeatable--) | `minCount(1)`                      | at least one                |
| [multiple](../javadocs/com/metreeca/tree/Shape.html#multiple--) | `and()`                            | any number                  |
| [only](../javadocs/com/metreeca/tree/Shape.html#only-org.eclipse.rdf4j.model.Value...-)(value, …) | `and(all(value, …), in(value, …))` | constant pre-defined values |

## Structural Constraints

Composite shapes specifying constraints to be met by a derived focus set generated by a path.

| shape                                                        | constraint                                                   |
| :----------------------------------------------------------- | :----------------------------------------------------------- |
| [field](../javadocs/com/metreeca/tree/shapes/Field.html)(name, [shape](../javadocs/com/metreeca/tree/Shape.html)) | the derived focus set generated by a single step path is consistent with a given shape |

Inverse path steps may be generated using the [Values.inverse(IRI)](../javadocs/com/metreeca/rdf/Values.html#inverse-org.eclipse.rdf4j.model.IRI-) factory method.

## Logical Constraints

Composite shapes specifying logical combinations of shapes.

| shape                                                        | constraint                                                   |
| :----------------------------------------------------------- | :----------------------------------------------------------- |
| [and](../javadocs/com/metreeca/tree/shapes/And.html)([shape](../javadocs/com/metreeca/tree/Shape.html), …) | the focus set is consistent with all shapes in a given target set |
| [or](../javadocs/com/metreeca/tree/shapes/Or.html)([shape](../javadocs/com/metreeca/tree/Shape.html), …) | the focus set is consistent with at least one shape in a given target set |
| [when](../javadocs/com/metreeca/tree/shapes/When.html)([test](../javadocs/com/metreeca/tree/Shape.html),[pass](../javadocs/com/metreeca/tree/Shape.html) [, [fail](../javadocs/com/metreeca/tree/Shape.html)]) | the focus set is consistent either with a *pass* shape, if consistent also with a *test* shape, or with a *fail* shape, otherwise; if omitted, the `fail` shape defaults to `and()`, that is it's always meet |

<p class="warning">Test shapes for conditional constraints are currently limited to combinations of parametric <code>guards</code> and logical operators.</p>
# Parameters

The combined use of conditional (`when`) and parametric (`guard`)  constraints supports the definition of **parametric** shapes, which specify different sets of constraints according to the current externally-assigned value of parametric **axis** variables.

Parametric axes may be specified for arbitrary custom variables, but the system relies on four pre‑defined parametric variables to support fine‑grained access control rules and role‑dependent read/write resource views.

Common combinations of parametric shapes on these axes are directly available as shorthand shapes.

Guards may be applied to groups of shapes either explicitely assembly a `when` shape, like:

```
when(<guard>, and(<shape>, …))
```

or using the shorthand [then](../javadocs/com/metreeca/tree/Shape.html#then-com.metreeca.tree.Shape...-) method, like:

```
<guard>.then(<shape>, …)
```

## Role Axis

Parametric guards for the [role](../javadocs/com/metreeca/tree/Shape.html#role) axis [selectively enable](../javadocs/com/metreeca/tree/Shape.html#role-java.util.Set-) target shapes according to the roles of the (possibly authenticated) user performing HTTP/S operations on target linked data resources.

| shorthand | usage context |
|-------|-------------|
|[role](../javadocs/com/metreeca/tree/Shape.html#role-java.util.Set-com.metreeca.tree.Shape...-)(value, …)| target shapes are to be considered only if the sets of [roles](../javadocs/com/metreeca/rest/Request.html#roles--) enabled for the user performing a request contains at least one of the given role values |


## Task Axis

Parametric guards for the [task](../javadocs/com/metreeca/tree/Shape.html#task) axis [selectively enable](../javadocs/com/metreeca/tree/Shape.html#task-org.eclipse.rdf4j.model.Value-) target shapes according to the method of the HTTP/S operations performed on target linked data resources.

| shorthand                                                    | HTTP/S method   | usage context                                                |
| ------------------------------------------------------------ | --------------- | ------------------------------------------------------------ |
| [create](../javadocs/com/metreeca/tree/Shape.html#create-com.metreeca.tree.Shape...-)() | POST            | resource creation                                            |
| [relate](../javadocs/com/metreeca/tree/Shape.html#relate-com.metreeca.tree.Shape...-)() | GET             | resource retrieval                                           |
| [update](../javadocs/com/metreeca/tree/Shape.html#update-com.metreeca.tree.Shape...-)([) | PUT             | resource updating                                            |
| [delete](../javadocs/com/metreeca/tree/Shape.html#delete-com.metreeca.tree.Shape...-)() | DELETE          | resouce deletion                                             |
| [client](../javadocs/com/metreeca/tree/Shape.html#server-com.metreeca.tree.Shape...-)() | POST+GET+DELETE | shorthand for client-managed data, specified at creation time, but not updated afterwards |
| [server](../javadocs/com/metreeca/tree/Shape.html#server-com.metreeca.tree.Shape...-)() | GET+DELETE      | shorthand for server-managed data, neither specified at creation time, nor updated afterwards |

## Area Axis

Parametric guards for the [area](../javadocs/com/metreeca/tree/Shape.html#Area) axis [selectively enable](../javadocs/com/metreeca/tree/Shape.html#view-org.eclipse.rdf4j.model.Value-) target shapes according to the usage context.

| shorthand                                                    | usage context                                               |
| ------------------------------------------------------------ | ----------------------------------------------------------- |
| [holder](../javadocs/com/metreeca/tree/Shape.html#holder--)() | collection holder description                               |
| [member](../javadocs/com/metreeca/tree/Shape.html#member--)() | collection member description (shorthand for digest+detail) |
| [digest](../javadocs/com/metreeca/tree/Shape.html#digest-com.metreeca.tree.Shape...-)() | short resource description, e.g. inside search result sets  |
| [detail](../javadocs/com/metreeca/tree/Shape.html#detail-com.metreeca.tree.Shape...-)() | full resource description                                   |

## Mode Axis

Parametric guards for the [mode](../javadocs/com/metreeca/tree/Shape.html#Mode) axis [selectively enable](../javadocs/com/metreeca/tree/Shape.html#mode-org.eclipse.rdf4j.model.Value-) target shapes according to the usage pattern.

| shorthand                                                    | usage pattern                                                |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| [convey](../javadocs/com/metreeca/tree/Shape.html#convey-com.metreeca.tree.Shape...-)() | target shapes are to be used only for validating incoming data or extracting outgoing data and not for selecting existing resources to be processed |
| [filter](../javadocs/com/metreeca/tree/Shape.html#filter-com.metreeca.tree.Shape...-)() | target shapes are to be used only for selecting existing resources to be processed and not for validating incoming data or extracting outgoing data |
