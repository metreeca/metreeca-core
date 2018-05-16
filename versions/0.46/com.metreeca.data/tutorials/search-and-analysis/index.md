---
title:	    Search and Analysis Tutorial
excerpt:    Hands-on guided tour of self-service search and analysis tools
tags:	    Tutorial
module:     "Metreeca Linked Data Hub"
version:    "0.46"
---

# Getting Started

This example-driven tutorial introduces the self-service search and analysis tools of the [Metreeca](https://www.metreeca.com/platform/) model-drive linked data platform. Basic familiarity with [business intelligence](http://en.wikipedia.org/wiki/Business_intelligence) and  [linked data](http://www.w3.org/standards/semanticweb/data) concepts is useful, but not required.

To get started open [https://demo.metreeca.com/apps/self/](https://demo.metreeca.com/apps/self/#endpoint=https://demo.metreeca.com/sparql) with a [supported](../../handbooks/installation#system-requirements) web browser and connect to a SPARQL endpoint.

The tutorial refers to the [https://demo.metreeca.com/sparql](https://demo.metreeca.com/apps/self/#endpoint=https://demo.metreeca.com/sparql) endpoint, which serves a semantic version of the [BIRT](http://www.eclipse.org/birt/phoenix/db/) sample dataset, cross-linked to [GeoNames](http://www.geonames.org/) entities for cities and countries. The BIRT sample is a typical business database, containing tables such as *offices*, *customers*, *products*, *orders*, *order lines*, … for *Classic Models*, a fictional world-wide retailer of scale toy models.

Should you want to try out Metreeca on your own datasets, for best results make sure to review [data usability](../../how-tos/improve-user-experience#data-usability) guidelines.

You may also try out Metreeca on DBpedia and other LOD endpoints: if you feel like, have a look at [https://www.metreeca.com/demos](https://www.metreeca.com/demos#analytics).

<p class="warning">The demo server is hosted on a cloud service: it is not expected to provide production-level performance and may experience some delays during on-demand workspace initialization.</p>

## Connecting to an Endpoint

Enter the URL of the endpoint ([https://demo.metreeca.com/sparql](https://demo.metreeca.com/apps/self/#endpoint=https://demo.metreeca.com/sparql)) in the *base* field of the report info dialog (or just click the link if you feel lazy ;-): subsequent queries will automatically configure themselves with the least recently used endpoint.

![Connecting to an Endpoint](images/connecting-to-an-endpoint.png)

To connect to a different endpoint, click on the *Connect To…* command in the *Manage* menu of the toolbar.

![connect-to-command](images/connect-to-command.png)

## Opening a Collection

![opening-a-collection](images/opening-a-collection.png)

# Fields and Facets

## Adding Fields

![adding-fields](images/adding-fields.png)

## Adding Facets

![adding-facets](images/adding-facets.png)

## Turning Fields into Facets

![turning-fields-into-facets](images/turning-fields-into-facets.png)

## Filtering Matches

![filtering-matches](images/filtering-matches.png)

As you enter constraints, the other facets automatically update their values to show the available options and the current range.

## Sorting Fields

![sorting-fields](images/sorting-fields.png)

## Turning Facets into Fields

![turning-facets-into-fields](images/turning-facets-into-fields.png)

## Rearranging Fields and Facets

![rearranging-fields-and-facets](images/rearranging-fields-and-facets.png)

# Linked Data Navigation

## Browsing Linked Resources

![browsing-linked-resources](images/browsing-linked-resources.png)

## Browsing Linked Resource Sets

![browsing-linked-resource-sets](images/browsing-linked-resource-sets.png)

# Set-Based Navigation

## Opening a Related Set

![opening-a-related-set](images/opening-a-related-set.png)

## Traversing Graph Paths

![traversing-graph-paths](images/traversing-graph-paths.png) In the most general case, fields, facets and related sets are defined by **property paths**, defined from a starting set using the property selection dialog. The selection interface is divided into three sections:

-   middle › starting set and properties included in the current path;
-   right › forward properties that may be appended to the current path;
-   left › backward properties that may be appended to the current path.

As you append properties to the path, panels are updated to show the current selection and available further steps.

![traversing-graph-paths-2](images/traversing-graph-paths-2.png)

# Summaries and Transforms

## Summarizing Values

![summarizing-values](images/summarizing-values.png)

## Transforming Values

![transforming-values](images/transforming-values.png)

![transforming-values-2](images/transforming-values-2.png)

## Working with Computed Values

![working-with-computed-values](images/working-with-computed-values.png)

# Charts and Maps

## Visualizing Sets as Charts

![visualizing-sets-as-charts](images/visualizing-sets-as-charts.png)

## Configuring Charts

![configuring-charts](images/configuring-charts.png)

## Visualizing Sets on Maps

![visualizing-sets-on-maps](images/visualizing-sets-on-maps.png)

![visualizing-sets-on-maps-2](images/visualizing-sets-on-maps-2.png)

## Managing Data Series

![managing-data-series](images/managing-data-series.png)

![managing-data-series-2](images/managing-data-series-2.png)

![managing-data-series-3](images/managing-data-series-3.png)

## Exploring Chart Items

![exploring-chart-items](images/exploring-chart-items.png)

# Browsing History

Linked Data and set-based navigation is recorded in the browser history.

![browsing-history](images/browsing-history.png)

# Managing Reports

Reports are persistently archived on your PC using the local storage provided by the browser.

## Saving Reports

![saving-reports](images/saving-reports.png)

## Exporting Reports

![exporting-reports](images/exporting-reports.png)

## Locking Reports

![locking-reports](images/locking-reports.png)

# Exporting Data

## Exporting Tables

![exporting-tables](images/exporting-tables.png)

Note that some versions of Excel won't correctly open the exported CSV file: if this is the case, import it manually from Excel *Data* menu.
