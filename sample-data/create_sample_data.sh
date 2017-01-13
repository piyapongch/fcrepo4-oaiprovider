#!/bin/sh

# Create Data Container
curl -X POST -H "Slug: dev" http://localhost:8080/fedora/rest/

# Create Community as a Part of Set Name
curl -i -X POST -H "Content-Type: text/turtle" -H "Slug: 0g354f922" -d "@community_object.ttl" http://localhost:8080/fedora/rest/dev/

# Create Collection as a Set
curl -i -X POST -H "Content-Type: text/turtle" -H "Slug: x346d4831" -d "@collection_object.ttl" http://localhost:8080/fedora/rest/dev/

# Create Items
curl -i -X POST -H "Content-Type: text/turtle" -H "Slug: tx31qj49h" -d "@item_report.ttl" http://localhost:8080/fedora/rest/dev/
curl -i -X POST -H "Content-Type: text/turtle" -H "Slug: c2v23vt43w" -d "@item_thesis.ttl" http://localhost:8080/fedora/rest/dev/

# Test the OAI Provider
# http://localhost:8080/fedora/rest/oai?verb=Identify
# http://localhost:8080/fedora/rest/oai?verb=ListMetadataFormats
# http://localhost:8080/fedora/rest/oai?verb=ListIdentifiers&metadataPrefix=oai_dc
# http://localhost:8080/fedora/rest/oai?verb=ListIdentifiers&metadataPrefix=oai_etdms
# http://localhost:8080/fedora/rest/oai?verb=ListRecords&metadataPrefix=oai_dc
# http://localhost:8080/fedora/rest/oai?verb=ListRecords&metadataPrefix=oai_etdms
# http://localhost:8080/fedora/rest/oai?verb=GetRecord&metadataPrefix=oai_etdms&identifier=oai:era.library.ualberta.ca:ark:/54379/t7c2v23vt43w
# http://localhost:8080/fedora/rest/oai?verb=GetRecord&metadataPrefix=oai_dc&identifier=oai:era.library.ualberta.ca:ark:/54379/t7tx31qj49h

