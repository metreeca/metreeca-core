---
title:  
excerpt:
---

The J2EE module provides an adapter for deploying apps based on the Metreeca [linked data framework](/modules/com.metreeca:link/${module.version}/) as web 
applications managed by a Servlet 3.1 container.

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
    <packaging>war</packaging>

    <dependency>
        <groupId>com.metreeca</groupId>
        <artifactId>j2ee</artifactId>
        <version>${module.version}</version>
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
