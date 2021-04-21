# Broken ;-(

- JSON / Simplified and generalized `Frame` API
- JSON / Restricted `Frame.focus()` to `Resource` values
- JSON / Migrated `Frame` API to focus `Shift` operators
- JSON / Reorganized and extended `Values` converter methods
- REST / Migrated `JSONLDFormat` to `Frame` payload to improve usability
- REST / Renamed `Context.asset()` to `Toolbox.service()` to avoid widespread conflicts with other concepts (RDF
  statement context, JSON-LD context, web app context, web app asset, …)
- REST / Factored configurable option mgmt to `Setup`
- REST / Merged `Engine.browse()/relate()` methods and removed `Browser` handler
- REST / Migrated `Creator` slug generator configuration fomr constructor to setter method
- REST / Factored request handling code to CRUD handlers and simplified `Engine` API
- REST / Removed transaction mgmt from `Engine` API
- REST / Migrated shape-based access control from `Engine.throttler()` to `Wrapper.keeper()`
- REST / Renamed `Gateway` wrapper to `Server`
- JSE / Merge `JSE.context(String/IRI)` setters to simplify API
- RDF4J / Simplified txn mgtm `Graph` API
- RDF4J / Migrated `Graph` SPARQL processors to `Frame`

# Added

- JSON / Extended `Frame` API with typed getters/setters
- JSON / Lay down focus `Shift` operators (predicate paths, value mappings, aggregates, …)
- REST / Added `Handler.route()/asset()` conditional handler factories

# Improved

- REST / Added request IRI placeholder to `status(code, details)` response generator method
- REST / Extended the default secret `vault()` implementation to retrieve parameters also from environment variables

# Fixed

- REST / `JSONLDFormat.query()` now correctly resolve aliased JSON-LD keywords
- REST / Hardened `Vault.get()` method against empty parameter identifiers
- Head / Hardened `JSEServer` and `JEEServer` against empty header names
