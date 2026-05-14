# Instana SDK Lab

Java 8 Maven WAR application for demonstrating Instana Java SDK method-level tracing, tags, JDBC traces, slow query simulation, and controlled error scenarios on JBoss EAP.

## Build

```bash
mvn clean package
```

The WAR is generated at:

```text
target/instana-sdk-lab.war
```

## Deploy

Deploy `target/instana-sdk-lab.war` to JBoss EAP. The application uses JAX-RS at `/api` and serves the UI from the web application root.

Example endpoints:

```text
/instana-sdk-lab/
/instana-sdk-lab/api/health
/instana-sdk-lab/api/scenarios
/instana-sdk-lab/api/orders/ORD-1001?customerId=CUST-88&scenario=normal
/instana-sdk-lab/api/orders/ORD-1001?customerId=CUST-88&scenario=slow-query
/instana-sdk-lab/api/orders/ORD-1001?customerId=CUST-88&scenario=business-error
/instana-sdk-lab/api/orders/ORD-1001?customerId=CUST-88&scenario=system-error
/instana-sdk-lab/api/orders/ORD-1001?customerId=CUST-88&scenario=random-error
```

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

- `order.id`
- `customer.id`
- `scenario`
- `error.type`
- `error.message`
- `db.rows`
- `db.elapsed_ms`
- `payment.provider`
