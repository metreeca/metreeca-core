# Local Test Endpoints

## RDF4J 2

|   status | ***                                      |
| -------: | :--------------------------------------- |
|     home | http://rdf4j.org/                        |
|     docs | http://docs.rdf4j.org/                   |
|   issues | https://github.com/eclipse/rdf4j/issues  |
| endpoint | http://localhost:8080/rdf4j-server/repositories/birt |

- connect to http://localhost:8080/rdf4j-workbench/
- create birt repository
  - template `Native Java Store`
- uplooad `test` dataset
  - select file
  - clear context
  - select n-triples format


## Stardog Community 5

| status   | ***                              |
| -------- | -------------------------------- |
| home     |                                  |
| docs     |                                  |
| issues   |                                  |
| endpoint | http://localhost:5820/birt/query |

- **allocate at lesat 4GB to the docker engine**
- connect to http://localhost:5820/
- select _Database_ from the toolbar
- create `birt` database (repository)
  - Reasoning type: NONE
- select `Query` from the database toolbar
- select `Data / Add` from the main toolbar
- upload test dataset


## GraphDB Free 8

| status   | ***                                      |
| -------- | ---------------------------------------- |
| home     |                                          |
| docs     |                                          |
| issues   |                                          |
| endpoint | <http://localhost:7200/repositories/birt> |

- connect to http://localhost:7200/
- create `birt` repository
  - ruleset: no inference
- connect to repository from top-right menu
- import test dataset from top-left sidebar


## Blazegraph 2

|   status | ***                                      |
| -------: | ---------------------------------------- |
|     home | https://www.blazegraph.com/              |
|     docs | <https://wiki.blazegraph.com/>           |
|   issues | https://jira.blazegraph.com/issues       |
| endpoint | http://localhost:9999/blazegraph/namespace/birt/sparql |

- connect to http://localhost:9999/blazegraph/
- open the `namespaces` tab from the toolbar
- create `birt` namespace (repository)
- mark as in `use`
- open the `update` tab from the toolbar
- drag test dataset file to the update text area or select it using controls at bottom
- upload test dataset


## Virtuoso OpenSource 7

|   status | **                                       |
| -------: | ---------------------------------------- |
|   issues | minor loss of functionality on transformed fields/facets due to broken numeric/temporal functions |
|     home | https://github.com/openlink/virtuoso-opensource |
|     docs | http://vos.openlinksw.com/ (Open-Source Edition) |
|   issues | https://github.com/openlink/virtuoso-opensource/issues |
| endpoint | http://localhost:8890/sparql             |

- connect to ​http://localhost:8890/
- log in conductor as `dba/dba` from top-left sidebar
- select `linked data` and then `quad store upload` from the toolbar
- select and upload test dataset file


# Cloud Test Endpoints

## Ontotext S4

| status   | ***                                      |
| -------- | ---------------------------------------- |
| home     |                                          |
| docs     |                                          |
| issues   |                                          |
| endpoint | <https://rdf.s4.ontotext.com/4830526919/work/repositories/birt> |

- connect to https://console.s4.ontotext.com/#/database
- create and open `work` database
- create and populate `birt` repository (like local Graph DB)
- from the console select database and repository
- make public


## Ontotext Cloud

- max 1 repository per db ;-(

- connect to https://cloud.ontotext.com/
- create and launch `work` database
- create and populate `birt` repository (like local Graph DB)


## Dydra

| status   | ***                                     |
| -------- | --------------------------------------- |
| home     |                                         |
| docs     |                                         |
| issues   |                                         |
| endpoint | <http://dydra.com/metreeca/birt/sparql> |

