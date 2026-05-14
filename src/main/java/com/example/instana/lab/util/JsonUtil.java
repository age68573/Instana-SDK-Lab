package com.example.instana.lab.util;

import com.example.instana.lab.model.OrderResult;

import java.util.Iterator;
import java.util.Map;

public final class JsonUtil {

    private JsonUtil() {
    }

    public static String orderResult(OrderResult result) {
        StringBuilder json = new StringBuilder();
        json.append('{');
        append(json, "status", result.getStatus()).append(',');
        append(json, "orderId", result.getOrderId()).append(',');
        append(json, "customerId", result.getCustomerId()).append(',');
        append(json, "scenario", result.getScenario()).append(',');
        append(json, "paymentStatus", result.getPaymentStatus()).append(',');
        append(json, "inventoryStatus", result.getInventoryStatus()).append(',');
        append(json, "queryRows", result.getQueryRows()).append(',');
        append(json, "queryMillis", result.getQueryMillis()).append(',');
        append(json, "elapsedMillis", result.getElapsedMillis()).append(',');
        append(json, "traceActive", result.isTraceActive()).append(',');
        append(json, "traceId", result.getTraceId()).append(',');
        append(json, "spanId", result.getSpanId());
        json.append('}');
        return json.toString();
    }

    public static String object(Map<String, Object> values) {
        StringBuilder json = new StringBuilder();
        json.append('{');
        Iterator<Map.Entry<String, Object>> iterator = values.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            append(json, entry.getKey(), entry.getValue());
            if (iterator.hasNext()) {
                json.append(',');
            }
        }
        json.append('}');
        return json.toString();
    }

    private static StringBuilder append(StringBuilder json, String key, Object value) {
        json.append('"').append(escape(key)).append('"').append(':');
        if (value == null) {
            json.append("null");
        } else if (value instanceof Number || value instanceof Boolean) {
            json.append(value);
        } else if (value instanceof String[]) {
            appendArray(json, (String[]) value);
        } else {
            json.append('"').append(escape(String.valueOf(value))).append('"');
        }
        return json;
    }

    private static void appendArray(StringBuilder json, String[] values) {
        json.append('[');
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append('"').append(escape(values[i])).append('"');
        }
        json.append(']');
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    escaped.append(c);
                    break;
            }
        }
        return escaped.toString();
    }
}
