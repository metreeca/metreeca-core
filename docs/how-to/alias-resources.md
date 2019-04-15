---
title:      How To Alias Resources
---

Sometimes you need to access resources using alternate identifiers or to set up simplified query endpoints: the [Aliaser](../javadocs/com/metreeca/rest/wrappers/Aliaser.html) wrapper/handler supports these use cases redirecting requests to canonical resources located by a custom alias resolver.

The following samples present typical setups built on the same data used in the [tutorials](../tutorials/publishing-ldp-apis) and a custom name-based product resolver.

```java
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.rest.wrappers.Connector.connect;

private Optional<IRI> byname(final RepositoryConnection connection, final String name) {
  return stream(connection.getStatements(null, RDFS.LABEL, literal(name)))
      .map(Statement::getSubject)
      .filter(resource -> connection.hasStatement(resource, RDF.TYPE, BIRT.Product, true))
      .map(resource -> (IRI)resource)
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

  .path("/products/byname/{name}", new Aliaser(connect((connection, request) -> request
      .parameter("name")
      .flatMap(name -> byname(connection, name))
  )))
```

## Shared Endpoints

```java
new Router()

    .path("/products/{id}", new Worker()
        .get(new Relator()
            .with(new Aliaser(connect((connection, request) -> request.parameter("id")
               
                // match provided id against expected code pattern
               
                .flatMap(code -> code.matches("S\\d+_\\d+")
          
                    // a product code: fall through to handler
             
                    ? Optional.of(request.item())
             
                     // a product name: resolve and redirect
             
                    : byname(connection, code)
             
                )
            )))
        )
```

# Simplified Query Endpoints

```java
new Router()

    .path("/products/", new Worker()
        .get(new Relator()
            .with(new Aliaser(connect((connection, request) -> request.parameter("name")
               
                // product name provided as query parameter: resolve and redirect
               
                .map(name -> byname(connection, name))
               
                // no name: fall through to handler and full faceted search
               
                .orElseGet(() -> Optional.of(request.item()))
               
            )))
        )
```

```
GET http://localhost:8080/products/

200 OK

{
  "_this": "/products/",
  "contains": [
    {
      "_this": "/products/S10_1678",
      "type": "/terms#Product",
      "label": "1969 Harley Davidson Ultimate Chopper",
      
      ⋮
      
    },
            
    ⋮
    
   ]  
}
```

```
GET http://localhost:8080/products/?name=Pont+Yacht

303 See Other
Location: http://localhost:8080/products/S72_3212
```

```
GET http://localhost:8080/products/?name=Something

404 Not Found
```
