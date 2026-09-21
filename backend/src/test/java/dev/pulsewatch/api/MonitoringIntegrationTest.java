package dev.pulsewatch.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import dev.pulsewatch.repo.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:integration;DB_CLOSE_DELAY=-1",
    "pulsewatch.collector.interval-ms=3600000", "pulsewatch.collector.retries=0",
    "pulsewatch.alert.down-incident-minutes=0"
})
@ActiveProfiles("local")
@AutoConfigureMockMvc
class MonitoringIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ServiceRepository services;
    @Autowired HealthCheckRepository checks;
    @Autowired MetricRepository metrics;
    @Autowired AlertRepository alerts;
    @Autowired IncidentRepository incidents;
    HttpServer target;
    AtomicInteger code = new AtomicInteger(200);

    @BeforeEach void setup() throws Exception {
        incidents.deleteAll(); alerts.deleteAll(); metrics.deleteAll(); checks.deleteAll(); services.deleteAll();
        target = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        target.createContext("/health", exchange -> {
            byte[] body = "{\"status\":\"UP\"}".getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(code.get(), body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        target.start();
    }
    @AfterEach void teardown() { target.stop(0); }
    String body(String name, boolean enabled) throws Exception {
        return json.writeValueAsString(Map.of("name", name, "host", "127.0.0.1", "port", target.getAddress().getPort(), "environment", "test", "healthEndpoint", "/health", "enabled", enabled));
    }
    String create(String name) throws Exception {
        String response = mvc.perform(post("/api/services").contentType("application/json").content(body(name, true))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asText();
    }
    @Test void dashboardAndHealthRequireNoLogin() throws Exception {
        mvc.perform(get("/api/dashboard/summary")).andExpect(status().isOk()).andExpect(jsonPath("$.totalServices").value(0));
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
    @Test void measuredChecksSerializeAndAccumulate() throws Exception {
        String id = create("healthy");
        for (int index=0; index<2; index++) mvc.perform(post("/api/services/"+id+"/check")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/api/services/"+id+"/health")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].httpStatus").value(200));
        mvc.perform(get("/api/services/"+id+"/metrics")).andExpect(status().isOk()).andExpect(jsonPath("$.history[0].requestCount").value(2)).andExpect(jsonPath("$.uptimePercent").value(100));
    }
    @Test void failuresDeduplicateAndRecoveryClosesIncident() throws Exception {
        String id = create("failing"); code.set(503);
        for(int index=0; index<2; index++) mvc.perform(post("/api/services/"+id+"/check")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DOWN"));
        assertEquals(1, alerts.count()); assertEquals(1, incidents.count());
        String alertId = alerts.findAll().get(0).getId().toString();
        mvc.perform(put("/api/alerts/"+alertId+"/acknowledge")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));
        code.set(200);
        mvc.perform(post("/api/services/"+id+"/check")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/api/alerts")).andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("RESOLVED"));
        mvc.perform(get("/api/incidents")).andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("RESOLVED"));
        mvc.perform(put("/api/alerts/"+alertId+"/acknowledge")).andExpect(status().isConflict());
    }
    @Test void deleteCascadesAllRecordedData() throws Exception {
        String id = create("delete-me"); code.set(503);
        mvc.perform(post("/api/services/"+id+"/check")).andExpect(status().isOk());
        mvc.perform(delete("/api/services/"+id)).andExpect(status().isNoContent());
        assertEquals(0, services.count()); assertEquals(0, checks.count()); assertEquals(0, metrics.count()); assertEquals(0, alerts.count()); assertEquals(0, incidents.count());
    }
    @Test void validatesNamesAndPausedServices() throws Exception {
        String id = create("one"); create("two");
        mvc.perform(put("/api/services/"+id).contentType("application/json").content(body("two", true))).andExpect(status().isConflict());
        mvc.perform(put("/api/services/"+id).contentType("application/json").content(body("one", false))).andExpect(status().isOk());
        mvc.perform(post("/api/services/"+id+"/check")).andExpect(status().isConflict());
        mvc.perform(post("/api/services").contentType("application/json").content(body("invalid", true).replace("127.0.0.1", "http://localhost"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/services/not-a-uuid")).andExpect(status().isBadRequest());
    }
}
