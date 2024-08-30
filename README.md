
# Spring Boot URL Shortener

A very simple Spring Boot based REST API that converts long URLs to tiny strings and uses H2 Database to persist data.

# Features
- **Variety**: Multiple URL shortening algorithms for different use-cases
- **Fixed Length**: The service produces URLs of a max length (10), ensuring predictability in the URL structure.
- **Collision Handling**: The service is designed with mechanisms to handle and reduce potential collisions.

# Non-Functional Requirements
* Scalability
* Performance and Elasticity
* Availability
* Resilience

# General Design
## Component / Application Design


## Technologies used
Here are the technologies used for this Api :
* Java 17
* Maven
* Spring Boot 3.3.0
* Spring Boot Starter Jpa
* Spring Boot Web 
* Spring Boot Actuator
* Spring Boot retry (Enable retry on database in failure)
* Apache commons-validator (For url validation)
* OpenApi (For Api Specification)
* H2 Database (file based database)
* Hazelcast (to enable Cache)
* Mockito
* JUnit5

## How to run application
To build and run the project :
* git clone https://github.com/patken/spring-boot-url-shortener-api.git
* cd spring-boot-url-shortener-api
* mvn clean install
  * It will build up the project by generating Api specification and model needed
  * Api specification oas3.yaml is located : src/main/resources/openapi/oas3.yaml
* Run with : mvn spring-boot:run --spring.profiles.active=local
  * Here we used local configuration for setting h2 database ; in others environment, it will be another database like Postgres
* The Rest Api will be available at the url : http://localhost:8080/api/v1/url-shortener

## Api Specification (EndPoint)

[OAS3 Specification file](https://petstore.swagger.io/?url=https://raw.githubusercontent.com/patken/spring-boot-url-shortener-api/main/src/main/resources/openapi/oas3.yaml)

### POST Save a new url with its shortened version.

POST /url-shortener

> Body Parameters

```json
{
  "url": "string"
}
```

#### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|body|body|[ShortenUrlRequest](#schemashortenurlrequest)| no | ShortenUrlRequest|none|

> Response Examples

> 201 Response

```json
{
  "originalUrl": "string",
  "shortenUrl": "string"
}
```

#### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|201|[Created](https://tools.ietf.org/html/rfc7231#section-6.3.2)|Url created successfully.|[ShortenUrlResponse](#schemashortenurlresponse)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Bad Request due to missing / invalid parameters|[Problem](#schemaproblem)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Unauthorized|[Problem](#schemaproblem)|
|500|[Internal Server Error](https://tools.ietf.org/html/rfc7231#section-6.6.1)|Internal Server Error|[Problem](#schemaproblem)|

### GET Get All url shortened url

GET /url-shortener

#### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|page|query|Integer| no ||The page of the occurrence|
|limit|query|Integer| no ||The limit for the number of records to be return by page|

> Response Examples

> 200 Response

```json
{
  "records": [
    {
      "originalUrl": "string",
      "shortenUrl": "string"
    }
  ],
  "total": 25,
  "next": "http://localhost:8080/api/v1/url-shortener?page=1&limit=10"
}
```

#### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|Ok, All the url are retrieved successfully.|[ShortenUrlPageResponse](#schemashortenurlpageresponse)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Bad Request due to missing / invalid parameters|[Problem](#schemaproblem)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Unauthorized|[Problem](#schemaproblem)|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|Not Found|[Problem](#schemaproblem)|
|500|[Internal Server Error](https://tools.ietf.org/html/rfc7231#section-6.6.1)|Internal Server Error|[Problem](#schemaproblem)|


### GET Get original url from shortened url

GET /url-shortener/{shortenUrl}

#### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|shortenUrl|path|string| yes ||The shortened url used to get original url|

> Response Examples

> 200 Response

```json
{
  "originalUrl": "string",
  "shortenUrl": "string"
}
```

#### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|Ok, the original url is retrieved successfully.|[ShortenUrlResponse](#schemashortenurlresponse)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Unauthorized|[Problem](#schemaproblem)|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|Not Found|[Problem](#schemaproblem)|
|500|[Internal Server Error](https://tools.ietf.org/html/rfc7231#section-6.6.1)|Internal Server Error|[Problem](#schemaproblem)|

### Data Schema

<h4 id="tocS_Element">Element</h2>

<a id="schemaelement"></a>
<a id="schema_Element"></a>
<a id="tocSelement"></a>
<a id="tocselement"></a>

```json
{
  "message": "string",
  "timestamp": "2024-08-20T15:35:12"
}

```

Element

#### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|message|string|false|none||A short, human-readable summary of the reason|
|timestamp|string(date-time)|false|none||A timestamp identifying the specific occurrence of the element in cause|

<h2 id="tocS_ShortenUrlPageResponse">ShortenUrlPageResponse</h2>

<a id="schemashortenurlpageresponse"></a>
<a id="schema_ShortenUrlPageResponse"></a>
<a id="tocSshortenurlpageresponse"></a>
<a id="tocsshortenurlpageresponse"></a>

```json
{
  "records": [
    {
      "originalUrl": "string",
      "shortenUrl": "string"
    }
  ],
  "total": 25,
  "next": "http://localhost:8080/api/v1/url-shortener?page=1&limit=10"
}

```

ShortenUrlPageResponse

#### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|records|[[ShortenUrlResponse](#schemashortenurlresponse)]|true|none||[Response while getting original url]|
|total|long|false|none||Total number of records found|
|next|string|false|none||Simple Link to get the next x elements|

<h2 id="tocS_ShortenUrlResponse">ShortenUrlResponse</h2>

<a id="schemashortenurlresponse"></a>
<a id="schema_ShortenUrlResponse"></a>
<a id="tocSshortenurlresponse"></a>
<a id="tocsshortenurlresponse"></a>

```json
{
  "originalUrl": "string",
  "shortenUrl": "string"
}

```

ShortenUrlResponse

#### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|originalUrl|string|false|none||Original Url shortened|
|shortenUrl|string|false|none||Url shortened|

<h2 id="tocS_Problem">Problem</h2>

<a id="schemaproblem"></a>
<a id="schema_Problem"></a>
<a id="tocSproblem"></a>
<a id="tocsproblem"></a>

```json
{
  "title": "string",
  "detail": "string",
  "elements": [
    {
      "message": "string",
      "timestamp": "2024-08-20T15:35:12"
    }
  ]
}

```

Problem

#### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|title|string|false|none||A short, human-readable summary of the problem type|
|detail|string|false|none||Explanation specific to this occurrence of the problem|
|elements|[[Element](#schemaelement)]|false|none||Error List|

<h2 id="tocS_ShortenUrlRequest">ShortenUrlRequest</h2>

<a id="schemashortenurlrequest"></a>
<a id="schema_ShortenUrlRequest"></a>
<a id="tocSshortenurlrequest"></a>
<a id="tocsshortenurlrequest"></a>

```json
{
  "url": "string"
}

```

ShortenUrlRequest

#### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|url|string|true|none||Original Url to shorten|
