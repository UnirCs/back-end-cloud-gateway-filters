# back-end-cloud-gateway-filters

**API Gateway with Anti-Corruption Layer (ACL)** for the **UNIR Supplies** microservices ecosystem, built with **Spring Cloud Gateway (WebFlux)** and custom global filters.

Unlike the [basic gateway](../back-end-cloud-gateway), this implementation enforces that **all incoming requests must be POST** with a structured JSON body. The gateway then translates each request into the actual HTTP method (GET, POST) expected by the downstream microservice. This pattern acts as an **Anti-Corruption Layer**, ensuring:

1. **Internal endpoints are never directly exposed** — clients don't know the real HTTP verbs or query parameters of backend services.
2. **All communication travels inside request bodies** — making it possible to encrypt/sign the entire payload in transit (encryption is not implemented in this version, but the architecture supports it).

---

## How it works

```
                                    ┌──────────────────────────────────┐
  Client ──► POST (always) ──────► │  Gateway + RequestTranslation   │
             {                      │         Filter (ACL)            │
               "targetMethod":"GET",│                                  │
               "queryParams":{...}, │  1. Extract GatewayRequest body  │
               "body":{...}         │  2. Decorate → real HTTP method  │
             }                      │  3. Forward to microservice      │
                                    └────────────┬─────────────────────┘
                                                 │
                              ┌───────────────────┼──────────────────┐
                              ▼                   ▼                  ▼
                        GET /supplies      POST /orders      ... other
                        ?name=...          {body}
```

### Request format

Every request to the gateway must be a `POST` with `Content-Type: application/json` and the following body structure:

```json
{
  "targetMethod": "GET",
  "queryParams": {
    "name": ["paper"],
    "type": ["Stationery"]
  },
  "body": null
}
```

| Field | Type | Description |
|-------|------|-------------|
| `targetMethod` | `string` | The actual HTTP method for the downstream service (`GET` or `POST`) |
| `queryParams` | `object` | Query parameters to append (for GET requests). Keys map to arrays of values. Can be `null`. |
| `body` | `object` | Request body to forward (for POST requests). Can be `null`. |

If the request is **not** a POST or has no `Content-Type`, the gateway immediately returns **400 Bad Request**.

---

## Components

### `RequestTranslationFilter` (Global Filter)

The core ACL filter. Implements `GlobalFilter` and intercepts every request:

1. **Rejects** non-POST requests or requests without `Content-Type` → returns `400`.
2. **Joins** the request body into a single `DataBuffer`.
3. **Extracts** the body into a `GatewayRequest` object via `RequestBodyExtractor`.
4. **Decorates** the request using `RequestDecoratorFactory` to transform it into the real HTTP method.
5. **Forwards** the mutated request down the filter chain to the target microservice.

### `RequestBodyExtractor`

- Reads the raw request body from the `DataBuffer`.
- Deserializes it into a `GatewayRequest` using Jackson `ObjectMapper`.
- Adjusts headers: removes `Content-Length` and sets `Transfer-Encoding: chunked` (required by Spring Cloud Gateway for mutated requests).

### `RequestDecoratorFactory`

Factory that creates the appropriate `ServerHttpRequestDecorator` based on `targetMethod`:

| Target Method | Decorator | Behavior |
|---------------|-----------|----------|
| `GET` | `GetRequestDecorator` | Sets method to GET, appends `queryParams` to URI, returns empty body |
| `POST` | `PostRequestDecorator` | Sets method to POST, serializes `body` to JSON as the request body |
| Other | — | Throws `IllegalArgumentException` |

### `GetRequestDecorator`

- Overrides method → `GET`
- Overrides URI → appends `queryParams` from the `GatewayRequest`
- Overrides body → `Flux.empty()` (GET requests have no body)

### `PostRequestDecorator`

- Overrides method → `POST`
- Overrides URI → target service URI (no query params)
- Overrides body → serializes `GatewayRequest.body` to JSON bytes via `ObjectMapper`

### `GatewayRequest` (Model)

```java
{
  HttpMethod targetMethod;              // Target HTTP method
  LinkedMultiValueMap<String, String> queryParams;  // Query params (for GET)
  Object body;                          // Request body (for POST)
  ServerWebExchange exchange;           // @JsonIgnore — internal
  HttpHeaders headers;                  // @JsonIgnore — internal
}
```

---

## Configuration

### CORS & Routing (`application.yml`)

This gateway **only allows POST** as the CORS-permitted method (unlike the basic gateway which allows all methods):

```yaml
allowedMethods:
  - POST
```

| Property | Value | Description |
|----------|-------|-------------|
| `discovery.locator.enabled` | `true` | Auto-discovers routes from Eureka |
| `discovery.locator.lower-case-service-id` | `true` | Lowercase service names in routes |
| `globalcors.allowedMethods` | `POST` | **Only POST allowed** from clients |
| `default-filters` | `DedupeResponseHeader` | Removes duplicate CORS headers |

### Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `8762` | Gateway listening port |
| `HOSTNAME` | `localhost` | Instance hostname for Eureka |
| `EUREKA_URL` | `http://localhost:8761/eureka` | Eureka server URL |
| `ALLOWED_ORIGINS` | `*` | CORS allowed origins |
| `ROUTE_TABLES_ENABLED` | `read_only` | Actuator gateway endpoint access |
| `PROFILE` | `default` | Spring active profile |

---

## Build & Run

### Compile and package

```bash
mvn clean package
```

### Run locally

```bash
java -jar target/gateway-filters-0.0.1-SNAPSHOT.jar
```

### Docker

```bash
docker build -t cloud-gateway-filters .
docker run -p 8762:8762 -e EUREKA_URL=http://eureka-host:8761/eureka cloud-gateway-filters
```

### Example: calling the catalogue through the gateway

```bash
# GET supplies with filters (translated from POST → GET)
curl -X POST http://localhost:8762/supplies-catalogue/api/v2/supplies \
  -H "Content-Type: application/json" \
  -d '{
    "targetMethod": "GET",
    "queryParams": { "type": ["Stationery"] },
    "body": null
  }'

# Create a supply (translated from POST → POST)
curl -X POST http://localhost:8762/supplies-catalogue/api/v1/supplies \
  -H "Content-Type: application/json" \
  -d '{
    "targetMethod": "POST",
    "queryParams": null,
    "body": {
      "name": "Sticky Notes",
      "description": "Yellow 3x3",
      "type": "Stationery",
      "price": 4.99,
      "stock": 100
    }
  }'
```

---

## Deploy on Railway

Deploy this gateway standalone:

[![Deploy on Railway](https://railway.app/button.svg)](https://railway.app/template/CWxqH0?referralCode=jesus-unir)

Deploy the full Spring microservices ecosystem:

[![Deploy on Railway](https://railway.app/button.svg)](https://railway.app/template/f6CKpT?referralCode=jesus-unir)

---
