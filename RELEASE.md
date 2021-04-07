# Broken ;-(

- JSON / Restricted `Frame.focus()` to `IRI` values
- JSON / Migrated `Frame` API to focus `Shift` operators
- REST / Migrated `JSONLDFormat` to `Frame` payload to improve usability
- REST / Renamed `Context.asset()` to `Toolbox.service()` to avoid widespread conflicts with other concepts (RDF
  statement context, JSON-LD context, web app context, web app asset, …)
- REST / Merged `Engine.browse()/relate()` methods and removed `Browser` handler
- REST / Factored request handling code to CRUD handlers and simplified `Engine` API
- REST / Migrated engine transaction mgmt to the dedicated `Engine.transaction())` method
- REST / Migrated shape-based access control from `Engine.throttler()` to `Wrapper.keeper()`
- REST / Renamed `Gateway` wrapper to `Server`
- JSE / Merge `JSE.context(String/IRI)` setters to simplify API

# Added

- JSON / Extended `Frame` API with typed getters/setters
- JSON / Lay down focus `Shift` operators (predicate paths, value mappings, aggregates, …)
- REST / Added `Handler.route()/asset()` conditional handler factories

# Improved

- REST / Added request IRI placeholder to `status(code, details)` response generator method
- REST / Extended the default secret `vault()` implementation to retrieve parameters also from environment variables

# Fixed

- REST / Hardened `Vault.get()` method against empty parameter identifiers
- Head / Hardened `JSEServer` and `JEEServer` against empty header names
