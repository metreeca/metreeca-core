---
title:		System APIs Reference 
excerpt:	Standard system-provided REST APIs
tags:		Reference
module:     "Metreeca Linked Data Framework"
version:    "0.46"
---

Standard system-managed services are exposed as REST APIs, enabling third-party apps and agents to programmaticaly interact with the platform.

System REST APIs return and accept RDF payloads as defined by associated payload [shapes](../../com.metreeca.spec/references/spec-language), with full support for HTTP content negotiation over available RDF formats, including [idiomatic JSON](../../com.metreeca.spec/references/idiomatic-json) serialization.

The default system configuration includes all the standard services documented in the following sections. Custom configuration may selectively enable required system services by including the fully qualified name of the relevant [service](../javadocs/com/metreeca/link/services/package-summary.html) class in the dedicated [service loader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) configuration file at `META-INF/services/com.metreeca.link.Service`.

# Root

| path | method | description                              | authorization |
| ---- | ------ | ---------------------------------------- | ------------- |
| `/`  | GET    | Retrieve the [VoID](https://www.w3.org/TR/void/) description of the server | public        |

### Payload Shape

```java
trait(VOID.ROOT_RESOURCE, and(
    trait(RDFS.LABEL, and(optional(), datatype(XMLSchema.STRING))),
    trait(RDFS.COMMENT, and(optional(), datatype(XMLSchema.STRING))))
)
```

### Payload Sample

```json
{
    "this": "http://localhost/",
    "rootResource": [
        {
            "this": "http://localhost/products/",
            "label": "Products"
        },
        {
            "this": "http://localhost/product-lines/",
            "label": "Product Lines"
        }
    ]
}
```

# Meta Services

Meta services expose read/write system administration REST APIs, enabling third-party apps and agents to programmaticaly monitor and configure the platform.

These APIs are not listed in the [LDP Port catalog](#ldp-ports) and may be accessed only by agents authenthicated with the system administrator role.

Payload specs and samples make use of the following system-defined RDF namespaces:

```sparql
prefix link: <tag:com.metreeca,2016:link/terms#>
prefix spec: <tag:com.metreeca,2016:spec/terms#>
```

## Status

| path  | method | description                              | authorization |
| ----- | ------ | ---------------------------------------- | ------------- |
| `/!`/ | GET    | Retrieve system status and configuration | sysadm        |

## RDF Namespaces

| path       | method | description                        | authorization |
| ---------- | ------ | ---------------------------------- | ------------- |
| `/!/names` | GET    | Retrieve RDF namespace definitions | sysadm        |
|            | PUT    | Update RDF namespace definitions   | sysadm        |

### Payload Shape

```java
trait(Link.Entry, and(datatype(Values.BNode),
    trait(Link.Key, and(required(), datatype(XMLSchema.String), pattern("\\w*"))),
    trait(Link.Value, and(required(), datatype(Values.IRI)))
))
```

### Payload Sample

```json
{
    "entry": [
        {
            "key": "rdf",
            "value":"http://www.w3.org/1999/02/22-rdf-syntax-ns#"
        },
        {
            "key": "rdfs",
            "value": "http://www.w3.org/2000/01/rdf-schema#"
        },
        {
            "key": "xsd",
            "value": "http://www.w3.org/2001/XMLSchema#"
        }
    ]
}
```

## LDP Ports

| path              | method  | description                              | authorization |
| ----------------- | ------- | ---------------------------------------- | ------------- |
| `/!/ports/`       | GET     | Retrieve LDP port catalog entries        | sysadm        |
|                   | POST    | Create a new model-driven LDP port       | sysadm        |
| `/!/ports/{uuid}` | GET     | Retrieve LDP port configuration detail   | sysadm        |
|                   | PUT*    | Update LDP port configuration            | sysadm        |
|                   | DELETE* | Delete LDP port                          | sysadm        |
|                   |         | * *available only for ports created through the REST API* |               |

The `/!/ports/` service is a model-driven LDP Basic Container supporting extended [faceted search](../../com.metreeca.spec/references/faceted-search). 

### Payload Shape

```java
and(

    trait(RDF.TYPE, and(required(), only(Link.Port))),

    verify(

        trait(RDFS.LABEL, and(required(), datatype(XMLSchema.STRING))),
        trait(RDFS.COMMENT, and(optional(), datatype(XMLSchema.STRING))),

        trait(Link.root, and(required(), datatype(XMLSchema.BOOLEAN))),
        trait(Link.path, and(required(), pattern(PathPattern))),

        and(

            trait(Link.spec, and(required(), datatype(XMLSchema.STRING))),

            trait(Link.create, and(optional(), datatype(XMLSchema.STRING))),
            trait(Link.update, and(optional(), datatype(XMLSchema.STRING))),
            trait(Link.delete, and(optional(), datatype(XMLSchema.STRING))),
            trait(Link.mutate, and(optional(), datatype(XMLSchema.STRING)))
        ),

        server( // server-managed read-only properties

            trait(Link.uuid, and(required(), datatype(XMLSchema.STRING))),
            trait(Link.soft, and(required(), datatype(XMLSchema.BOOLEAN))),

            trait(RDFS.ISDEFINEDBY, and(optional(), datatype(Values.IRI),
                relate(trait(RDFS.LABEL, optional()))
            ))    
        )    
    )
)
```
<!-- keep aligned modelling tutorial -->


| property           | value                                    |
| ------------------ | ---------------------------------------- |
| `rdfs:label`       | a human-readable port label              |
| `rdfs:comment`     | an optional human-readable port description |
| `link:root`        | root resource flag; if `true`, flags the resource handled by the port as a navigation entry-point, to be included, for instance, as a root resource in the system [VoID](https://www.w3.org/TR/void/) report |
| `link:path`        | server-relative path pattern; must be a syntactically correct [URI absolute path](https://tools.ietf.org/html/rfc3986#section-3.3), optionally followed by a question mark (`?`), a slash (`/`) or a wildcard (`/*`) |
| `link:spec`        | [RDF-encoded](../../com.metreeca.spec/references/spec-language#rdf-encoding) port shape serialized using [N-Triples](https://www.w3.org/TR/n-triples/) |
| `link:create`      | SPARQL Update post-processing script for POST method |
| `link:update`      | SPARQL Update post-processing script for PUT method |
| `link:delete`      | SPARQL Update post-processing script for DELETE method |
| `link:mutate`      | SPARQL Update post-processing script for all state-mutating methods (POST/PUT/DELETE) |
| `link:uuid`        | port UUID                                |
| `link:soft`        | soft port flag; `true` for system-managed ports created through the REST API; `false` for hardwired ports created programmatically |
| `rdfs:isDefinedBy` | optional reference linking ancillary ports to a *master* port |

<p class="warning">The <code>link:spec</code> format is provisional and likely to change after shared shapes are introduced.</p>

The optional trailing character in the path pattern controls how resources are handled by the matching port according to the following schema.

| path pattern | handling mode                            |
| ------------ | ---------------------------------------- |
| `<path>`     | the resource at `<path>` will be handled as an LDP RDF Resource, exposing RDF properties as specified by the port shape (more on that in the next steps…) |
| *`<path>?`*  | the resource at `<path>` will be handled as an LDP Basic Container including all of the RDF resources matched by the port shape |
| *`<path>/`*  | the resource at `<path>/` will be handled as an LDP Basic Container including all of the RDF resources matched by the port shape; an ancillary port mapped to the `<path>/*` pattern is automatically generated to handle container items as LDP RDF Resources, exposing RDF properties as specified by the port shape |
| `<path>/*`   | every resource with an IRI starting with `<path>/` will be handled as an LDP RDF Resource, exposing RDF properties as specified by the port shape |

All resources handled as LDP Basic Container support [faceted search](../../com.metreeca.spec/references/faceted-search), sorting and pagination out of the box.

SPARQL Update post-processing scripts are executed after the corresponding state-mutating HTTP method is successfully applied to the target resource, with the following bindings:

| variable | value                                    |
| -------- | ---------------------------------------- |
| `<base>` | the server base URL of the HTTP request  |
| `?this`  | the IRI of the targe resource either as derived from the HTTP request or as defined by the `Location` HTTP header after a POST request |

### Payload Sample

```json
{
    "this": "http://localhost/!/ports/0d32d37f-47f0-390b-9d9d-5181dc492187",
    "type": "tag:com.metreeca,2016:link/terms#Port",
    "label": "Product Lines",
    "path": "/product-lines/",
    "root": true,
    "uuid": "0d32d37f-47f0-390b-9d9d-5181dc492187",
    "soft": false
}
```

# Data Services

## SPARQL 1.1 Query/Update

| path      | method   | description                              | authorization                            |
| --------- | -------- | ---------------------------------------- | ---------------------------------------- |
| `/sparql` | GET/POST | [SPARQL 1.1 Protocol](http://www.w3.org/TR/sparql11-protocol/) Query operations | sysadm unless configured as [public](../../com.metreeca.data/handbooks/configuration#queryupdate) |
|           |          | [SPARQL 1.1 Protocol](http://www.w3.org/TR/sparql11-protocol/) Update operations | sysadm                                   |

Service behaviour may be be fine-tuned through system [configuration](../../com.metreeca.data/handbooks/configuration#queryupdate) properties.

## SPARQL 1.1 Graph Store

| path      | method          | description                              | authorization                            |
| --------- | --------------- | ---------------------------------------- | ---------------------------------------- |
| `/graphs` | GET/HEAD        | [SPARQL 1.1 Graph Store HTTP Protocol](http://www.w3.org/TR/sparql11-http-rdf-update) read operations | sysadm unless configured as [public](../../com.metreeca.data/handbooks/configuration#graph-store) |
|           | POST/PUT/DELETE | [SPARQL 1.1 Graph Store HTTP Protocol](http://www.w3.org/TR/sparql11-http-rdf-update) write operations | sysadm                                   |

Service behaviour may be be fine-tuned through system [configuration](../../com.metreeca.data/handbooks/configuration#graph-store) properties.

This impementation extends the standard protocol with the following methods.

| path      | method | description                              | authorization                            |
| --------- | ------ | ---------------------------------------- | ---------------------------------------- |
| `/graphs` | GET    | Retrieve named graphs [VoID](https://www.w3.org/TR/void/) descriptions (no `default`/`graph` query parameters) | sysadm unless configured as [public](../../com.metreeca.data/handbooks/configuration#graph-store) |

### Payload Shape

```java
trait(RDF.VALUE, and(
  
  trait(RDF.TYPE, only(VOID.DATASET))
  
))
```

### Payload Sample

```json
{
    "this": "http://localhost/graphs",
    "value": [
        {
            "this": "http://localhost/data",
            "type": "http://rdfs.org/ns/void#Dataset"
        }
    ]
}
```
