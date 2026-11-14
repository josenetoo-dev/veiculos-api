package com.josenetoo_dev.veiculos_api;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Configuration for opt-in MySQL integration tests. Only a loopback test endpoint is accepted. */
record MySqlTestConfiguration(String host, int port, String user, String password) {
    private static final Pattern URL = Pattern.compile(
            "jdbc:mysql://127\\.0\\.0\\.1:([0-9]{1,5})/phase1_test");

    static MySqlTestConfiguration fromEnvironment() {
        return from(System.getenv());
    }

    static boolean isCompletelyAbsent(Map<String, String> environment) {
        return environment.get("MYSQL_TEST_URL") == null
                && environment.get("MYSQL_TEST_USER") == null
                && environment.get("MYSQL_TEST_PASSWORD") == null;
    }

    static MySqlTestConfiguration from(Map<String, String> environment) {
        String url = required(environment, "MYSQL_TEST_URL");
        String user = required(environment, "MYSQL_TEST_USER");
        String password = required(environment, "MYSQL_TEST_PASSWORD");

        Matcher matcher = URL.matcher(url);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "MYSQL_TEST_URL deve apontar para jdbc:mysql://127.0.0.1:<porta>/phase1_test, sem parâmetros adicionais");
        }

        int port = Integer.parseInt(matcher.group(1));
        if (port < 1 || port > 65535 || port == 3306) {
            throw new IllegalArgumentException("MYSQL_TEST_URL deve usar uma porta TCP válida diferente de 3306");
        }
        return new MySqlTestConfiguration("127.0.0.1", port, user, password);
    }

    String schemaUrl(String purpose) {
        if (purpose == null || !purpose.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException("Finalidade de schema de teste inválida");
        }
        String schema = "phase1_test_" + purpose + "_" + UUID.randomUUID().toString().replace("-", "");
        return "jdbc:mysql://" + host + ":" + port + "/" + schema + "?createDatabaseIfNotExist=true";
    }

    private static String required(Map<String, String> environment, String key) {
        String value = environment.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " é obrigatório para habilitar os testes MySQL");
        }
        return value;
    }
}
