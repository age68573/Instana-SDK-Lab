# Instana SDK Lab

Java 17 Maven WAR application for demonstrating Instana Java SDK method-level tracing, tags, JDBC traces, slow query simulation, and controlled error scenarios on JBoss 8 / Jakarta EE.

## Build

```bash
mvn clean package
```

The WAR is generated at:

```text
target/instana-sdk-lab.war
```

## Deploy

Deploy `target/instana-sdk-lab.war` to JBoss 8. The application serves API routes at `/api` and serves the UI from the web application root.

Example endpoints:

```text
/instana-sdk-lab/
/instana-sdk-lab/api/health
/instana-sdk-lab/api/scenarios
```

## Demo Flow

Open the UI:

```text
http://<jboss-host>:8080/instana-sdk-lab/
```

The page is a Traditional Chinese order operations console. Select one operation, choose a scenario, then run the request.

| Operation | HTTP | Example API | Purpose |
| --- | --- | --- | --- |
| 查詢訂單工作區 | GET | `/api/orders/ORD-1001/workspace?customerId=CUST-88&scenario=normal` | Reads the order, customer profile, audit trail, and work queue. |
| 送出新訂單 | POST | `/api/orders/ORD-1001/submission?customerId=CUST-88&scenario=normal` | Validates the order, checks risk, reserves inventory, writes audit data. |
| 核准出貨 | PUT | `/api/orders/ORD-1001/approval?customerId=CUST-88&scenario=normal` | Reserves inventory, authorizes payment, schedules fulfillment. |
| 取消訂單 | DELETE | `/api/orders/ORD-1001/cancellation?customerId=CUST-88&scenario=normal` | Releases inventory, updates status, writes audit data. |

Supported scenarios:

| Scenario | Expected behavior |
| --- | --- |
| `normal` | Successful request with JDBC activity and method-level SDK spans. |
| `slow-query` | Adds a slower JDBC workload so DB/query time is visible. |
| `bad-query` | Runs an intentionally inefficient JDBC cross join so the database span is noticeably slower. |
| `slow-method` | Makes `instana-sdk-lab.payment.authorize` run for more than 5 seconds. |
| `business-error` | Returns HTTP 400 and tags the trace with business error metadata. |
| `system-error` | Returns HTTP 500 and tags the trace with system error metadata. |
| `random-error` | Randomly fails to create mixed successful and erroneous traces. |

## Monitoring Notes

In Instana, each request should produce an HTTP entry span plus SDK child spans for the major business methods. The named routes are designed to be easier to identify than a plain `GET` or `POST`:

```text
GET /api/orders/{orderId}/workspace
POST /api/orders/{orderId}/submission
PUT /api/orders/{orderId}/approval
DELETE /api/orders/{orderId}/cancellation
```

Useful spans to inspect:

- `instana-sdk-lab.order.retrieve-worklist`
- `instana-sdk-lab.order.submit`
- `instana-sdk-lab.order.approve`
- `instana-sdk-lab.order.cancel`
- `instana-sdk-lab.db.initialize`
- `instana-sdk-lab.db.find-order`
- `instana-sdk-lab.db.audit`
- `instana-sdk-lab.inventory.reserve`
- `instana-sdk-lab.payment.authorize`

Useful trace tags:

- `request.name`
- `http.route`
- `order.operation`
- `order.id`
- `customer.id`
- `scenario`
- `error.type`
- `error.message`
- `db.rows`
- `db.elapsed_ms`
- `db.workload`
- `slow.method`
- `slow.threshold_ms`

For failure demonstrations, HTTP 400 and HTTP 500 responses are intentional. They are useful for validating Instana error detection, trace filtering, and tags.

## Instana Agent Configuration

The Instana Java Trace SDK annotations are inactive unless the Instana agent scans this application package. Add the package to the agent configuration:

```yaml
com.instana.plugin.javatrace:
  instrumentation:
    sdk:
      packages:
        - 'com.example.instana.lab'
```

Restart the agent after updating the configuration.

## Demonstrated Tags

- `request.name`
- `http.route`
- `order.operation`
- `order.id`
- `customer.id`
- `scenario`
- `error.type`
- `error.message`
- `db.rows`
- `db.elapsed_ms`
- `db.workload`
- `slow.method`
- `slow.threshold_ms`
- `payment.provider`
