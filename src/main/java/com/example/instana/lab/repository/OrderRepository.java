package com.example.instana.lab.repository;

import com.example.instana.lab.model.Scenario;
import com.instana.sdk.annotation.Span;
import com.instana.sdk.annotation.TagParam;
import com.instana.sdk.support.SpanSupport;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

public class OrderRepository {

    private static final String JDBC_URL = "jdbc:h2:mem:instana_sdk_lab;DB_CLOSE_DELAY=-1";
    private static final String JDBC_USER = "sa";
    private static final String JDBC_PASSWORD = "";
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    @Span(value = "instana-sdk-lab.db.initialize", type = Span.Type.INTERMEDIATE)
    public void initialize() {
        if (INITIALIZED.get()) {
            return;
        }
        synchronized (INITIALIZED) {
            if (INITIALIZED.get()) {
                return;
            }
            try {
                Class.forName("org.h2.Driver");
                Connection connection = openConnection();
                try {
                    Statement statement = connection.createStatement();
                    try {
                        statement.execute("CREATE TABLE IF NOT EXISTS ORDERS (" +
                                "ORDER_ID VARCHAR(64) PRIMARY KEY, " +
                                "CUSTOMER_ID VARCHAR(64), " +
                                "AMOUNT DECIMAL(10,2), " +
                                "STATUS VARCHAR(32))");
                        statement.execute("CREATE TABLE IF NOT EXISTS ORDER_AUDIT (" +
                                "ID IDENTITY PRIMARY KEY, " +
                                "ORDER_ID VARCHAR(64), " +
                                "ACTION VARCHAR(64), " +
                                "DETAIL VARCHAR(255), " +
                                "CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
                        statement.execute("MERGE INTO ORDERS KEY(ORDER_ID) VALUES " +
                                "('ORD-1001', 'CUST-88', 1299.00, 'PAID'), " +
                                "('ORD-1002', 'CUST-99', 300.00, 'PENDING'), " +
                                "('ORD-1003', 'CUST-77', 42.00, 'PAID')");
                    } finally {
                        statement.close();
                    }
                } finally {
                    connection.close();
                }
                INITIALIZED.set(true);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("H2 driver not found", e);
            } catch (SQLException e) {
                throw new IllegalStateException("Unable to initialize H2 database", e);
            }
        }
    }

    @Span(value = "instana-sdk-lab.db.list-work-queue", type = Span.Type.EXIT)
    public QueryResult listWorkQueue(@TagParam("scenario") Scenario scenario) {
        initialize();
        long startedAt = System.currentTimeMillis();
        int rows = 0;
        try {
            Connection connection = openConnection();
            try {
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT ORDER_ID, CUSTOMER_ID, AMOUNT, STATUS FROM ORDERS ORDER BY ORDER_ID");
                try {
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        while (resultSet.next()) {
                            rows++;
                        }
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    statement.close();
                }
            } finally {
                connection.close();
            }
            if (scenario == Scenario.SLOW_QUERY) {
                rows += runSlowJdbcWorkload();
            } else if (scenario == Scenario.BAD_QUERY) {
                rows += runBadJdbcWorkload();
            }
            return queryResult(rows, startedAt);
        } catch (SQLException e) {
            throw new IllegalStateException("Work queue query failed", e);
        }
    }

    @Span(value = "instana-sdk-lab.db.find-order", type = Span.Type.EXIT)
    public QueryResult findOrder(@TagParam("order.id") String orderId,
                                 @TagParam("customer.id") String customerId,
                                 @TagParam("scenario") Scenario scenario) {
        initialize();
        long startedAt = System.currentTimeMillis();
        int rows = 0;
        try {
            Connection connection = openConnection();
            try {
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT ORDER_ID, CUSTOMER_ID, AMOUNT, STATUS FROM ORDERS WHERE ORDER_ID = ?");
                try {
                    statement.setString(1, orderId);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        while (resultSet.next()) {
                            rows++;
                        }
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    statement.close();
                }
            } finally {
                connection.close();
            }

            if (scenario == Scenario.SLOW_QUERY) {
                rows += runSlowJdbcWorkload();
            } else if (scenario == Scenario.BAD_QUERY) {
                rows += runBadJdbcWorkload();
            }

            long elapsed = System.currentTimeMillis() - startedAt;
            SpanSupport.annotate("db.rows", String.valueOf(rows));
            SpanSupport.annotate("db.elapsed_ms", String.valueOf(elapsed));
            return new QueryResult(rows, elapsed);
        } catch (SQLException e) {
            throw new IllegalStateException("Order query failed", e);
        }
    }

    @Span(value = "instana-sdk-lab.db.upsert-order", type = Span.Type.EXIT)
    public QueryResult upsertOrder(@TagParam("order.id") String orderId,
                                   @TagParam("customer.id") String customerId,
                                   @TagParam("scenario") Scenario scenario) {
        initialize();
        long startedAt = System.currentTimeMillis();
        try {
            Connection connection = openConnection();
            try {
                PreparedStatement statement = connection.prepareStatement(
                        "MERGE INTO ORDERS KEY(ORDER_ID) VALUES (?, ?, ?, ?)");
                try {
                    statement.setString(1, orderId);
                    statement.setString(2, customerId);
                    statement.setBigDecimal(3, new java.math.BigDecimal("560.00"));
                    statement.setString(4, "SUBMITTED");
                    int rows = statement.executeUpdate();
                    return queryResult(rows, startedAt);
                } finally {
                    statement.close();
                }
            } finally {
                connection.close();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Order upsert failed", e);
        }
    }

    @Span(value = "instana-sdk-lab.db.update-order-status", type = Span.Type.EXIT)
    public QueryResult updateOrderStatus(@TagParam("order.id") String orderId,
                                         @TagParam("scenario") Scenario scenario,
                                         @TagParam("order.status") String status) {
        initialize();
        long startedAt = System.currentTimeMillis();
        try {
            Connection connection = openConnection();
            try {
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE ORDERS SET STATUS = ? WHERE ORDER_ID = ?");
                try {
                    statement.setString(1, status);
                    statement.setString(2, orderId);
                    int rows = statement.executeUpdate();
                    if (rows == 0) {
                        throw new IllegalStateException("order " + orderId + " was not found");
                    }
                    return queryResult(rows, startedAt);
                } finally {
                    statement.close();
                }
            } finally {
                connection.close();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Order status update failed", e);
        }
    }

    @Span(value = "instana-sdk-lab.db.insert-audit", type = Span.Type.EXIT)
    public QueryResult insertAudit(@TagParam("order.id") String orderId,
                                   @TagParam("order.action") String action,
                                   String detail) {
        initialize();
        long startedAt = System.currentTimeMillis();
        try {
            Connection connection = openConnection();
            try {
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO ORDER_AUDIT (ORDER_ID, ACTION, DETAIL) VALUES (?, ?, ?)");
                try {
                    statement.setString(1, orderId);
                    statement.setString(2, action);
                    statement.setString(3, detail);
                    int rows = statement.executeUpdate();
                    return queryResult(rows, startedAt);
                } finally {
                    statement.close();
                }
            } finally {
                connection.close();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Audit insert failed", e);
        }
    }

    private QueryResult queryResult(int rows, long startedAt) {
        long elapsed = System.currentTimeMillis() - startedAt;
        SpanSupport.annotate("db.rows", String.valueOf(rows));
        SpanSupport.annotate("db.elapsed_ms", String.valueOf(elapsed));
        return new QueryResult(rows, elapsed);
    }

    private int runSlowJdbcWorkload() throws SQLException {
        SpanSupport.annotate("db.workload", "slow-query");
        int rows = 0;
        Connection connection = openConnection();
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT X FROM SYSTEM_RANGE(1, 25000) WHERE MOD(X, 997) = 0");
            try {
                ResultSet resultSet = statement.executeQuery();
                try {
                    while (resultSet.next()) {
                        rows++;
                    }
                } finally {
                    resultSet.close();
                }
            } finally {
                statement.close();
            }
        } finally {
            connection.close();
        }
        sleep(900L);
        return rows;
    }

    private int runBadJdbcWorkload() throws SQLException {
        SpanSupport.annotate("db.workload", "bad-query-cross-join");
        int rows = 0;
        Connection connection = openConnection();
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) AS TOTAL_ROWS " +
                            "FROM SYSTEM_RANGE(1, 15000) A " +
                            "CROSS JOIN SYSTEM_RANGE(1, 15000) B " +
                            "WHERE MOD(A.X + B.X, 7919) = 0");
            try {
                ResultSet resultSet = statement.executeQuery();
                try {
                    if (resultSet.next()) {
                        rows += resultSet.getInt("TOTAL_ROWS");
                    }
                } finally {
                    resultSet.close();
                }
            } finally {
                statement.close();
            }
        } finally {
            connection.close();
        }
        return rows;
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while simulating slow query", e);
        }
    }

    public static class QueryResult {
        private final int rows;
        private final long elapsedMillis;

        public QueryResult(int rows, long elapsedMillis) {
            this.rows = rows;
            this.elapsedMillis = elapsedMillis;
        }

        public int getRows() {
            return rows;
        }

        public long getElapsedMillis() {
            return elapsedMillis;
        }
    }
}
