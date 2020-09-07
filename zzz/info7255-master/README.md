[![CircleCI](https://circleci.com/gh/adityarkelkar/info7255.svg?style=svg)](https://circleci.com/gh/adityarkelkar/info7255)

# Advanced Big Data Applications and Indexing Techniques
## Northeastern University (May 2019 - August 2019)

Repository related to development for REST Api prototype model demo work for INFO 7255  
  
[**Architecture diagram**](https://github.com/adityarkelkar/info7255/blob/master/ArchitectureDiagram.pdf)

## Contents
In this project, we will develop a REST Api to parse a JSON schema model divided into three demos
1. **Prototype demo 1**
    - Develop a Spring Boot based REST Api to parse a given sample JSON schema.
    - Save the JSON schema in a redis key value store.
    - Demonstrate the use of operations like `GET`, `POST` and `DELETE` for the first prototype demo.
2. **Prototype demo 2**
    - Regress on your model and perform additional operations like `PUT` and `PATCH`.
    - Secure the REST Api with a security protocol like JWT or OAuth2.
3. **Prototype demo 3**
    - Adding Elasticsearch capabilities
    - Adding Kafka system for REST API queueing

## Pre-requisites
1. Redis Server
2. Elasticsearch and Kibana(Local or cloud based)
3. Apache Kafka