---
title:      Installation Handbook
excerpt:    System requirements, installation and upgrade procedures
tags:       Handbook
module:     "Metreeca Linked Data Hub"
version:    "0.46"
---

| system | requirements                             |
| ------ | ---------------------------------------- |
| client | Metreeca interactive tools are web applications: to run them you need the latest version of a supported web browser ([Firefox](http://www.mozilla.org/firefox/new/), [Chrome](https://www.google.com/chrome/), [Safari](https://www.apple.com/safari/), [Edge](http://microsoft.com/en-us/windows/microsoft-edge), [Explorer](http://windows.microsoft.com/en-us/internet-explorer/download-ie)); unsupported browsers may work as well, but they are not tested and your mileage may vary… |
| server | system requirements vary according to the following deployment options |

# Docker Image

<!-- align Installation Handbook / Docker README -->
<!-- use absolute links to http://docs.metreeca.com/quick-start/ -->

| system | requirements                             |
| ------ | ---------------------------------------- |
| client | the latest version of a modern web browser |
| server | an updated [Docker](https://www.docker.com/get-docker) installation |

Docker [images](https://hub.docker.com/r/metreeca/metreeca/) are the easiest way to deploy Metreeca to your local server or to a cloud service: to get started:

- set a password for the *admin* system administrator user

  ```sh
  KEY="<your admin password here>"
  ```

- run a Metreeca container:

  ```sh
  docker run --detach \
    --name metreeca \
    --publish 8080:8080 \
    --env KEY="${KEY}" \
    metreeca/metreeca:latest
  ```

- open the platform interface at http://localhost:8080/ and sign in as *admin*

- get started going through the tutorials at https://www.metreeca.com/quick-start

- delve deeper with the documentation at http://docs.metreeca.com/

**Warning** / *The current version of the Docker image contains a preview release of the platform with limited user authentication and authorization capabilities: administration tools,  unsafe HTTP methods on linked data REST APIs (POST/PUT/DELETE) and SPARQL endpoints are restricted to system administrators, but fine-grained access control and user-dependent views aren't yet supported. More to come in the next release…*

## Environment Variables

The following environment variables may be set with the `--env` Docker option to tune the platform behaviour, like:

```sh
docker run --env <name>=<value> …
```

| name  | value                                    |
| :---- | :--------------------------------------- |
| JAVA  | space separated command line options for the Java virtual machine |
| SETUP | space separated [configuration](http://docs.metreeca.com/quick-start/configure) properties (`<property>=<value>`) |
| KEY   | the password for the system administrator user (*admin*) |
| BASE  | the absolute canonical server base URL   |

## Host Volumes

To persist platform data on a host volume, mount it under the `/opt/metreeca/data` directory  in the platform container using either the `--volume` or the `--mount` Docker option, like:

```sh
docker run --volume </host/path>:/opt/metreeca/data …
```

## Memory Allocation

By default, the Java virtual machine running the platform is allocated a limited amount of memory, to make it interact gracefully with the Docker container (see for instance https://developers.redhat.com/blog/2017/03/14/java-inside-docker/).

To increase memory limits, configure the following command line options using the `JAVA` environment variable, like:

```sh
docker run --env 'JAVA=-Xmx2g'…
```

| option       | value                            |
| :----------- | :------------------------------- |
| -Xmx*{size}* | the maximum heap size (in bytes) |
| -Xss*{size}* | the thread stack size (in bytes) |

Append to *{size}* the letter `k` or `K` to indicate KB, `m` or `M` to indicate MB, `g` or `G` to indicate GB. 

## Custom Configuration

To define a [custom configuration](http://docs.metreeca.com/quick-start/configure) for the platform either define the relevant properties as Java system properties in the `JAVA` [environment variable](#environment-variables), like:

```sh
docker run --env 'JAVA=-D<property>=<value> ' …
```

or create and edit a custom configuration file named `setup.properties` in the root of the host volume to be [mounted](#host-volumes) to the Docker container.

## Bulk Data Management

Bulk data management is supported through the following standard SPARQL 1.1 [endpoints](http://docs.metreeca.com/quick-start/manage#sparql-endpoints).

| endpoint                                 | URL                          |
| ---------------------------------------- | ---------------------------- |
| [SPARQL 1.1 Update](http://www.w3.org/TR/sparql11-protocol) | http://localhost:8080/sparql |
| [SPARQL 1.1 Graph Store](http://www.w3.org/TR/sparql11-http-rdf-update) | http://localhost:8080/graphs |

Update operations on all endpoints are always restricted to system administrators; retrieval operations are restricted to system administrators unless otherwise [configured](http://docs.metreeca.com/quick-start/configure#sparql-endpoints).

## Bulk Data Upload

Bulk data upload is also supported through the automated RDF [spooling](http://docs.metreeca.com/quick-start/manage#rdf-spooler) folder at `/opt/metreeca/data/spool`/. To upload an RDF file just copy it to the spooling folder and wait for the spooler process to pick it up:

```sh
docker cp <rdf-file> <container-name>:/opt/metreeca/data/spool/
```

If you [mounted](#host-volumes) a host volume, just copy RDF files to the spooling folder on the host:

```sh
cp <rdf-file> </host/path>/spool/
```

The spooler accepts both plain RDF files and zip/tar archives containing RDF files, gracefully handling gzip/bzip2 compression.

After the file is uploaded, it's automatically removed from the spooling folder. If something went wrong during processing, the file is marked as ignored and preserved for post-mortem analysis.

**Warning** / *When launching a new container wait for the spooling folder to be created during process initialization before attempting a bulk upload. Be also aware that the spooler process may require some seconds before picking up new files.*

## External Graph Database

A wide range of third-party RDF graph databases, supporting the extended  [RDF4J Server REST API](http://docs.rdf4j.org/rest-api/) or other proprietary wire protocols, can be [configured](http://docs.metreeca.com/quick-start/configure#graph-backend]) as embedded or external graph storage backends.

# Web Application

<p class="warning">Work in progress…</p>

# Stand-Alone Server

<p class="warning">Work in progress…</p>
