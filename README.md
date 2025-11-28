# url shortener service

## Introduction
This project is a url-shortener REST service (Spring Boot) which forms part of an assessment for the Senior Java Expert Role at Maliroso. 

## Project goal
Build a small, clean microservice that converts long URLs into short codes, stores them and resolves them again. This checks API design, persistence, error handling, tests, build/run.

## Frameworks, libraries, and deliverables
* Language: Java 17+
* Build: Maven
* Allowed frameworks: Spring Boot (Web, Validation, Actuator), Spring Data JPA / Hibernate.
* DB: Postgres (via Docker)
* Tests: JUnit 5 + Mockito
* Rate Limiter: Bucket4j
* Validation: Spring Validator
* Delivery: Git repo including README and Dockerfile

## Assumptions
Below are some assumptions made while building this project:
1. Url short code are unique, but a long url can have multiple short codes differentiated by expiresAt time. If a long url has a short code that has not expired at the time of request, it will returned instead of creating a new one.
2. Url short codes are valid for seven (7) days after which they expire.
3. http is used instead of https as this is not running in production where ssl certificate is available.
4. Rate limiter limits request to 10 per minute.

## Build / Run instructions
### Using Docker and docker-compose
Dockerfile and docker-compose files have been added to the project for easy run on computers that have docker and docker-compose installed.
To run it using docker-compose type:
```agsl
> git clone https://github.com/sylnit/url_shortener.git
> cd url_shortener
> docker-compose up --build

Follow the API documentation to interact with the API
```

### Compile and run raw file
Make sure you have maven installed on target computer, then do the following:
```agsl
> ./mvnw test
> ./mvnw spring-boot:run

To package the application in a jar file, run the following command:

> ./mvnw package
> java -jar target/url_shortener-0.0.1-SNAPSHOT.jar
```



## API / CLI Documentation
### Swagger API Documentation
The API is well document using OpenAPI 3 (springdoc) + Swagger-UI.
Once the project is running, the swagger API documentation can be access at:
[http://localhost:8080/context-path/v3/api-docs](http://localhost:8080/context-path/v3/api-docs)

### Actuator Metrics (Number of redirects)
While the application is running, to see the actuator metrics, go to the follow url
[http://localhost:8080/actuator/metrics](http://localhost:8080/actuator/metrics)

## Features
The application contains the following implemented features:
* Create short url using a long url
* Validate create short url request using Validator
* Redirect to long url when a valid request is made with short url
* Expiry date for url mappings
* Idempotency for short urls that haven't expired
* HitCount to track the number of hits on a short url
* Collision safety with Base62
* Persistence with JPA
* Observability with actuator showing number redirects
* OpenAPI 3 (springdoc) + Swagger-UI
* Dockerfile + docker-compose.yml (with Postgres)
* Rate limiter of 10 requests per minute
* Tests