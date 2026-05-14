package com.example.instana.lab.api;

import com.example.instana.lab.model.OrderResult;
import com.example.instana.lab.model.Scenario;
import com.example.instana.lab.service.BusinessException;
import com.example.instana.lab.service.OrderService;
import com.example.instana.lab.util.JsonUtil;
import com.instana.sdk.annotation.Span;
import com.instana.sdk.support.SpanSupport;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class LabApiServlet extends HttpServlet {

    private final OrderService orderService = new OrderService();

    @Override
    @Span(value = "instana-sdk-lab.servlet.request", type = Span.Type.ENTRY, capturedStackFrames = 1)
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getPathInfo();
        if (path == null || "/".equals(path)) {
            writeJson(response, HttpServletResponse.SC_OK, root());
            return;
        }
        if ("/health".equals(path)) {
            writeJson(response, HttpServletResponse.SC_OK, health());
            return;
        }
        if ("/scenarios".equals(path)) {
            writeJson(response, HttpServletResponse.SC_OK, scenarios());
            return;
        }
        if (path.startsWith("/orders/")) {
            OrderPath orderPath = parseOrderPath(path);
            handleOrder(request, response, orderPath.orderId, "GET", orderPath.operation);
            return;
        }
        writeJson(response, HttpServletResponse.SC_NOT_FOUND, notFound(path));
    }

    @Override
    @Span(value = "instana-sdk-lab.servlet.post", type = Span.Type.ENTRY, capturedStackFrames = 1)
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        routeOrderMutation(request, response, "POST", "submit");
    }

    @Override
    @Span(value = "instana-sdk-lab.servlet.put", type = Span.Type.ENTRY, capturedStackFrames = 1)
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        routeOrderMutation(request, response, "PUT", "approve");
    }

    @Override
    @Span(value = "instana-sdk-lab.servlet.delete", type = Span.Type.ENTRY, capturedStackFrames = 1)
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        routeOrderMutation(request, response, "DELETE", "cancel");
    }

    private String root() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("application", "instana-sdk-lab");
        body.put("health", "/api/health");
        body.put("scenarios", "/api/scenarios");
        body.put("retrieveWorkspace", "/api/orders/ORD-1001/workspace");
        body.put("submitOrder", "/api/orders/ORD-1001/submission");
        body.put("approveOrder", "/api/orders/ORD-1001/approval");
        body.put("cancelOrder", "/api/orders/ORD-1001/cancellation");
        return JsonUtil.object(body);
    }

    private String health() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("status", "UP");
        body.put("application", "instana-sdk-lab");
        body.put("api", "servlet");
        return JsonUtil.object(body);
    }

    private String scenarios() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("scenarios", Scenario.names());
        body.put("defaultScenario", Scenario.NORMAL.getCode());
        body.put("example", "/api/orders/ORD-1001/workspace?customerId=CUST-88&scenario=slow-query");
        body.put("methods", new String[]{"GET workspace", "POST submission", "PUT approval", "DELETE cancellation"});
        return JsonUtil.object(body);
    }

    private String notFound(String path) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("status", "ERROR");
        body.put("message", "No API route for " + path);
        return JsonUtil.object(body);
    }

    private void routeOrderMutation(HttpServletRequest request, HttpServletResponse response,
                                    String httpMethod, String operation) throws IOException {
        String path = request.getPathInfo();
        if (path != null && path.startsWith("/orders/")) {
            OrderPath orderPath = parseOrderPath(path);
            handleOrder(request, response, orderPath.orderId, httpMethod, orderPath.operation);
            return;
        }
        writeJson(response, HttpServletResponse.SC_NOT_FOUND, notFound(path));
    }

    private void handleOrder(HttpServletRequest request, HttpServletResponse response, String orderId,
                             String httpMethod, String operation) throws IOException {
        long startedAt = System.currentTimeMillis();
        if (operation == null || operation.trim().length() == 0) {
            operation = defaultOperation(httpMethod);
        }
        String customerId = valueOrDefault(request.getParameter("customerId"), "CUST-100");
        Scenario scenario = Scenario.fromCode(valueOrDefault(request.getParameter("scenario"), "normal"));

        SpanSupport.annotate("http.method", httpMethod);
        SpanSupport.annotate("http.route", "/api/orders/{orderId}/" + pathFromOperation(operation));
        SpanSupport.annotate("request.name", httpMethod + " " + operation);
        SpanSupport.annotate("order.operation", operation);
        SpanSupport.annotate("order.id", orderId);
        SpanSupport.annotate("customer.id", customerId);
        SpanSupport.annotate("scenario", scenario.getCode());

        try {
            OrderResult result = executeOperation(httpMethod, orderId, customerId, scenario);
            result.setElapsedMillis(System.currentTimeMillis() - startedAt);
            result.setTraceActive(SpanSupport.isTracing());
            result.setTraceId(SpanSupport.traceId());
            result.setSpanId(SpanSupport.spanId());
            writeJson(response, HttpServletResponse.SC_OK, JsonUtil.orderResult(result));
        } catch (BusinessException e) {
            SpanSupport.annotate("error.type", "business");
            SpanSupport.annotate("error.message", e.getMessage());
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    error(orderId, customerId, scenario, "business", e));
        } catch (RuntimeException e) {
            SpanSupport.annotate("error.type", "system");
            SpanSupport.annotate("error.message", e.getMessage());
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    error(orderId, customerId, scenario, "system", e));
        }
    }

    private OrderResult executeOperation(String httpMethod, String orderId, String customerId, Scenario scenario) {
        if ("POST".equals(httpMethod)) {
            return orderService.submitOrder(orderId, customerId, scenario);
        }
        if ("PUT".equals(httpMethod)) {
            return orderService.approveOrder(orderId, customerId, scenario);
        }
        if ("DELETE".equals(httpMethod)) {
            return orderService.cancelOrder(orderId, customerId, scenario);
        }
        return orderService.retrieveWorklist(orderId, customerId, scenario);
    }

    private OrderPath parseOrderPath(String path) {
        String remainder = path.substring("/orders/".length());
        int slash = remainder.indexOf('/');
        if (slash < 0) {
            return new OrderPath(remainder, null);
        }
        String orderId = remainder.substring(0, slash);
        String operationPath = remainder.substring(slash + 1);
        return new OrderPath(orderId, operationFromPath(operationPath));
    }

    private String operationFromPath(String operationPath) {
        if ("workspace".equals(operationPath)) {
            return "retrieve-workspace";
        }
        if ("submission".equals(operationPath)) {
            return "submit-order";
        }
        if ("approval".equals(operationPath)) {
            return "approve-order";
        }
        if ("cancellation".equals(operationPath)) {
            return "cancel-order";
        }
        return operationPath;
    }

    private String pathFromOperation(String operation) {
        if ("retrieve-workspace".equals(operation)) {
            return "workspace";
        }
        if ("submit-order".equals(operation)) {
            return "submission";
        }
        if ("approve-order".equals(operation)) {
            return "approval";
        }
        if ("cancel-order".equals(operation)) {
            return "cancellation";
        }
        return operation;
    }

    private String defaultOperation(String httpMethod) {
        if ("POST".equals(httpMethod)) {
            return "submit-order";
        }
        if ("PUT".equals(httpMethod)) {
            return "approve-order";
        }
        if ("DELETE".equals(httpMethod)) {
            return "cancel-order";
        }
        return "retrieve-workspace";
    }

    private String error(String orderId, String customerId, Scenario scenario, String errorType, Exception exception) {
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
        return JsonUtil.object(body);
    }

    private String valueOrDefault(String value, String defaultValue) {
        if (value == null || value.trim().length() == 0) {
            return defaultValue;
        }
        return value;
    }

    private void writeJson(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(body);
    }

    private static class OrderPath {
        private final String orderId;
        private final String operation;

        private OrderPath(String orderId, String operation) {
            this.orderId = orderId;
            this.operation = operation;
        }
    }
}
