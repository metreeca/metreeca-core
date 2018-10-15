---
title: FAQs and Known Issues
---

# Graph Backend

## SPARQL Transaction Support

The linked data API publishing engine requires transaction support on the SPARQL [graph backend](javadocs/?com/metreeca/tray/rdf/Graph.html) in order to perform automatic validation and post-processing of incoming data: to this date only backends supporting the de-facto standard [RDF4J Server REST API](http://docs.rdf4j.org/rest-api/) are supported. More backend-specific adapters will be introduced in future releases.

<p class="warning">Ontotext GraphDB supports RDF4J Server REST API, but as of v8.x a known issue with transaction management prevents the SHACL engine from properly validating incoming data, causing severe errors on resource creation and updating. Until resolved, connect to GraphDB respositories using the <a href="javadocs/?com/metreeca/tray/rdf/graphs/RDF4JSPARQL.html">SPARQL 1.1 Store </a> backend option.</p>
