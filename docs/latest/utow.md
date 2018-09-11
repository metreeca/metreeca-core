---
title:      "${project.name}"
excerpt:    "${project.description}"
---

The `utow` module provides an adapter for deploying apps based on the Metreeca [linked data framework](/modules/com.metreeca:link/${module.version}/) as [Undertow](http://undertow.io/) handlers.

# Getting Started

To deploy a linked data app as a web application, package it as a `war` archive adding:

- compile dependencies from the platform [linked data framework](/modules/com.metreeca:link/${module.version}/) and other required platfom components;
- a runtime dependency from the J2EE module;
- a provided dependency from the Servlet 3.1 API.

Using maven: 

```xml
<project>

    <groupId>com.example</groupId>
    <artifactId>app</artifactId>
    <version>1.0</version>
	    
    <dependency>
        <groupId>com.metreeca</groupId>
        <artifactId>link</artifactId>
        <version>${module.version}</version>
    </dependency>
 
    <dependency>
        <groupId>com.metreeca</groupId>
        <artifactId>utow</artifactId>
        <version>${module.version}</version>
        <scope>runtime</scope>
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
