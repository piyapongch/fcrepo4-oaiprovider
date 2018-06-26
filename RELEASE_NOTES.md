# RELEASE NOTES - OAI PROVIDER

# Table of Contents

* [General v4.7.x Notes](#v4.7.x)
* [FAQ / Notes](#faq)
* [Release v4.7.4.3 Notes](#v4.7.4.3)
* [Release v4.7.4.4 Notes](#v4.7.4.4)
* [Release v4.7.4.5 Notes](#v4.7.4.5)
* [Release v4.7.4.6 Notes](#v4.7.4.6)
* [Release v4.7.4.7 Notes](#v4.7.4.7)
* [Release v4.7.4.8 Notes](#v4.7.4.8)
* [Release v4.7.4.9 Notes](#v4.7.4.9)
* [Release v4.7.5.0 Notes](#v4.7.5.0)



<a name="v4.7.x"/>

## General Notes - v4.7.x Fcrepo4 OAI Provider


### Archetecture

* OAI Provider is a Java-based extension of Fedora Commons (Fcrepo) version 4.7.x.
* Build script packages, via Maven, the OAI Provider Java Jar into a War file containing the OAI Provider along with a full Fedora Commons release and associated config files tailored toward the UAL environment.
* OAI Provider interacts with Fedora below the Fedora Web API and directly calls Fcrepo session, Fcrepo service, and ModeShape layers (including the use of JCR2-SQL).
* Utilizes JAX-B to autogenerate Java Class files representing the output (OAI DC, OAI, ETDMS, And ORE) XML metadata and includes serialization routines for the content. 
* OAI Provider Specifications based on:
    * Open Archives Initiative (OAI) Protocol for Metadata Harvesting (OAI-PMH) (i.e., includes the web API protocal specification)
        * https://www.openarchives.org/pmh/
        * http://www.openarchives.org/OAI/openarchivesprotocol.html
    * Metadata output
        * oai_dc - 
        * oai_etdms - 
        * ORE - http://www.openarchives.org/ore/1.0/atom 
    * Built with the Feburary 2018 UofA Libraries Metadata specs / data dictionary
        * https://github.com/ualbertalib/metadata/commit/9d399072010c51ab066c56b4252595695355d049#diff-040171f6a9f356b320be186e0c34d6e1
        * https://github.com/ualbertalib/metadata/tree/9d399072010c51ab066c56b4252595695355d049
        * https://github.com/ualbertalib/metadata/tree/9d399072010c51ab066c56b4252595695355d049


### Build/Installation


#### Build

* provides Maven-based build 
    * compiles OAI Provider code into a Jar (`mvn clean; mvn package;`)
    * downloads Fedora Commons War file and adds the following items before rebuilding
        * OAI Provider Jar
        * config files: OAI Provider, Fedora (Modeshape config, indexing, and fedora config) - oai.xml, master.xml, repository.json (file-simple, jdbc-postgres), activemq.xml (modifed) node-types_ual.cnd), web.xml (allow /oai public access while password protecting other url patterns)
        * ModeShape libraries to allow the usage of Lucene indexing within ModeShape (e.g., allowing indexing of multi-valued properties, for example collection membership (https://docs.jboss.org/author/display/MODE50/Lucene)
    * Builds a War file named `fcrepo-oaiprovider-4.7.4.x.war` where `4.7.4` is the Fcrepo version with the last dot number, 'x',  represting the OAI Provider version


#### Installation / Deployment

* Build script packages (e.g., mvn clean; mvn package;) all necessary config and library files such that one can drop-in the resulting `fcrepo-oaiprovider-4.7.4.3.war` into a Tomcat instance with the following property passed into Tomcat at start-up
    * -Dfcrepo.modeshape.configuration=classpath:/config/file-simple/repository.json 
    * -Dfcrepo.modeshape.index.directory=/fcrepo4-data/modeshape.index 
    * -Dfcrepo.home=/fcrepo4-data 
    * Connection to the activemq triplestore connection broker
        * -Dfcrepo.triplestore.activemq.broker=
    * Use Saxon as the XML processor
        * -Djava.xml.transform.TransformerFactory=net.sf.saxon.TransformerFactoryImpl 
    * Fedora/Modeshape performance enhancement (optional) [reference](https://wiki.duraspace.org/display/FEDORA4x/Configuration+Options+Inventory#ConfigurationOptionsInventory-Parallelstreamprocessing)
        * -Dfcrepo.streaming.parallel=true
    * Fedora/Modeshape performance enhancement (optional) [reference](https://wiki.duraspace.org/display/FEDORA4x/Configuration+Options+Inventory#ConfigurationOptionsInventory-cacheSize) - unsure the optimal value - this is the default
        * Dfcrepo.modeshape.cacheSize=10000
    * build package `war` file contains the config files required with server specific properties assigned /etc/tomcat/conf.d/jvm_opts.conf via the -Dproperty=value syntax (e.g., -Dfcrepo.triplestore.activemq.broker)


* Alternative Modeshape persistance layer - PostgreSQL JDBC in `src/main/resources/config/jdbc-postgresql/`

* Alternatively, as a Docker image described here: https://github.com/ualbertalib/fcrepo4-oaiprovider/issues/27    

* OAI-PMH validator - testing script: 
    * https://github.com/zimeon/oaipmh-validator.git 
* Alternative testing tool to harvest all records
    * https://github.com/lmika/oaipmh


#### Updating to a new Fcrepo release (minor release)
* update `pom.xml`
    * change version from `4.7.4` to version `4.7.x`
    * review the Fcrepo release notes: https://wiki.duraspace.org/display/FF/Fedora+4.7.4+Release+Notes 
        * look for ModeShape version changes in the JIRA tickets - if changed then update the following: 
            * modeshape-lucene-index-provider-5.3.0.Final.jar
            * lucene-core-6.0.1.jar
            * lucene-analyzers-common-6.0.1.jar
            * more info might appear in the release notes for the ModeShape version (http://docs.jboss.org/modeshape/)

* Example: bump to version 4.7.5 Fcrepo
  * In the `pom.xml` change the following properties
    * fcrepo.version: bump Fcrepo to 4.7.5 - https://wiki.duraspace.org/display/FF/Fedora+4.7.5+Release+Notes
    * modeshape.version: bump to 5.4.0.Final as per Fcrepo release 
    * lucene.version: bump lucene to 6.4.1 as per ModeShape release


#### OAI Provider usage examples

* Via the web Browser plugin https://restlet.com
    * config: https://github.com/ualbertalib/fcrepo4-oaiprovider/tree/master/config/restlet
* General
    * http://localhost:8080/fcrepo/rest/oai?verb=Identify
    * http://localhost:8080/fcrepo/rest/oai?verb=ListSets 
    * http://localhost:8080/fcrepo/rest/oai?verb=ListIdentifiers&metadataPrefix=oai_dc
    * http://localhost:8080/fcrepo/rest/oai?verb=ListRecords&metadataPrefix=oai_dc
    * http://localhost:8080/fcrepo/rest/oai?verb=GetRecord&metadataPrefix=oai_etdms&identifier={replace_me_with_a_record_identifier}
    * http://localhost:8080/fcrepo/rest/oai?verb=ListRecords&metadataPrefix=oai_dc&set=3fedbfcc-d99e-40ca-b66b-2c7c7433e77b/6cddc888-91f8-4f8c-aa54-49b1b597891e
    * http://localhost:8080/fcrepo/rest/oai?verb=ListRecords&metadataPrefix=oai_dc&from=2002-02-05&until=2002-02-06T05:35:00Z 

<a name="faq"/>

## FAQ

1. No OAI Provider results returned by `ListRecords` (Fcrepo 4.7.x)?
    * Without indices the OAI Provider returns zero records
    * check for completion of Modeshape index building (requires around 3 hours 2018-05-01)
    * check if index building was interupted as the index building may not restart (`fcrepo-repo.log` in debug mode will log: "[modeshape-reindexing-5-thread-1] DEBUG - SLF4JLoggerImpl.debug(102): Enabled index 'hasModel' from provider 'local' in workspace 'default' after reindexing has completed" when complete and "Disabling index 'hasModel' from provider 'local' in workspace 'default' while it is reindexed. It will not be used in queries until reindexing has completed" while indexing occurs
    * to check if indexing is working, the `fcrepo-oai.log` in debug logging level displays the query plan identifying the indices utilized.

2. Warning: changing Modeshape index in `repository.json` config from `local` to `lucene` in Fcrepo 4.7.4 will cause the JRC-SQL `OR` condition to fail be returning results satisfying only one side of the "or" condition - https://github.com/ualbertalib/fcrepo4-oaiprovider/blob/b6b4b4f4f77f27fa38ff0070e7f157d7fe8c2a39/src/main/java/org/fcrepo/oai/service/OAIProviderService.java#L1200-L1204

3. Warning: changing Modeshape index in `repository.json` config from `local` to `lucene` in Fcrepo 4.7.4 - unable to switch the index back to `local` from `lucene` as the property stored within PostgreSQL presistance layer "seemed" to override the `respository.json` config. 

4. Certain Modeshape index providers (e.g., `local`) only support single-valued properties [reference](https://docs.jboss.org/author/display/MODE50/Local)

5. Warning: due to the usage of system defined namespace prefixes instead predefined namespace prefixes within a [CND file](https://wiki.duraspace.org/display/FEDORA4x/Best+Practices+-+RDF+Namespaces) before content migrated into Fedora, ns00x patterned namespace prefixes were assigned by Fedora/Modeshape 4.7.4. The OAI Provider uses the `{uri}property` syntax to idenify the properies (e.g., [`{http://purl.org/dc/terms/}accessRights`](https://github.com/ualbertalib/fcrepo4-oaiprovider/blob/fcrepo4-oaiprovider-4.7.4.5/src/main/resources/config/jdbc-postgresql/repository.json). Attempting to change via unregister/register function lead to errors [reference](https://github.com/ualbertalib/fcrepo4-oaiprovider/blob/cba619e8c466c64d6bc78c8563a8d5c134c30738/src/main/java/org/fcrepo/oai/service/OAIProviderService.java#L254-L274) as the functions only changed properties not values such as `The namespace with URI "http://projecthydra.org/works/models#" cannot be unregistered because the node type "{http://projecthydra.org/works/models#}Thesis" uses it.`

6. Language codes: warning, if additional languages are added or changes to translation from code to string literal, modify oai.xml, iso 639-2 mapping

6. Type codes: warning, if additional types are added or changes to translation from code to string literal, modify oai.xml, dc type mapping

<a name="v4.7.4.3"/>

## v4.7.4.3 Fcrepo4 OAI Provider

### Overview

#### What is in this release

OAI provider is a Java module that extends Fcrepo by integrating with Modeshape and Fcrepo (i.e, at a level below the web API). The Maven build infrastructure packages the OAI Provider modules/jar with the complete Fcrepo WAR file. To install the OAI Provider means that the Fcrepo WAR file within Tomcat webapps directory is replaced. 

What is included within an OAI Provider Release as defined by the [OAI Provider Maven build](https://github.com/ualbertalib/fcrepo4-oaiprovider/blob/b5babd5b60dfb3ba56517dcfb3d37631e72275ad/pom.xml)?

- complete release of Fcrepo

- OAI Provider libraries and supporting libraries

- Production config files including:
kj
  - [access control](https://github.com/ualbertalib/fcrepo4-oaiprovider/blob/b5babd5b60dfb3ba56517dcfb3d37631e72275ad/src/main/webapp/WEBINF/web.xml)

  - [ModeShape index](https://github.com/ualbertalib/fcrepo4-oaiprovider/blob/b5babd5b60dfb3ba56517dcfb3d37631e72275ad/src/main/resources/config/minimal-default/repository.json) as defined by the start-up parameters `-Dfcrepo.modeshape.configuration` (e.g., [dockerfile](https://github.com/jefferya/docker-fcrepo4/blob/4.7.4/4.7/Dockerfile) ) and ModeShape performance parameters (e.g., `cacheSize` and node property index)

  - [pre-defined namespaces](https://github.com/ualbertalib/fcrepo4-oaiprovider/blob/b5babd5b60dfb3ba56517dcfb3d37631e72275ad/src/main/resources/config/minimal-default/node-types_ual.cnd) otherwise Fcrepo startup errors on Modeshape index due to undefined namespaces

  - [parameters related to OAI Provider](https://github.com/ualbertalib/fcrepo4-oaiprovider/blob/b5babd5b60dfb3ba56517dcfb3d37631e72275ad/src/main/resources/spring/oai.xml)

  - [fcrepo startup parameter](https://github.com/jefferya/docker-fcrepo4/blob/4.7.4/4.7/Dockerfile) including `-Dfcrepo.streaming.parallel=true` related to performance

- [ModeShape Lucene libraries](https://github.com/ualbertalib/fcrepo4-oaiprovider/blob/b5babd5b60dfb3ba56517dcfb3d37631e72275ad/pom.xml#L390-L401) to support Modeshape index on multi-valued properties as the `local` index provider for "multi-value properties, this provider will only store & use the first value." [reference](https://docs.jboss.org/author/display/MODE50/Local) and [reference](https://docs.jboss.org/author/display/MODE50/Local)


#### Features

* Alignment of metadata with Jupiter metadata (e.g., FileSets, swrc:institution, etc.)
    * Initial work toward support for data model alterations described in [Jupiter](https://github.com/ualbertalib/jupiter) and the [Data Dictionary](https://github.com/ualbertalib/metadata/tree/master/data_dictionary)
    * Alignment of metadata with Jupiter seed data (e.g., FileSets, swrc:institution, etc.)
    * Includes file related metadata: traverse graph to locate attached file and return properties: ebucore:filename, premise:hasSize, ebucore:hasMimeType
    * swrc:institution: map to human readible output
* Fedora 4.7.4 compatibility
* performance tweaks in config files
    * `cacheSize` in repository.json - tweak for performance
    * `Dfcrepo.streaming.parallel=true` - add to docker for performance testing
    * ModeShape indexing
* namespaces as per [Data Dictionary](https://github.com/ualbertalib/metadata/tree/master/data_dictionary) including node-types_ual.cnd to prevent startup errors due to undefined namespaces (when not existing content is present)
* `model:hasModel` to differentiate between thesis and regular models
* `dcterms:accessRights`-> public only
* `ual:path` - for sets (including fix for multivalue problem i.e., and IRItem belonging to multiple collections)
* reverse JCR query workarounds (from Fcrepo 4.2) no longer required in Fcrepo 4.7.4
* alter property acquisition to conform to PCDM and Jupiter specific structures
* restrict response to only `public` dcterms:accessRights

#### Notes

* v4.7.4.3 release is untested with other Fcrepo versions. The release is largely based on version 4.2.


<a name="v4.7.4.4"/>

## v4.7.4.4 Fcrepo4 OAI Provider

Adds ability to function with system generated namespaces (e.g., ns001) when CND file with prefixes not specified beforehand.

Refactors code to work with Fedora without namespace references populated before data added: (https://wiki.duraspace.org/display/FEDORA4x/Best+Practices+-+RDF+Namespaces)

Why? ModeShape JCR and JCR SQL uses registered prefixs e.g., dcterms:title and if not registered beforehand then the system assigns ns00x:title with expectations within the JCR level that these prefixes be used or {http://purl.org/dc/terms/}title syntax used. This update uses the latter syntax


<a name="v4.7.4.5"/>

## v4.7.4.5 Fcrepo4 OAI Provider

* added PostgreSQL repository.json config to the Maven build
* added activeMQ connector to triplestore config to the Maven build


<a name="v4.7.4.6"/>

## v4.7.4.6 Fcrepo4 OAI Provider

* updated documentation
* output language term instead of url code #34
* output type term instead of url code #33
* single DOI reference #35
* tweak setSpec as per #30

<a name="v4.7.4.7"/>

## v4.7.4.7 Fcrepo4 OAI Provider

* change to remove '1/' from before the UUID idenifier #30 

<a name="v4.7.4.8"/>

## v4.7.4.8 Fcrepo4 OAI Provider

* update web.xml with contents from https://github.com/fcrepo4/fcrepo4/blob/fcrepo-4.7.4/fcrepo-webapp/src/main/webapp/WEB-INF/web.xml
* update documentation to describe how web.xml changes from the default 

<a name="v4.7.4.9"/>

## v4.7.4.9 Fcrepo4 OAI Provider

* Addresses issue #44 - LAC date (without time) validation error


<a name="v4.7.5.0"/>

## v4.7.5.0 Fcrepo4 OAI Provider

### Overview

A minor revision of v4.7.4 - see release note for detailed explaination of the 4.7.x.x release family.

### Features

* update version in pom.xml
  * fcrepo.version: bump Fcrepo to 4.7.5 - https://wiki.duraspace.org/display/FF/Fedora+4.7.5+Release+Notes
  * modeshape.version: bump to 5.4.0.Final as per Fcrepo release 
  * lucene.version: bump lucene to 6.4.1 as per ModeShape release
* Add Narayana config : https://github.com/fcrepo4/fcrepo4/commit/cab6f05305e26cf7b0aa818e6683dda422c6984d / https://jira.duraspace.org/browse/FCREPO-2664
* Check web.xml for updates

