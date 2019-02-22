---
title: FAQs and Known Issues
---

# Graph Backend

## SPARQL Transaction Support

The linked data API publishing engine requires transaction support on the SPARQL [graph backend](javadocs/?com/metreeca/tray/rdf/Graph.html) in order to perform automatic validation and post-processing of incoming data: to this date only backends supporting the de-facto standard [RDF4J Server REST API](http://docs.rdf4j.org/rest-api/) are supported. More backend-specific adapters will be introduced in future releases.
