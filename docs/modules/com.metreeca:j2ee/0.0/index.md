---
title:      "Metreeca J2EE Adapter"
subject:    "Metreeca J2EE Adapter"
project:    "com.metreeca:j2ee"
version:    "0.0"
layout:     module
---

The J2EE module provides an adapter for deploying apps based on the Metreeca [linked data framework](/modules/com.metreeca:link/0.0/) as web 
applications managed by a Servlet 3.0 container.

To deploy a linked data apps as a web application, package it as a `war` archive, e.g. using maven:

```xml
<project>

	<groupId>com.example</groupId>
	<artifactId>app</artifactId>
	<version>1.0</version>
	<packaging>war</packaging>
	
</project>
```

adding Metreeca J2EE gateway to `WEB-INF/web.xml` like:

```xml
<web-app>

	<listener>
		<listener-class>com.metreeca.j2ee.Gateway</listener-class>
	</listener>

	<filter>
		<filter-name>gateway</filter-name>
		<filter-class>com.metreeca.j2ee.Gateway</filter-class>
	</filter>

	<filter-mapping>
		<filter-name>gateway</filter-name>
		<url-pattern>/*</url-pattern>
	</filter-mapping>

</web-app>
```

Custom properties may be defined in `WEB-INF/metreeca.properties`, e.g.

```properties
setup.storage=/opt/example/data
graph=native
```

Toolkits/Services 



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

