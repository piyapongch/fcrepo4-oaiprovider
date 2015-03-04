Fedora4 OAI Provider
====================
- https://wiki.duraspace.org/display/FEDORA41/Setup+OAI-PMH+Provider
- https://github.com/fcrepo4-labs/fcrepo4-oaiprovider
- See [integration-test](https://github.com/ualbertalib/fcrepo4-oaiprovider/tree/dev/src/test/java/org/fcrepo/oai/integration) for details
- Support metadata prefix oai_dc, marc21 and premis
- Create OAI set
```bash
$ curl -X POST http://localhost:8080/fcrepo-webapp-plus-rbacl-4.1.0/rest/oai/sets -H "Content-Type: text/xml" -d @oai_set.xml -u fedoraAdmin:fedoraAdmin
```
@oai_set.xml
```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<set xmlns:ns2="http://www.openarchives.org/OAI/2.0/">
    <ns2:setSpec>oai-test-set</ns2:setSpec>
    <ns2:setName>oai-test-set</ns2:setName>
</set>
````
- Create object with fedoraconfig:isPartOfOAISet
```bash
$ curl -X POST --header 'Slug: obj2-with-oai-set' --header 'Content-Type: application/sparql-update' -d @oai_ispartof_set.sparql -u fedoraAdmin:fedoraAdmin http://localhost:8080/fcrepo-webapp-plus-rbacl-4.1.0/rest
```
@oai_ispartof_set.sparql
```sparql
INSERT {<> <http://fedora.info/definitions/v4/config#isPartOfOAISet> "oai-test-set"} 
WHERE {}
```
- Hydra integrations
... Add OAI functionality to Sufia front end to add object metadata, isPartOfOAISet and setSpec and setName to /oai/sets.
... Use Apache Camel to add metadata and OAI set.
- Add OAI link for MARC21 binary
```sparql
INSERT {<> <http://fedora.info/definitions/v4/config#hasOaiMarc21Record> “oai-marc21/binary-path”} 
WHERE {}
```
- Add OAI link for PREMIS binary
```sparql
INSERT {<> <http://fedora.info/definitions/v4/config#hasOaiPremisRecord> “oai-premis/binary-path”} 
WHERE {}
```
