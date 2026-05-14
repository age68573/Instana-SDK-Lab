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
            handleOrder(request, response, path.substring("/orders/".length()));
            return;
        }
        writeJson(response, HttpServletResponse.SC_NOT_FOUND, notFound(path));
    }

    private String root() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("application", "instana-sdk-lab");
        body.put("health", "/api/health");
        body.put("scenarios", "/api/scenarios");
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
        body.put("example", "/api/orders/ORD-1001?customerId=CUST-88&scenario=slow-query");
        return JsonUtil.object(body);
    }

    private String notFound(String path) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("status", "ERROR");
        body.put("message", "No API route for " + path);
        return JsonUtil.object(body);
    }

    private void handleOrder(HttpServletRequest request, HttpServletResponse response, String orderId) throws IOException {
        long startedAt = System.currentTimeMillis();
        String customerId = valueOrDefault(request.getParameter("customerId"), "CUST-100");
        Scenario scenario = Scenario.fromCode(valueOrDefault(request.getParameter("scenario"), "normal"));

        SpanSupport.annotate("order.id", orderId);
        SpanSupport.annotate("customer.id", customerId);
        SpanSupport.annotate("scenario", scenario.getCode());

        try {
            OrderResult result = orderService.handleOrder(orderId, customerId, scenario);
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
}
