package com.lynxtrip.api;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", true);
        body.put("service", "lynxtrip-backend");
        body.put("ts", Instant.now().toString());
        body.put("database", probeDatabase());
        return body;
    }

    private Map<String, Object> probeDatabase() {
        Map<String, Object> m = new LinkedHashMap<>();
        try (Connection c = dataSource.getConnection()) {
            String product = c.getMetaData().getDatabaseProductName();
            m.put("kind", databaseKind(product));
            try (Statement st = c.createStatement();
                    ResultSet rs = st.executeQuery("SELECT 1")) {
                if (!rs.next()) {
                    throw new IllegalStateException("SELECT 1 returned no row");
                }
            }
            m.put("ok", true);
        } catch (Exception e) {
            if (!m.containsKey("kind")) {
                m.put("kind", "unknown");
            }
            m.put("ok", false);
            m.put("error", e.getClass().getSimpleName());
        }
        return m;
    }

    private static String databaseKind(String productName) {
        if (productName == null) {
            return "unknown";
        }
        String p = productName.toLowerCase(Locale.ROOT);
        if (p.contains("mysql")) {
            return "mysql";
        }
        if (p.contains("h2")) {
            return "h2";
        }
        return p.replace(' ', '_');
    }
}
