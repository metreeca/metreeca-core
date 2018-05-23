---
caption:    "${module.caption}"
project:    "${module.project}"
version:    "${module.version}"
layout:     module
---

The J2EE module provides an adapter for deploying apps based on the Metreeca [linked data framework](/modules/com.metreeca:link/${module.version}/) as web 
applications managed by a Servlet 3.1 container.

To deploy a linked data apps as a web application, package it as a `war` archive adding a dependency from the J2EE module, e.g. using maven:

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




adding Metreeca J2EE gateway to `WEB-INF/web.xml` like:

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

