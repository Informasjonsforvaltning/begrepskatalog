# Concept-catalogue

Backend for the concept-registration.

## Run locally in IDEA
Start local instances of SSO, RabbitMQ and MongoDB
```
% docker-compose up -d
```

Add "-Dspring.profiles.active=develop" as a VM option in the Run/Debug Configuration

Run (Shift+F10) or debug (Shift+F9) the application

## Test interactively
First you have to get a jwt:
```
% curl localhost:8084/jwt/write -o jwt.txt
```
As of now, you have to edit jwt.txt manually to use it:
* Remove curly brackets
* Replace 'token:' with the text 'Authorization: bearer'

Example:
```
% curl -i -H "Content-Type: application/json" -H @jwt.txt -d '{"anbefaltTerm": {"navn": {"en": "iut"}},"status": "utkast","ansvarligVirksomhet": {"id": "910244132"},"definisjon":{"tekst": {"en": "interactive user test"}}}' -X POST "http://localhost:8201/begreper?orgNummer=910244132"
% curl -i -H "Accept: application/json" -H @jwt.txt "http://localhost:8201/begreper?orgNummer=910244132"
% curl -i -H "Content-Type: application/json" -H @jwt.txt -d '[{"op":"add","path":"/status","value":"publisert"}]'  -X PATCH "http://localhost:8201/begreper/d0f3ad19-59c5-4ee1-b64c-1c938b4b5d09"
% curl -H "Accept: text/turtle" http://localhost:8201/collections