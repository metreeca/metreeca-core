# Changed

- REST / Renamed Gateway wrapper to Server

# Added

# Improved

- REST / Added request IRI placeholder to `status(code, details)` response generator method
- REST / Extended the default secret `vault()` implementation to retrieve parameters also from environment variables
- REST / Hardened `Vault.get()` method against empty parameter identifiers

# Fixed