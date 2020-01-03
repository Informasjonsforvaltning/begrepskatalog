# Concept-catalogue

Backend for the concept-registration.

## Run locally in a container

```
% mvn clean install
% docker-compose up -d
```

## Test interactively
First you have to get a jwt:
```
curl localhost:8084/jwt/write -o jwt.txt
```
As of now, you have to edit jwt.txt manually to use it:
* Remove curly brackets
* Replace 'token:' with the text 'Authorization: bearer'

Example:
```
% curl -i -H "Content-Type: application/json" -H @jwt.txt -d '{"anbefaltTerm":{"navn":{}},"status":"utkast","ansvarligVirksomhet":{"id":"910244132"}}' -X POST "http://localhost:8201/begreper?orgNummer=910244132"
% curl -i -H "Accept: application/json" -H @jwt.txt "http://localhost:8201/begreper?orgNummer=910244132"
% curl -i -H "Content-Type: application/json" -H @jwt.txt -d '[{"op":"add","path":"/anbefaltTerm/navn/en","value":"iut"},{"op":"add","path":"/definisjon","value":{"tekst":{"en":"interactive user test"}}}]' -X PATCH "http://localhost:8201/begreper/a71a3506-fa77-4c37-9f6c-ea807fd0129a"
% curl -i -H "Content-Type: application/json" -H @jwt.txt -d '[{"op":"add","path":"/status","value":"publisert"}]'  -X PATCH "http://localhost:8201/begreper/d0f3ad19-59c5-4ee1-b64c-1c938b4b5d09"
% curl -H "Accept: text/turtle" http://localhost:8201/collections
```
