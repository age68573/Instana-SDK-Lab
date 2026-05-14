package com.example.instana.lab.service;

import com.example.instana.lab.model.OrderResult;
import com.example.instana.lab.model.Scenario;
import com.example.instana.lab.repository.OrderRepository;
import com.instana.sdk.annotation.Span;
import com.instana.sdk.annotation.TagParam;
import com.instana.sdk.support.SpanSupport;

import java.util.Random;

public class OrderService {

    private final OrderRepository orderRepository = new OrderRepository();
    private final Random random = new Random();

    @Span(value = "instana-sdk-lab.order.retrieve-worklist", type = Span.Type.INTERMEDIATE)
    public OrderResult retrieveWorklist(@TagParam("order.id") String orderId,
                                        @TagParam("customer.id") String customerId,
                                        @TagParam("scenario") Scenario scenario) {
        SpanSupport.annotate("http.method", "GET");
        SpanSupport.annotate("order.operation", "retrieve");
        validateOrder(orderId, customerId, scenario);
        auditRequest(orderId, "GET_RETRIEVE", "User opened order workspace");
        enrichCustomerProfile(customerId, scenario);
        OrderRepository.QueryResult queryResult = orderRepository.findOrder(orderId, customerId, scenario);
        OrderRepository.QueryResult queueResult = orderRepository.listWorkQueue(scenario);
        return result("GET", "retrieve", orderId, customerId, scenario, queryResult, queueResult,
                "READ_ONLY", "NOT_REQUIRED", "NOT_REQUIRED", "RECORDED", "PROFILE_OK", "NOT_SENT");
    }

    @Span(value = "instana-sdk-lab.order.submit", type = Span.Type.INTERMEDIATE)
    public OrderResult submitOrder(@TagParam("order.id") String orderId,
                                   @TagParam("customer.id") String customerId,
                                   @TagParam("scenario") Scenario scenario) {
        SpanSupport.annotate("http.method", "POST");
        SpanSupport.annotate("order.operation", "submit");
        validateOrder(orderId, customerId, scenario);
        maybeRandomFailure(scenario);
        String riskStatus = runRiskScreening(orderId, customerId, scenario);
        String inventoryStatus = reserveInventory(orderId, scenario);
        OrderRepository.QueryResult upsertResult = orderRepository.upsertOrder(orderId, customerId, scenario);
        auditRequest(orderId, "POST_SUBMIT", "Order submitted by customer " + customerId);
        String notificationStatus = sendNotification(orderId, "ORDER_SUBMITTED");
        return result("POST", "submit", orderId, customerId, scenario, upsertResult, null,
                "SUBMITTED", inventoryStatus, "PENDING_APPROVAL", "RECORDED", riskStatus, notificationStatus);
    }

    @Span(value = "instana-sdk-lab.order.approve", type = Span.Type.INTERMEDIATE)
    public OrderResult approveOrder(@TagParam("order.id") String orderId,
                                    @TagParam("customer.id") String customerId,
                                    @TagParam("scenario") Scenario scenario) {
        SpanSupport.annotate("http.method", "PUT");
        SpanSupport.annotate("order.operation", "approve");
        validateOrder(orderId, customerId, scenario);
        maybeRandomFailure(scenario);
        String inventoryStatus = reserveInventory(orderId, scenario);
        String paymentStatus = authorizePayment(orderId, customerId, scenario);
        String fulfillmentStatus = scheduleFulfillment(orderId, scenario);
        OrderRepository.QueryResult updateResult = orderRepository.updateOrderStatus(orderId, scenario, "APPROVED");
        auditRequest(orderId, "PUT_APPROVE", "Order approved for fulfillment");
        String notificationStatus = sendNotification(orderId, "ORDER_APPROVED");
        return result("PUT", "approve", orderId, customerId, scenario, updateResult, null,
                paymentStatus, inventoryStatus, fulfillmentStatus, "RECORDED", "APPROVED", notificationStatus);
    }

    @Span(value = "instana-sdk-lab.order.cancel", type = Span.Type.INTERMEDIATE)
    public OrderResult cancelOrder(@TagParam("order.id") String orderId,
                                   @TagParam("customer.id") String customerId,
                                   @TagParam("scenario") Scenario scenario) {
        SpanSupport.annotate("http.method", "DELETE");
        SpanSupport.annotate("order.operation", "cancel");
        validateOrder(orderId, customerId, scenario);
        maybeRandomFailure(scenario);
        releaseInventory(orderId, scenario);
        OrderRepository.QueryResult updateResult = orderRepository.updateOrderStatus(orderId, scenario, "CANCELLED");
        auditRequest(orderId, "DELETE_CANCEL", "Order cancellation accepted");
        String notificationStatus = sendNotification(orderId, "ORDER_CANCELLED");
        return result("DELETE", "cancel", orderId, customerId, scenario, updateResult, null,
                "REFUND_PENDING", "RELEASED", "CANCELLED", "RECORDED", "NOT_REQUIRED", notificationStatus);
    }

    @Span(value = "instana-sdk-lab.order.handle", type = Span.Type.INTERMEDIATE)
    public OrderResult handleOrder(@TagParam("order.id") String orderId,
                                   @TagParam("customer.id") String customerId,
                                   @TagParam("scenario") Scenario scenario) {
        SpanSupport.annotate("order.id", orderId);
        SpanSupport.annotate("customer.id", customerId);
        SpanSupport.annotate("scenario", scenario.getCode());

        validateOrder(orderId, customerId, scenario);
        maybeRandomFailure(scenario);
        String inventoryStatus = reserveInventory(orderId, scenario);
        String paymentStatus = authorizePayment(orderId, customerId, scenario);
        OrderRepository.QueryResult queryResult = orderRepository.findOrder(orderId, customerId, scenario);

        OrderResult result = new OrderResult();
        result.setStatus("OK");
        result.setOperation("legacy-handle");
        result.setHttpMethod("GET");
        result.setOrderId(orderId);
        result.setCustomerId(customerId);
        result.setScenario(scenario.getCode());
        result.setInventoryStatus(inventoryStatus);
        result.setPaymentStatus(paymentStatus);
        result.setQueryRows(queryResult.getRows());
        result.setQueryMillis(queryResult.getElapsedMillis());
        return result;
    }

    @Span(value = "instana-sdk-lab.order.validate", type = Span.Type.INTERMEDIATE)
    public void validateOrder(@TagParam("order.id") String orderId,
                              @TagParam("customer.id") String customerId,
                              @TagParam("scenario") Scenario scenario) {
        if (orderId == null || orderId.trim().length() == 0) {
            SpanSupport.annotate("error.type", "business");
            throw new BusinessException("order id is required");
        }
        if (customerId == null || customerId.trim().length() == 0) {
            SpanSupport.annotate("error.type", "business");
            throw new BusinessException("customer id is required");
        }
        if (scenario == Scenario.BUSINESS_ERROR) {
            SpanSupport.annotate("error.type", "business");
            throw new BusinessException("business rule rejected order " + orderId);
        }
    }

    @Span(value = "instana-sdk-lab.inventory.reserve", type = Span.Type.INTERMEDIATE)
    public String reserveInventory(@TagParam("order.id") String orderId,
                                   @TagParam("scenario") Scenario scenario) {
        simulateWork(80L, 180L);
        if (scenario == Scenario.SYSTEM_ERROR) {
            SpanSupport.annotate("error.type", "system");
            throw new IllegalStateException("inventory service timeout for order " + orderId);
        }
        return "RESERVED";
    }

    @Span(value = "instana-sdk-lab.inventory.release", type = Span.Type.INTERMEDIATE)
    public String releaseInventory(@TagParam("order.id") String orderId,
                                   @TagParam("scenario") Scenario scenario) {
        simulateWork(45L, 110L);
        if (scenario == Scenario.SYSTEM_ERROR) {
            SpanSupport.annotate("error.type", "system");
            throw new IllegalStateException("inventory release failed for order " + orderId);
        }
        return "RELEASED";
    }

    @Span(value = "instana-sdk-lab.payment.authorize", type = Span.Type.INTERMEDIATE)
    public String authorizePayment(@TagParam("order.id") String orderId,
                                   @TagParam("customer.id") String customerId,
                                   @TagParam("scenario") Scenario scenario) {
        simulateWork(60L, 140L);
        SpanSupport.annotate("payment.provider", "fake-card-gateway");
        return "AUTHORIZED";
    }

    @Span(value = "instana-sdk-lab.risk.screen-order", type = Span.Type.INTERMEDIATE)
    public String runRiskScreening(@TagParam("order.id") String orderId,
                                   @TagParam("customer.id") String customerId,
                                   @TagParam("scenario") Scenario scenario) {
        simulateWork(35L, 95L);
        SpanSupport.annotate("risk.engine", "mock-risk-v2");
        if (scenario == Scenario.BUSINESS_ERROR) {
            SpanSupport.annotate("error.type", "business");
            throw new BusinessException("risk engine rejected order " + orderId);
        }
        return "LOW_RISK";
    }

    @Span(value = "instana-sdk-lab.customer.enrich-profile", type = Span.Type.INTERMEDIATE)
    public String enrichCustomerProfile(@TagParam("customer.id") String customerId,
                                        @TagParam("scenario") Scenario scenario) {
        simulateWork(20L, 70L);
        SpanSupport.annotate("customer.segment", "enterprise");
        return "PROFILE_OK";
    }

    @Span(value = "instana-sdk-lab.fulfillment.schedule", type = Span.Type.INTERMEDIATE)
    public String scheduleFulfillment(@TagParam("order.id") String orderId,
                                      @TagParam("scenario") Scenario scenario) {
        simulateWork(55L, 130L);
        SpanSupport.annotate("fulfillment.region", "north-warehouse");
        return "SCHEDULED";
    }

    @Span(value = "instana-sdk-lab.audit.write", type = Span.Type.INTERMEDIATE)
    public String auditRequest(@TagParam("order.id") String orderId,
                               @TagParam("order.action") String action,
                               String detail) {
        orderRepository.insertAudit(orderId, action, detail);
        return "RECORDED";
    }

    @Span(value = "instana-sdk-lab.notification.send", type = Span.Type.INTERMEDIATE)
    public String sendNotification(@TagParam("order.id") String orderId,
                                   @TagParam("notification.type") String notificationType) {
        simulateWork(25L, 85L);
        SpanSupport.annotate("notification.channel", "email");
        return "SENT";
    }

    @Span(value = "instana-sdk-lab.random.failure-check", type = Span.Type.INTERMEDIATE)
    public void maybeRandomFailure(@TagParam("scenario") Scenario scenario) {
        if (scenario == Scenario.RANDOM_ERROR && random.nextInt(100) < 45) {
            SpanSupport.annotate("error.type", "random");
            throw new IllegalStateException("random downstream failure was triggered");
        }
    }

    private void simulateWork(long minMillis, long maxMillis) {
        long range = Math.max(1L, maxMillis - minMillis);
        long delay = minMillis + Math.abs(random.nextLong() % range);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while simulating work", e);
        }
    }

    private OrderResult result(String httpMethod, String operation, String orderId, String customerId, Scenario scenario,
                               OrderRepository.QueryResult primaryQuery,
                               OrderRepository.QueryResult secondaryQuery,
                               String paymentStatus,
                               String inventoryStatus,
                               String fulfillmentStatus,
                               String auditStatus,
                               String riskStatus,
                               String notificationStatus) {
        int rows = 0;
        long queryMillis = 0L;
        if (primaryQuery != null) {
            rows += primaryQuery.getRows();
            queryMillis += primaryQuery.getElapsedMillis();
        }
        if (secondaryQuery != null) {
            rows += secondaryQuery.getRows();
            queryMillis += secondaryQuery.getElapsedMillis();
        }

        OrderResult result = new OrderResult();
        result.setStatus("OK");
        result.setOperation(operation);
        result.setHttpMethod(httpMethod);
        result.setOrderId(orderId);
        result.setCustomerId(customerId);
        result.setScenario(scenario.getCode());
        result.setPaymentStatus(paymentStatus);
        result.setInventoryStatus(inventoryStatus);
        result.setFulfillmentStatus(fulfillmentStatus);
        result.setAuditStatus(auditStatus);
        result.setRiskStatus(riskStatus);
        result.setNotificationStatus(notificationStatus);
        result.setQueryRows(rows);
        result.setQueryMillis(queryMillis);
        return result;
    }
}
