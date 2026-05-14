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

    @Span(value = "instana-sdk-lab.payment.authorize", type = Span.Type.INTERMEDIATE)
    public String authorizePayment(@TagParam("order.id") String orderId,
                                   @TagParam("customer.id") String customerId,
                                   @TagParam("scenario") Scenario scenario) {
        simulateWork(60L, 140L);
        SpanSupport.annotate("payment.provider", "fake-card-gateway");
        return "AUTHORIZED";
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
}
