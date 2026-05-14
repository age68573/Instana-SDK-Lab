package com.example.instana.lab.api;

import com.example.instana.lab.model.OrderResult;
import com.example.instana.lab.model.Scenario;
import com.example.instana.lab.service.BusinessException;
import com.example.instana.lab.service.OrderService;
import com.example.instana.lab.util.JsonUtil;
import com.instana.sdk.annotation.Span;
import com.instana.sdk.annotation.TagParam;
import com.instana.sdk.support.SpanSupport;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {

    private final OrderService orderService = new OrderService();

    @GET
    @Path("/health")
    public Response health() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("status", "UP");
        body.put("application", "instana-sdk-lab");
        return json(Response.Status.OK, JsonUtil.object(body));
    }

    @GET
    @Path("/scenarios")
    public Response scenarios() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("scenarios", Scenario.names());
        body.put("defaultScenario", Scenario.NORMAL.getCode());
        body.put("example", "/api/orders/ORD-1001?customerId=CUST-88&scenario=slow-query");
        return json(Response.Status.OK, JsonUtil.object(body));
    }

    @GET
    @Path("/orders/{orderId}")
    @Span(value = "instana-sdk-lab.order.request", type = Span.Type.ENTRY, capturedStackFrames = 1)
    public Response getOrder(
            @TagParam("order.id") @PathParam("orderId") String orderId,
            @TagParam("customer.id") @DefaultValue("CUST-100") @QueryParam("customerId") String customerId,
            @TagParam("scenario") @DefaultValue("normal") @QueryParam("scenario") String scenarioCode) {

        long startedAt = System.currentTimeMillis();
        Scenario scenario = Scenario.fromCode(scenarioCode);
        SpanSupport.annotate("order.id", orderId);
        SpanSupport.annotate("customer.id", customerId);
        SpanSupport.annotate("scenario", scenario.getCode());

        try {
            OrderResult result = orderService.handleOrder(orderId, customerId, scenario);
            result.setElapsedMillis(System.currentTimeMillis() - startedAt);
            result.setTraceActive(SpanSupport.isTracing());
            result.setTraceId(SpanSupport.traceId());
            result.setSpanId(SpanSupport.spanId());
            return json(Response.Status.OK, JsonUtil.orderResult(result));
        } catch (BusinessException e) {
            SpanSupport.annotate("error.type", "business");
            SpanSupport.annotate("error.message", e.getMessage());
            return error(Response.Status.BAD_REQUEST, orderId, customerId, scenario, "business", e);
        } catch (RuntimeException e) {
            SpanSupport.annotate("error.type", "system");
            SpanSupport.annotate("error.message", e.getMessage());
            return error(Response.Status.INTERNAL_SERVER_ERROR, orderId, customerId, scenario, "system", e);
        }
    }

    private Response error(Response.Status status, String orderId, String customerId, Scenario scenario,
                           String errorType, Exception exception) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("status", "ERROR");
        body.put("orderId", orderId);
        body.put("customerId", customerId);
        body.put("scenario", scenario.getCode());
        body.put("errorType", errorType);
        body.put("message", exception.getMessage());
        body.put("traceActive", SpanSupport.isTracing());
        body.put("traceId", SpanSupport.traceId());
        body.put("spanId", SpanSupport.spanId());
        return json(status, JsonUtil.object(body));
    }

    private Response json(Response.Status status, String body) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(body)
                .build();
    }
}
