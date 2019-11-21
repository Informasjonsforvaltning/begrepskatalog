# Concept-catalogue

Backend for the concept-registration.

## Run locally in a container

```
% mvn clean install
% docker-compose up -d
```

## Test interactively

Example:
```
% curl -i -H "Content-Type: application/json" -H @jwt.txt -d '{"anbefaltTerm":{"navn":{}},"status":"utkast","ansvarligVirksomhet":{"id":"910244132"}}' -X POST "http://localhost:8201/begreper?orgNummer=910244132"
% curl -i -H "Accept: application/json" -H @jwt.txt "http://localhost:8201/begreper?orgNummer=910244132"
% curl -i -H "Content-Type: application/json" -H @jwt.txt -d '[{"op":"add","path":"/anbefaltTerm/navn/en","value":"iut"},{"op":"add","path":"/definisjon/tekst/en","value":"interactive user test"}]' -X PATCH "http://localhost:8201/begreper/d0f3ad19-59c5-4ee1-b64c-1c938b4b5d09"
% curl -i -H "Content-Type: application/json" -H @jwt.txt -d '[{"op":"add","path":"/status","value":"publisert"}]'  -X PATCH "http://localhost:8201/begreper/d0f3ad19-59c5-4ee1-b64c-1c938b4b5d09"
% curl -H "Accept: text/turtle" http://localhost:8201/collections
```
