---
title:		Release Notes
excerpt:	Release notes and product roadmap
tags:		Reference
module:     "Metreeca Linked Data Hub"
version:    "0.45"
redirect_to:   "https://github.com/metreeca/metreeca/releases/tag/v0.45"
---

<!-- use absolute liks to https://docs.metreeca.com/ -->

# v0.45+20171222

This is a minor beta release focusing on better interoperability and usability.

# New Features and Improvements

- automated RDF [spooling](https://docs.metreeca.com/quick-start/manage#rdf-spooler) folder for bulk data upload
- dynamic linked data URL [rewriting](https://docs.metreeca.com/quick-start/configure#platform)
- support for Virtuoso remote graph stores
- limited support for remote vanilla SPARQL 1.1 graph stores

# Bug Fixes

- fixed default storage folder configuration: now it's actually defined as the current user working directory
- fixed custom configuration loading for Docker images: now its actually loaded from the custom storage folder
- fixed model-driven stats/items queries: now constraints on shape root are correctly included in generated queries 
- hardened model-driven SPARQL reader: now it's possible to retrieve focus sets with empty projections

# v0.44+20171115

This is a major beta release marking the first general availability of the integrated Metreeca platform.

# New Features and Improvements

- model-driven linked data API publishing server and framework;
- interactive model-driven linked data browsing/editing tools;
- interactive linked data modelling tools.

------

# v0.43+20170826

This is a major beta release focusing on performance and stability.

# New Features and Improvements

- overhauled generated SPARQL templates to greatly improve query performance on large reports/datasets.

# Bug Fixes

- fixed twitching on vertical scrolling in table view.

------

# v0.42+20170531

This is a major beta release starting the transition from Graph Rover to the integrated Metreeca platform.

# New Features and Improvements

- dropped the requirement for CORS support on target SPARQL endpoints: now all requests are routed through a proxy on the delivery server accessible either through `http` or `https`.

------


# Version 0 (Beta)

## 0.42+20170531

This is a major beta release starting the transition toward the integrated Metreeca platform.

New Features and Improvements:

: - dropped the requirement for CORS support on target SPARQL endpoints: now all requests are routed through a proxy on the delivery server accessible either through http: or https:.

## 0.41+20151120

This is a minor beta release improving the user interface for connection to knowledge bases and providing initial on-premises deployment options.

Browser requirements have been upgraded to Firefox 41+, Chrome 45+, Explorer 11, Safari 8.

New Features and Improvements:

: - implemented a dedicated connection dialog supporting opening of specific collections and resources;
- added support for numeric categories in bar charts.

Fixed Bugs:

: - fixed handling of *https:* URLs in the report configuration dialog;
- fixed placement of textual annotations and duplicated plain literals in the linked data navigation view;
- fixed encoding issues when connecting to Sesame 2.8 endpoints.

## 0.40+20150829

This is a minor beta release improving handling of large resource collections.

Browser requirements have been upgraded to Firefox 39+, Chrome 43+, Explorer 11, Safari 8.

Graph Rover is now fully interoperable with [Ontotext](http://ontotext.com/)'s [GraphDB](http://ontotext.com/products/ontotext-graphdb/) v6.4 and [Self-Service Semantic Suite (S4)](http://ontotext.com/products/ontotext-s4/) fully-managed cloud graph database.

New Features and Improvements

:
- improved result sampling to support operations on large resource collections;
- provided feedback for estimated values in facets when sampling.

Fixed Bugs

:
- range facets values are now computed correctly on result sets exceeding 1000 items.

## 0.39+20150804

This is a minor beta release improving embedding support and handling of large resource collections (≫ 10k items).

Browser requirements have been upgraded to Firefox 38+, Chrome 43+, Explorer 11, Safari 8.

New Features and Improvements

:
- introduced result sampling to support operations on large resource collections;
- added the ability to initialize embedded instances from exported reports without relying on the sharing service.

## 0.38+20150609

This is a major beta release introducing support for embedding the tool into third-party HTML pages.

Browser requirements have been upgraded to Firefox 37+, Chrome 42+, Explorer 11, Safari 8.

New Features and Improvements

:
- implemented faceted search, linked data navigation and infographics embedded modes;
- improved handling of large tooltips in infographics views.

Fixed Bugs

:
- fixed overflowing and disappearing tooltips in infographics views.

## 0.37+20150515

This is a major beta release providing initial public support for the DBpedia endpoint.

Browser requirements have been upgraded to Firefox 37+, Chrome 41+, Explorer 11, Safari 8.

Google Analytics statistical tracking was anonymized to comply with upcoming changes in privacy regulations.

New Features and Improvements

:
- improved interoperability with DBpedia/Virtuoso endpoint;
- improved handling of long property lists in the linked data navigation view;
- overhauled history management, which now should work smoothly across all supported browsers;
- improved performance of initial queries.

Fixed Bugs

:
- fixed erratic internal errors and focusing issues when setting the endpoint in the report configuration dialog.

## 0.36+20150427

This is a minor beta release with lots of diffused improvements to linked data navigation and query generation.

Browser requirements have been upgraded to Firefox 36+, Chrome 41+, Explorer 11, Safari 8.

We were again forced to modify the pattern used to [identify key collections](how-tos/improve-user-experience#identify-key-collections), as previous ones didn't behave with inferencing repositories…

New Features and Improvements

:
- introduced basic support for browsing SKOS taxonomies;
- improved linked data navigation over RDFS/OWL inferencing repositories;
- improved path picker search usability;
- provided better feedback for loading pictures.

Fixed Bugs

:
- fixed many rough edges in the query generation engine;
- fixed erratic internal errors when filtering properties in the path picker.

## 0.35+20150316

This is a major beta release, improving linked data navigation and introducing an overhauled query generation engine, which efficiently splits complex high-level queries into sequences of simple SPARQL queries.

Browser requirements have been upgraded to Firefox 35+, Chrome 40+, Explorer 11, Safari 8.

Note that the pattern used to [identify key collections]how-tos/improve-user-experience#identify-key-collections) was changed again since version 0.34: we hope this is the last time a change is required…

New Features and Improvements

:
- completely redesigned the query generation engine;
- optimized queries for singleton sets;
- implemented responsive layout and added new property sections in book view;
- implemented progressive picture loading in board view;
- provided loading feedback for pictures in board and book views.

Fixed Bugs

:
- fixed clearing range limits with empty result set in range filter: now the other limit is preserved;
- fixed failing validation for comma-separated numbers in range filter with Internet Explorer;
- fixed issues with overflowing and unduly clipped path labels.

## 0.34+20150215

This is a major beta release, improving linked data navigation experience.

Browser requirements have been upgraded to Firefox 33+, Chrome 38+, Explorer 11, Safari 8.

New Features and Improvements

:
- completely redesigned linked data navigation layout in book view;
- better integration between linked data navigation and browser history;
- improved image sizing and positioning in board view;
- improved automatic zoom level selection for singleton markers in marker map;
- implemented manual/automatic viewport configuration option in marker map;
- improved series handling in marker map: now georeferenced resources may be used also as main items;
- improved path reporting in path picker: now truncated labels are reported in tooltip and URIs are reported as links;
- enabled native contextual menus.

Fixed Bugs

:
- prevented unwarranted request cancellations when interacting with path picker;
- fixed broken horizontal space management for Firefox 34+;
- fixed renaming of fields included in the 'More' header menu: now labels are editable;
- fixed horizontal resizing behaviour: now fields are moved to the 'More' header menu as required;
- preserved page on history navigation in book view;
- fixed manual viewport adjustment in marker map: now viewport is stable when the dataset changes;
- fixed handling of uppercase file suffixes (e.g. .JPG) in picture URIs.

## 0.33+20141128

This is a major beta release, introducing support for Safari.

New Features and Improvements

:
- support for Safari 8;
- improved dialog visuals;
- improved error reporting.

Fixed Bugs

:
- fixed editing of the endpoint field in the report configuration dialog: now whitespace is ignored;
- fixed vertical sizing issues with the path picker dialog: now long property lists are completely scrollable;
- fixed renaming of fields included in the 'more' menu of the header: now labels are editable;
- fixed sizing issues with tooltips in charts and maps;
- prevented file drops when modal dialogs are open;
- prevented opening of files with unsupported formats.

## 0.32+20141114

This is a major beta release, introducing support for Internet Explorer.

New Features and Improvements

:
- support for Internet Explorer 11;
- improved usability of range facets, with better tabbing/focusing behaviour;
- improved error reporting on local store exceptions.

Fixed Bugs

:
- fixed protocol prefix handling in report info dialog: now doesn't turn *https:* into *http:*;
- fixed naming for CSV export: now the file is named after the report, rather than the first field;
- (Firefox) fixed a regression preventing text selection while editing labels.

## 0.31+20141030

This is a major beta release, introducing support for Google Chrome and a polished user interface, with some new shiny features and lots of minor glitches fixed.

New Features and Improvements

:
- support for Google Chrome 37+;
- restyled cross-browser dialogs and error messages;
- overhauled UI handling of report locking;
- adaptive tile sizing in board view;
- support for item series in line chart;
- improved heuristics for field auto-assignment to roles in charts and maps;
- improved range facets: now users can tab to move from lower to upper value;
- improved pictures handling: now open in a new page;
- improved link tooltips: now shown only for truncated text;
- improved error reporting on timeouts.

Fixed Bugs

:
- fixed tabbing through fields in the report info dialog;
- prevented removal of the last report field;
- fixed 'Duplicate' menu command for facets: now insert duplicate path as facet;
- fixed formatting transients when clearing limits in range facets;
- fixed vertical accordion effects on field rearrangement in table view;
- fixed internal errors raised when double-clicking cells in charts and maps;
- fixed handling of dragged series over multiple roles in charts and maps: now they are accepted only once;
- excluded labels and comments from ordinal series for charts and maps;
- fixed misplaced tooltips in bubble chart;
- fixed stacking order for maps zoom controls: now show below drop-down menus;
- fixed user interface recovery after error: now is not left in an unstable state;
- fixed erratic internal errors on window resize;
- fixed cursor feedback on drag-and-drop operations on Windows: now forbidden signs are not shown.

## 0.30+20141015

This is an incremental beta release, introducing some new features focused on better handling of large results sets (horizontal space management, progressive rendering on scroll, …).

New Features and Improvements

:
- implemented horizontal space management in Table view;
- implemented progressive rendering on scroll in Table view;
- improved name guessing from URIs: now handles hyphenation and camel-case;
- added type-specific rendering support for xsd:date/time/gYear/gMonth/gDay;
- added type-specific rendering support for xsd:anyURI with tel:/callto: scheme;
- simplified name/comment matching in SPARQL query generator;
- improved blank nodes guidelines;
- migrated icons to FontAwesome.

Fixed Bugs

:
- fixed resource retrieval in SPARQL query generator: now handles per-property retrieval limits;
- fixed sorting criterion handling in SPARQL query generator: now ensures strict total ordering to eventually support pagination of large result sets;
- fixed sticky scrolling issues with Firefox in Table/Board views: now vertical scrolling position is reset when rendering reports;
- fixed label normalization: now trailing newlines are removed;
- fixed transients showing 'no options' when clearing range facets;
- fixed focusing issues in combo boxes;
- fixed stacking order for about dialog: now shows in front of functional dialogs;
- fixed warning pages for missing resources / unsupported browsers.

## 0.29+20140721

This is an incremental beta release, introducing some new features, but with an overall focus on stability and bug fixes.

Note that the pattern used to [identify key collections](how-tos/improve-user-experience#identify-key-collections) was changed since version 0.28.

New Features and Improvements

:
- implemented export of datasets as CSV files;
- improved handling of unlabelled URIs: now labels are guessed from well-known slash/hash patterns;
- improved definition of custom key collections, migrating to annotation properties in a custom namespace;
- improved performance of key collection query;
- improved rendering of inverse links in the path picker dialog;
- improved property rendering in the path picker dialog: now tooltips report raw URIs, enabling synonymous properties to be disambiguated.

Fixed Bugs

:
- fixed opening of related resource sets when pivoting on filtered paths: now constraints are not applied to the related set;
- fixed opening of related resource sets from fields different from the left-most one;
- fixed formatting of year values: now thousand separator is not included;
- fixed rendering of inverse properties in book face: now the leading \^ is not shown;
- fixed compatibility issues with dydra.com SPARQL hosting service;
- fixed sizing issues in the path picker dialog
- fixed drag&drop issues when reordering chart/maps series on Firefox for Windows.

## 0.28+20140331

This is the initial public beta release. The application is fairly stable and (with the exception of charting and mapping views) has endured a year‑long private beta cycle: expect however some functional and graphical changes until the first release in the 1.x series…

New Features and Improvements

:
- implemented client-side query caching, improving responsiveness and substantially reducing endpoint load;
- implemented interactive charting and mapping views;
- improved automatic response from textual fields in facets.

Fixed Bugs

:
- prevented unwarranted repeated queries when updating facet values;
- prevented unwarranted warning message when closing unmodified report info dialogs for locked reports;
- fixed handling of *https:* SPARQL endpoint urls in the report info dialog;
- fixed broken cut & paste on field label editing;
- prevented report from reverting to the default view during textual search;
- fixed broken input validation patterns for range facets;
- fixed document titling: now includes app name only for anonymous documents.

## 0.27+20140121

This is a bug fixes release, focused on the initial public beta of the companion Metreeca Path Finder app.

New Features and Improvements

:
- published tutorial at <http://docs.metreeca.it/>.
- improved formatting of numeric placeholders and values in facets, limiting the maximum number of decimal digits;
- hardened query engine against malformed SPARQL endpoint responses.

## 0.26+20131112

This is the initial private beta release. It includes a fairly stable SPARQL query engine derived from a well tested internal demonstrator, but still misses charting and mapping views.

New Features and Improvements

:
- provided human-readable name when saving exported documents;
- hardened document export format against XML reformatting;
- improved loading feedback;
- upgraded supported Firefox version to 26+;

Fixed Bugs

:
- fixed document export: now doesn't clip exported documents;
- fixed handling of trailing slash and /index.html on [Google Application Engine](https://groups.google.com/forum/?fromgroups=#!topic/google-appengine/QTVjd9uDiXI);
- fixed error 404 redirection on Google Application Engine;
