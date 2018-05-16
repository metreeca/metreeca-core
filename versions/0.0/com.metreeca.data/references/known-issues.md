---
title:		Known Issues
excerpt:	Known implementation and compatibility issues
tags:		Reference
module:     "Metreeca Linked Data Hub"
version:    "0.0"
---

# Inteoperability

## SPARQL Transaction Support

Metreeca [linked data API publishing](../tutorials/linked-data-development/) engine requires transaction support on the SPARQL backend in order to perform automatic validation and post-processing of incoming data: to this date only backends supporting the *de-facto* standard [RDF4J Server REST API](http://docs.rdf4j.org/rest-api/) are supported. More backend-specific adapters will be introduced in upcoming releases.

<p class="warning">Ontotext GraphDB supports RDF4J Server REST API, but as of v8.x a known issue with transaction management prevents the SHACL engine from properly validating incoming data, causing severe errors on resource creation and updating. Until resolved, connect to GraphDB respositories using the <a href="../handbooks/configuration#graphdb">SPARQL 1.1 Store </a> backend option.</p>

<p class="note">Linked data navigation and analysis features are unaffected.</p>

## SPARQL Compliance

Metreeca self-service [search and analysis](../tutorials/search-and-analysis/) tools are expected to interact with any SPARQL 1.1 endpoint, but we are currently testing mainly with RDF4J 2. [Issue reports](https://github.com/metreeca/metreeca/issues) are welcome…

# Performance

Metreeca is currently optimized for interactive activities on local mid-sized datasets: linked data navigation and option filtering will scale comfortably, but performance is likely to degrade when trying to apply other features to large datasets.

## Reverse Links

Introspection of reverse properties to be included in the left-hand column of the path picker dialog may be speeded up by enabling object-centric indexes in the triple store (e.g. _object/subject/predicate_ or _object/predicate/subject_).

## Full-Text and Range Searches

Optimizable by the SPARQL endpoint with dedicated indexing support, but that's outside our control.


## Aggregate Summaries and Filtering

Aggregate numeric summaries and filtering on aggregate values are inherently resource-heavy and possibly beyond optimization ;-(

## Cloud Delivery

-   The cloud delivery service is hosted on Google Application Engine and until the traffic is sustained the first connection after a period of inactivity may be a little delayed by the on-demand initialization of the virtual instance;
-   The demo dataset used in the tutorial is also hosted on a cloud service and is not expected to provide production-level performance: again, some delays during the first interactions and erratic performance levels are likely.

# User Interface

-   Safari isn't yet able to assign meaningful names to exported files: until a workaround is found, the user is expected to rename them manually.

