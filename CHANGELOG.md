# Changelog

All notable changes to this project will be documented in this file. See [standard-version](https://github.com/conventional-changelog/standard-version) for commit guidelines.

## 1.1.0 (2019-09-06)


### Bug Fixes

* [#2138](https://github.com/Informasjonsforvaltning/concept-catalogue/issues/2138) Fix invalid access check for GET/PATCH/DELETE /begrep endpoint ([8e411f8](https://github.com/Informasjonsforvaltning/concept-catalogue/commit/8e411f8))
* **concept-catalogue:** [#2075](https://github.com/Informasjonsforvaltning/concept-catalogue/issues/2075) update missing tillattTerm and frarådetTerm fields ([bd4d7e9](https://github.com/Informasjonsforvaltning/concept-catalogue/commit/bd4d7e9))
* **concept-catalogue:** [#2121](https://github.com/Informasjonsforvaltning/concept-catalogue/issues/2121) Implement publishing bar(adds last-updated-datetime) ([0947dbe](https://github.com/Informasjonsforvaltning/concept-catalogue/commit/0947dbe))
* **concept-catalogue:** [#2156](https://github.com/Informasjonsforvaltning/concept-catalogue/issues/2156) do not set default values to serialised strings ([9b88545](https://github.com/Informasjonsforvaltning/concept-catalogue/commit/9b88545))
* **concept-catalogue:** [#2156](https://github.com/Informasjonsforvaltning/concept-catalogue/issues/2156) perform empty list check in order to determine whether parameters should be updated ([776e7ef](https://github.com/Informasjonsforvaltning/concept-catalogue/commit/776e7ef))
* **concept-catalogue:** [#2169](https://github.com/Informasjonsforvaltning/concept-catalogue/issues/2169) allow overwriting kildebeskrivelse if it is set to null ([69dd9f7](https://github.com/Informasjonsforvaltning/concept-catalogue/commit/69dd9f7))
* **concept-catalogue:** Include source in harvesting ([ba7f102](https://github.com/Informasjonsforvaltning/concept-catalogue/commit/ba7f102))
* **concept-catalogue:** Reinstate test, now with working ,mockito wildcard ([252057e](https://github.com/Informasjonsforvaltning/concept-catalogue/commit/252057e))


### Features

* [#2138](https://github.com/Informasjonsforvaltning/concept-catalogue/issues/2138) Apply security globally to all operations ([34bdaf5](https://github.com/Informasjonsforvaltning/concept-catalogue/commit/34bdaf5))
* [#2138](https://github.com/Informasjonsforvaltning/concept-catalogue/issues/2138) Protect begreper/?orgNummer={orgnr} endpoint ([d176180](https://github.com/Informasjonsforvaltning/concept-catalogue/commit/d176180))
* [#2138](https://github.com/Informasjonsforvaltning/concept-catalogue/issues/2138) Protect DELETE /begreper/{id} endpoint ([abb4a3e](https://github.com/Informasjonsforvaltning/concept-catalogue/commit/abb4a3e))
* [#2138](https://github.com/Informasjonsforvaltning/concept-catalogue/issues/2138) Protect GET /begreper/{id} endpoint ([9067405](https://github.com/Informasjonsforvaltning/concept-catalogue/commit/9067405))
* [#2138](https://github.com/Informasjonsforvaltning/concept-catalogue/issues/2138) Protect PATCH begreper endpoint ([9846fd2](https://github.com/Informasjonsforvaltning/concept-catalogue/commit/9846fd2))
* [#2138](https://github.com/Informasjonsforvaltning/concept-catalogue/issues/2138) Protect POST begreper endpoint ([6467ae8](https://github.com/Informasjonsforvaltning/concept-catalogue/commit/6467ae8))
* [#2138](https://github.com/Informasjonsforvaltning/concept-catalogue/issues/2138) Require authentication for all endpoints, except harvesting (/collections/) ([e2886b1](https://github.com/Informasjonsforvaltning/concept-catalogue/commit/e2886b1))
* **concept-catalogue:** [#2075](https://github.com/Informasjonsforvaltning/concept-catalogue/issues/2075) add missing harvest endpoint tillattTerm and frarådetTerm fields ([71b842f](https://github.com/Informasjonsforvaltning/concept-catalogue/commit/71b842f))
* **concept-catalogue:** [#2169](https://github.com/Informasjonsforvaltning/concept-catalogue/issues/2169) allow for nullable forholdTilKilde in kildebeskrivelse ([79f32bc](https://github.com/Informasjonsforvaltning/concept-catalogue/commit/79f32bc))
* **concept-catalogue:** [#2219](https://github.com/Informasjonsforvaltning/concept-catalogue/issues/2219) use JSON patch to update begrep ([b6683bd](https://github.com/Informasjonsforvaltning/concept-catalogue/commit/b6683bd))
