---
title:  How to Fix sun.security.provider.certpath.SunCertPathBuilderException
---

# Test

```java
public static void main(final String... args) throws IOException {

    final String host="www.serviziocontrattipubblici.it";
    final int port=443;

    try (

            final Socket socket=SSLSocketFactory.getDefault().createSocket(host, port);

            final InputStream input=socket.getInputStream();
            final OutputStream output=socket.getOutputStream()

    ) {

        output.write(1);

        while ( input.read() >= 0 ) {}

    }

}
```

# Diagnosis

- the target host likely doesn't include an intermediate certificate
  - test with https://www.ssllabs.com/ssltest/analyze.html

# Solutions

## Enable Automatic Download

See https://security.stackexchange.com/a/168061

```
-Dcom.sun.security.enableAIAcaIssuers=true
```

```java
static { System.setProperty("com.sun.security.enableAIAcaIssuers", "true"); }
```

## Manually Add Certificate to Keystore

- download certificate chain
  - expand certification paths
  - download clicking icons at top right
- for each missing certificate
  - identify cert alias
  - copy missing certificates from chain to `public.crt`

```shell
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_231.jdk/Contents/Home/ sudo $JAVA_HOME/bin/keytool \
    -import -alias "Thawte RSA CA 2018" -file public.crt \
    -keystore $JAVA_HOME/jre/lib/security/cacerts -storepass changeit
```
