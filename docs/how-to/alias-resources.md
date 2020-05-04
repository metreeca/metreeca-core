---
title:      How To Alias Resources
---

Sometimes you need to access resources using alternate identifiers or to set up simplified query endpoints: the [Aliaser](../javadocs/com/metreeca/rest/wrappers/Aliaser.html) wrapper/handler supports these use cases redirecting requests to canonical resources located by a custom alias resolver.

The following samples present typical setups built on the same data used in the [tutorials](../tutorials/publishing-ldp-apis) and a custom name-based product resolver.

```java
private Optional<String> byname(final RepositoryConnection connection, final String name) {
    return stream(connection.getStatements(null, RDFS.LABEL, literal(name)))
            .map(Statement::getSubject)
            .filter(resource -> connection.hasStatement(resource, RDF.TYPE, BIRT.Product, true))
            .map(Value::stringValue)
            .findFirst();
}
```

# Alternate Identifiers

## Independent Endpoints

```java
new Router()
 
  // primary code-based endpoint

  .path("/products/{code}", new Worker()
    .get(new Relator())
  )
 
  // alternate name-based endpoint

  .path("/products/byname/{name}", new Aliaser(request -> request
        .parameter("name")
        .flatMap(connect(this::byname))
))
```

```shell
GET http://localhost:8080/products/byname/Pont+Yacht

303 See Other
Location: http://localhost:8080/products/S72_3212
```

## Shared Endpoints

```java
new Router()

    .path("/products/{code}", new Worker()
        .get(new Relator()
            .with(new Aliaser(request -> request
                    .parameter("code")
                    .filter(code -> !code.matches("S\\d+_\\d+")) // not a product code
                    .flatMap(connect(this::byname)) // resolve and redirect
            ))
        )
```

```shell
GET http://localhost:8080/products/Pont+Yacht

303 See Other
Location: http://localhost:8080/products/S72_3212
```

# Simplified Query Endpoints

```java
new Router()
    .path("/products/", new Worker()
        .get(new Relator()
            .with(new Aliaser(request -> request
                .parameter("name") // product name keywords provided as query parameter
                .map(name -> "?~label="+Codecs.encode(name)) // rewrite query
        ))
	)
)
```

```shell
GET http://localhost:8080/products/?name=Model+A

303 See Other
Location: http://localhost:8080/products/?~label=Model+A
```
