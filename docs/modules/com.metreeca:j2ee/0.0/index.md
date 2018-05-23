---
caption:    "Metreeca J2EE Adapter"
project:    "com.metreeca:j2ee"
version:    "0.0"
layout:     module
---

The J2EE module provides an adapter for deploying apps based on the Metreeca [linked data framework](/modules/com.metreeca:link/0.0/) as web 
applications managed by a Servlet 3.1 container.

# Getting Started

To deploy a linked data apps as a web application, package it as a `war` archive adding:

- compile dependencies from the platform [linked data framework](/modules/com.metreeca:link/0.0/) and other required platfom components;
- a runtime dependency from the J2EE module;
- a provided dependency from the Servlet 3.1 API.

Using maven:

```xml
<project>

	<groupId>com.example</groupId>
	<artifactId>app</artifactId>
	<version>1.0</version>
	<packaging>war</packaging>
	    
    <dependency>
        <groupId>com.metreeca</groupId>
        <artifactId>link</artifactId>
        <version>0.0</version>
    </dependency>
 
    <dependency>
        <groupId>com.metreeca</groupId>
        <artifactId>j2ee</artifactId>
        <version>0.0</version>
        <scope>runtime</scope>
    </dependency>
    
    <dependency>
        <groupId>javax.servlet</groupId>
        <artifactId>javax.servlet-api</artifactId>
        <version>3.1.0</version>
        <scope>provided</scope>
    </dependency>

</project>
```


Then include and configure Metreeca J2EE gateway in `WEB-INF/web.xml` like:

```xml
<web-app>
    
    <filter>
        <filter-name>metreeca</filter-name>
        <filter-class>com.metreeca.j2ee.Gateway</filter-class>
    </filter>

    <filter-mapping>
        <filter-name>metreeca</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>

</web-app>
```

## Application Components

Standard platform-provided [linked data services](/modules/com.metreeca:link/0.0/apidocs/com/metreeca/link/services/package-frame.html) and custom application-provided [toolkits](/modules/com.metreeca:link/0.0/apidocs/com/metreeca/link/Toolkit.html) and [services](/modules/com.metreeca:link/0.0/apidocs/com/metreeca/link/Service.html) are listed in the `com.metreeca.link.Tookit`  and `com.metreeca.link.Service` [service loader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) provider configuration files in the ``META-INF/services/` resource directory of the application, as discussed in the [linked data framework](/modules/com.metreeca:link/0.0/) docs.

For multi-module Maven projects with overlaid WARs, configure the [WAR plugin](https://maven.apache.org/plugins/maven-war-plugin/) to create separate JARs for each module, in order to avoid possible clashes among multiple provider configuration files from different WAR modules.

```xml
<plugin>

    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-war-plugin</artifactId>
    <version>3.2.0</version>

    <configuration>
        <archiveClasses>true</archiveClasses>
    </configuration>

</plugin>
```

## Configuration Properties

[Configuration properties](/modules/com.metreeca:tray/0.0/references/configuration) for standard and custom components may be defined in the `WEB-INF/metreeca.properties` configuration file, e.g.

```properties
setup.storage=/opt/example/data
graph=native
```