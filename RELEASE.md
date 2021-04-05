# Broken ;-(

- REST / Renamed `Context.asset()` to `Toolbox.service()` to avoid widespread conflicts with other concepts (RDF
  statement context, JSON-LD context, web app context, web app asset, …)
- REST / Merged `Engine.browse()/relate()` methods and removed `Browser` handler
- REST / Migrated engine transaction mgmt to the dedicated `Engine.transaction())` method
- REST / Renamed `Gateway` wrapper to `Server`
- JSE / Merge `JSE.context(String/IRI)` setters to simplify API

# Added

- REST / Added `Handler.route()/asset()` conditional handler factories

# Improved

- REST / Added request IRI placeholder to `status(code, details)` response generator method
- REST / Extended the default secret `vault()` implementation to retrieve parameters also from environment variables
- REST / Hardened `Vault.get()` method against empty parameter identifiers
- Head / Hardened `JSEServer` and `JEEServer` against empty header names

# Fixed