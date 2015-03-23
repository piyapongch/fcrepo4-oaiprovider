# This script creates a simple object and adds fedoraconfig:isPartOfOAISet property using oai_ispartof_set.sparql.
curl -X POST -H "Slug: obj-with-oai-set" -H "Content-Type: application/sparql-update" -d @oai_ispartof_set.sparql http://localhost:8080/fcrepo-webapp-plus-rbacl-4.1.0/rest/ -u fedoraAdmin:fedoraAdmin
