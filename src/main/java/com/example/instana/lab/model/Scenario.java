package com.example.instana.lab.model;

public enum Scenario {
    NORMAL("normal"),
    SLOW_QUERY("slow-query"),
    BAD_QUERY("bad-query"),
    SLOW_METHOD("slow-method"),
    BUSINESS_ERROR("business-error"),
    SYSTEM_ERROR("system-error"),
    RANDOM_ERROR("random-error");

    private final String code;

    Scenario(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static Scenario fromCode(String code) {
        if (code != null) {
            for (Scenario scenario : values()) {
                if (scenario.code.equalsIgnoreCase(code)) {
                    return scenario;
                }
            }
        }
        return NORMAL;
    }

    public static String[] names() {
        Scenario[] scenarios = values();
        String[] names = new String[scenarios.length];
        for (int i = 0; i < scenarios.length; i++) {
            names[i] = scenarios[i].getCode();
        }
        return names;
    }
}
