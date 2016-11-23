# XSLTiny
## Presentation
This is a simplified XSL-like transforming language for xml translation to different dialects

## How it works
The target document format which may be expressed in different dialects (or languages) is translated from a source document which contains the pure information.
The format of the source file is defined by a provided document containing the context of the transformation.
Then every dialect (or language) has its document containing the transformation rules to obtain different pieces of information: the document is transformed into a map of xslt stylesheets that we apply to get a map of translations.

## Why using XSLTiny
The purpose is to facilate the creation of many transformations for the same context.

# Use
## Download
The released versions are available on [xsltiny.sf.net](https://sourceforge.net/projects/xsltiny/) repository or as a maven dependency on [mymavenrepo.com](https://mymavenrepo.com/repo/0qo9dAdBcLRywnctciNm/)

## Run on command line
```shell
java -jar xsltiny.jar -c context.xml -t dialect.xml -d data.xml
```

## Integrate into your maven project
```XML
  <repositories>
    <repository>
      <id>myMavenRepo.read</id>
      <url>https://mymavenrepo.com/repo/0qo9dAdBcLRywnctciNm/</url>
    </repository>
    ...
  </repositories>
  <dependencies>
    <dependency>
      <groupId>net.sf.xsltiny</groupId>
      <artifactId>xsltiny</artifactId>
      <version>1.0</version>
      <scope>compile</scope>
    </dependency>
    ...
  </dependencies>
```

# Examples
[osql.sf.net](https://sourceforge.net/projects/osql/) is an example of use of xsltiny