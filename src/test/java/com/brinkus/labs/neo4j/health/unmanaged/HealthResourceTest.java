package com.brinkus.labs.neo4j.health.unmanaged;

import com.brinkus.labs.neo4j.health.type.Health;
import com.brinkus.labs.neo4j.health.type.HealthStatusCode;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HealthResourceTest {

    private class HealthCheckDeserializer extends StdDeserializer<Health> {

        public HealthCheckDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public Health deserialize(
                final JsonParser jsonParser,
                final DeserializationContext deserializationContext
        ) throws IOException {
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);

            Health.Builder builder = new Health.Builder()
                    .status(HealthStatusCode.valueOf(node.get("status").asText()))
                    .withDetail("description", node.get("description").asText());
            if (node.has("error")) {
                builder.withDetail("error", node.get("error").asText());
            }
            return builder.build();
        }

    }

    private final ObjectMapper mapper;

    private DatabaseManagementService dbms;

    private GraphDatabaseService service;

    private Transaction transaction;

    private HealthResource healthCheckResource;

    public HealthResourceTest() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Health.class, new HealthCheckDeserializer(Health.class));
        mapper.registerModule(module);
    }

    @Before
    public void before() {
        this.transaction = mock(Transaction.class);
        this.service = mock(GraphDatabaseService.class);
        when(this.service.beginTx()).thenReturn(transaction);
        this.dbms = mock(DatabaseManagementService.class);
        when(this.dbms.database(anyString())).thenReturn(service);
        this.healthCheckResource = new HealthResource(dbms);
    }

    @Test
    public void serviceUp() throws Exception {
        when(service.executeTransactionally(anyString(), anyMap(), any())).thenReturn(1L);
        when(service.isAvailable(1000)).thenReturn(true);

        Response response = healthCheckResource.health("neo4j");
        assertThat(response.getStatus(), is(200));
        Health health = mapper.readValue(((byte[]) response.getEntity()), Health.class);

        assertThat(health.getStatus().getCode(), is(HealthStatusCode.UP));
        assertThat(health.getDetails().get("description"), is("Neo4j health check was successful."));
    }

    @Test
    public void outOfService() throws Exception {
        when(service.executeTransactionally(anyString(), anyMap(), any())).thenReturn(0L);
        when(service.isAvailable(1000)).thenReturn(true);

        Response response = healthCheckResource.health("neo4j");
        assertThat(response.getStatus(), is(200));
        Health health = mapper.readValue(((byte[]) response.getEntity()), Health.class);

        assertThat(health.getStatus().getCode(), is(HealthStatusCode.OUT_OF_SERVICE));
        assertThat(health.getDetails().size(), is(1));
        assertThat(health.getDetails().get("description"), is("Neo4j has no available nodes!"));
    }

    @Test
    public void down() throws Exception {
        when(service.isAvailable(1000)).thenReturn(false);

        Response response = healthCheckResource.health("neo4j");
        assertThat(response.getStatus(), is(200));
        Health health = mapper.readValue(((byte[]) response.getEntity()), Health.class);

        assertThat(health.getStatus().getCode(), is(HealthStatusCode.DOWN));
        assertThat(health.getDetails().size(), is(1));
        assertThat(health.getDetails().get("description"), is("Neo4j health check result was invalid!"));
    }

    @Test
    public void exception() throws Exception {
        when(service.isAvailable(1000)).thenThrow(new NullPointerException("null"));

        Response response = healthCheckResource.health("neo4j");
        assertThat(response.getStatus(), is(200));
        Health health = mapper.readValue(((byte[]) response.getEntity()), Health.class);

        assertThat(health.getStatus().getCode(), is(HealthStatusCode.OUT_OF_SERVICE));
        assertThat(health.getDetails().size(), is(2));
        assertThat(health.getDetails().get("description"), is("Neo4j health check failed!"));
        assertThat(health.getDetails().get("error"), is("java.lang.NullPointerException: null"));
    }

}
