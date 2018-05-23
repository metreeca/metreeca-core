---
title:      Configuration Reference
excerpt:    Platform configuration options
caption:    "Metreeca Tooling Framework"
project:    "com.metreeca:tray"
version:    "0.0"
---

Standard platform-provided components may be configured using the configuration properties defined in the following sections.

Configuration properties may be defined either as **system properties** using Java  command line options (`-D<property>=<value>`) or included in a **configuration file** using the standard format for Java [property files](https://docs.oracle.com/javase/8/docs/api/java/util/Properties.html#load-java.io.Reader-). 

Configuration properties defined as system properties take precedence over properties defined in configuration files.

Java system properties and configuration files are defined and loaded from system-specific sources varying according to the deployment option.

| property type | notes                                    |
| :------------ | :--------------------------------------- |
| boolean       | boolean properties are considered `true` if their value is equal, ignoring case, to the string "true" |
| numeric       | digits in numeric property values may be spaced for readability with an underscore charecter ("_") |
| file path     | unless otherwise specified, relative paths in configuration properties are interpreted with respect to the current working directory of the process as read from the `user.dir` system property |

# Platform

| property         | type      | value                                                        | default                                                    |
| :--------------- | --------- | :----------------------------------------------------------- | :--------------------------------------------------------- |
| setup.properties | file path | the path of the configuration properties file; this property is intended to be defined as a system property, e.g. through a java command-line flag as `-Dsetup.properties=<path>` | empty (setup is loaded from a deployment dependent source) |
| setup.storage    | file path | the path of the default storage folder                       | the current working directory                              |
| setup.base       | URL       | the absolute canonical linked data base URL                  | the server base URL of the current HTTP request            |

Linked data resources published by the platform may be accessed using a number of different URLs pointing to the same server: if the *canonical* base URL is defined using the `setup.base` property (and it's actually different from the *alternate* base URL used to access the resource), the rewriting engine dynamically replaces/restores the *alternate* base URL with/from the *canonical* base URL in the target linked data URL and incoming/outgoing RDF payloads.

<p class="warning">Dynamic URL rewriting is mainly intended to enable data portability among test/staging and production instances: the process may be quite expensive, so make sure that the canonical URL matches the expected server base URL of the production instance.</p>

<p class="warning">Modifying the <code>setup.base</code> property in the configuration file won't automatically migrate to the new canonical base URL existing linked data resources in the graph backend.</p>

## RDF Spooler

The RDF [spooler](administration#rdf-spooler) process supports unassisted bulk upload of RFD files to the [graph backend](#graph-backend).

| property      | type      | value                                    | default                                |
| ------------- | --------- | ---------------------------------------- | -------------------------------------- |
| spool.storage | file path | the path of the RDF spooling folder      | [`{setup.storage}`](#platform)`/spool` |
| spool.base    | IRI       | the base IRI to be used during RDF parsing | [`{setup.base}`](#platform)            |

# SPARQL Endpoints

The following standard SPARQL 1.1 [endpoints](administration#sparql-endpoints) expose RDF content from the [graph backend](#graph-backend).

| endpoint                                 | URL                         |
| ---------------------------------------- | --------------------------- |
| [SPARQL 1.1 Query/Update](http://www.w3.org/TR/sparql11-protocol) | `http(s)://{server}/sparql` |
| [SPARQL 1.1 Graph Store](http://www.w3.org/TR/sparql11-http-rdf-update) | `http(s)://{server}/graphs` |

## Query/Update

| property       | type    | value                                    | default |
| :------------- | ------- | :--------------------------------------- | :------ |
| sparql.timeout | integer | SPARQL operations timeout [s]            | 60 s    |
| sparql.public  | boolean | if `true`, SPARQL Query operations are available without authentication | false   |

## Graph Store

| property      | type    | value                                    | default |
| :------------ | ------- | :--------------------------------------- | :------ |
| graphs.public | boolean | if `true`, SPARQL Graph Store GET/HEAD operations are available without authentication | false   |

## Proxy

| property              | type    | value                        | default |
| :-------------------- | ------- | :--------------------------- | :------ |
| proxy.timeout.connect | integer | proxy connection timeout [s] | 30 s    |
| proxy.timeout.read    | integer | proxy read timeout [s]       | 60 s    |

# Graph Backend

The graph backend database stores RDF content managed by the platform.

A wide range of third-party solutions, supporting the extended  [RDF4J Server REST API](http://docs.rdf4j.org/rest-api/) or other proprietary wire protocols, can be configured as embedded or external graph storage backends.

| property | type   | value                                    | default  |
| :------- | ------ | :--------------------------------------- | :------- |
| graph    | string | the type of the graph storage backend, as defined in the following sections | `memory` |

## RDF4J Memory

Embedded in-memory graph backend with optional disk-based persistence.

| property                | type      | value                                    | default                                |
| ----------------------- | --------- | ---------------------------------------- | -------------------------------------- |
| graph                   | string    | `memory`                                 |                                        |
| graph.memory.persistent | boolean   | if true, graph contents are persisted    | `false`                                |
| graph.memory.storage    | file path | the path of the storage directory for the graph backend | [`{setup.storage}`](#platform)`/graph` |

## RDF4J Native

Embedded graph backend with disk-based persistence.

| property             | type      | value                                    | default                                |
| -------------------- | --------- | ---------------------------------------- | -------------------------------------- |
| graph                | string    | `native`                                 |                                        |
| graph.native.storage | file path | the path of the storage directory for the graph backend | [`{setup.storage}`](#platform)`/graph` |

## RDF4J Remote

Remote graph backend supporting [RDF4J Server REST API](http://docs.rdf4j.org/rest-api/).

| property         | type   | value                                    | default  |
| ---------------- | ------ | ---------------------------------------- | -------- |
| graph            | string | `remote`                                 |          |
| graph.remote.url | URL    | the URL of the target repository on the remote RDF4J server | required |
| graph.remote.usr | string | the username to use for authenticating with the remote repository | empty    |
| graph.remote.pwd | string | the password to use for authenticating with the remote repository | empty    |

<p id="graphdb" class="warning">Ontotext GraphDB supports RDF4J Server REST API, but as of v8.x a known issue with transaction management prevents the SHACL engine from properly validating incoming data, causing severe errors on resource creation and updating.</p>

<p class="note">Linked data navigation and analysis features are unaffected.</p>

Until the backend issue is resolved, connect to GraphDB respositories using the <a href="#sparql-1-1-store">SPARQL 1.1 Store </a> backend option, with the following base configuration:

| property            | value                                    |
| ------------------- | ---------------------------------------- |
| graph               | `sparql`                                 |
| graph.sparql.query  | `http(s)://{server}/repositories/{repository-id}` |
| graph.sparql.update | `http(s)://{server}/repositories/{repository-id}/statements` |

## SPARQL 1.1

Remote graph backend supporting vanilla query/update [SPARQL 1.1 Protocol](https://www.w3.org/TR/sparql11-protocol/).

| property            | type   | value                                    | default                                  |
| ------------------- | ------ | ---------------------------------------- | ---------------------------------------- |
| graph               | string | `sparql`                                 |                                          |
| graph.sparql.url    | URL    | the URL of the remote SPARQL 1.1 endpoint | required, unless both `{graph.sparql.query}` and `{graph.sparql.update}` are specified as absolute URLs |
| graph.sparql.query  | URL    | the URL of the remote SPARQL 1.1 Query endpoint; relative URLs will be resolved against `{graph.sparql.url}` | required if `{graph.sparql.url}` is empty; otherwise `{graph.sparql.url}` |
| graph.sparql.update | URL    | the URL of the remote SPARQ 1.1L Update endpoint; relative URLs will be resolved against `{graph.sparql.url}` | required if `{graph.sparql.url}` is empty; otherwise `{graph.sparql.url}` |
| graph.sparql.usr    | string | the username to use for authenticating with remote endpoints | empty                                    |
| graph.sparql.pwd    | string | the password to use for authenticating with remote endpoints | empty                                    |

<p class="warning">REST transaction management is not generally available on plain SPARQL 1.1 endpoints, preventing the SHACL engine from fully validating incoming data in the context of the target graph and orderly executing post-processing hooks.</p>

<p class="note">Linked data navigation and analysis features are unaffected.</p>

Whe using this option to connect to a SPARQL 1.1 endpoint:

- POST/PUT/DELETE HTTP operations on model-driven LDP resources are restricted to authenticated agents with administration privileges;
- internal index updates and resource post-processing hooks are executed outside transactions, possibly exposing inconsistent states.

## Virtuoso

Remote [Virtuoso](https://virtuoso.openlinksw.com) graph backend.

| property               | type   | value                                    | default   |
| ---------------------- | ------ | ---------------------------------------- | --------- |
| graph                  | string | `virtuoso`                               |           |
| graph.virtuoso.url     | URL    | the [JDBC URL](http://docs.openlinksw.com/virtuoso/jdbcurl4mat/) of the target database managed by the remote Virtuoso engine | required  |
| graph.virtuoso.usr     | string | the username to use for authenticating with the remote engine | empty     |
| graph.virtuoso.pwd     | string | the password to use for authenticating the remote engine | empty     |
| graph.virtuoso.default | IRI    | the IRI of the default graph in the quad store | `rdf:nil` |
