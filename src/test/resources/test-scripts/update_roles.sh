# This script add basic roles from roles.json to test-collection container.
curl -X POST http://localhost:8080/fcrepo-webapp-plus-rbacl-4.1.0/rest/test-collection/fcr:accessroles -H "Content-Type: application/json" -d "@roles.json" -u fedoraAdmin:fedoraAdmin
