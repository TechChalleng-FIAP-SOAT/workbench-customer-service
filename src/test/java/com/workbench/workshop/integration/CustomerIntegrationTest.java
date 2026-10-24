package com.workbench.workshop.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integração end-to-end do Customer Service com PostgreSQL real (Testcontainers):
 * cria cliente e veículo via REST, consulta por documento/placa, lista por cliente
 * e valida os caminhos de erro (documento inválido, duplicidade, não encontrado).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CustomerIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @BeforeAll
    static void requireDocker() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker indisponível — pulando teste de integração.");
    }

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String SECRET = "akmsdkmasfnjsgknakmsdkmasdkmsadfkmdsakmdkmsadsa";

    /** Token staff ADMIN (as rotas protegidas exigem token após a Estratégia 2). */
    private static String bearer() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        long now = System.currentTimeMillis();
        String token = Jwts.builder()
                .subject("admin@workbench.dev")
                .claim("roles", List.of("ADMIN"))
                .issuedAt(new Date(now))
                .expiration(new Date(now + 3600000))
                .signWith(key)
                .compact();
        return "Bearer " + token;
    }

    /** Anexa o header Authorization (ADMIN) a um request builder. */
    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", bearer());
    }

    @Test
    void clientAndVehicleFlow() throws Exception {
        // 1. cria cliente
        MvcResult created = mockMvc.perform(auth(post("/api/clients"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"joao silva\",\"document\":\"529.982.247-25\","
                                + "\"email\":\"j@x.com\",\"phone\":\"1199\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("JOAO SILVA"))     // @UpperCase
                .andExpect(jsonPath("$.document").value("52998224725")) // normalizado
                .andReturn();
        long clientId = mapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        // 2. busca cliente por documento (aceita formatado)
        mockMvc.perform(auth(get("/api/clients/document/529.982.247-25")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clientId));

        // 3. cria veículo vinculado ao cliente
        mockMvc.perform(auth(post("/api/vehicles"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":" + clientId + ",\"plate\":\"abc-1234\",\"brand\":\"VW\","
                                + "\"model\":\"Gol\",\"year\":2020,\"color\":\"prata\",\"type\":\"CAR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plate").value("ABC-1234"));

        // 4. busca veículo por placa e lista por cliente
        mockMvc.perform(auth(get("/api/vehicles/plate/ABC1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brand").value("VW"));
        mockMvc.perform(auth(get("/api/vehicles/client/" + clientId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // 5. lista clientes ativos
        mockMvc.perform(auth(get("/api/clients")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        // 6. atualiza cliente
        mockMvc.perform(auth(put("/api/clients/" + clientId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"joao atualizado\",\"email\":\"novo@x.com\",\"phone\":\"2000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("novo@x.com"));

        // 7. busca veículo por id (usa o id retornado na criação)
        long vehicleId = mapper.readTree(mockMvc.perform(auth(get("/api/vehicles/plate/ABC1234")))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        // 8. atualiza veículo
        mockMvc.perform(auth(put("/api/vehicles/" + vehicleId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plate\":\"ABC1234\",\"brand\":\"VW\",\"model\":\"Gol G6\",\"year\":2021,"
                                + "\"color\":\"PRETO\",\"type\":\"CAR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("Gol G6"));

        // 9. desativa veículo e cliente (soft delete → 204)
        mockMvc.perform(auth(delete("/api/vehicles/" + vehicleId))).andExpect(status().isNoContent());
        mockMvc.perform(auth(delete("/api/clients/" + clientId))).andExpect(status().isNoContent());
    }

    @Test
    void vehicleNotFoundReturns404() throws Exception {
        mockMvc.perform(auth(get("/api/vehicles/888888"))).andExpect(status().isNotFound());
        mockMvc.perform(auth(get("/api/vehicles/plate/ZZZ9999"))).andExpect(status().isNotFound());
    }

    @Test
    void invalidPlateReturns400() throws Exception {
        mockMvc.perform(auth(get("/api/vehicles/plate/1234567"))).andExpect(status().isBadRequest());
    }

    @Test
    void invalidDocumentReturns400() throws Exception {
        mockMvc.perform(auth(post("/api/clients"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"document\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateDocumentReturns409() throws Exception {
        String body = "{\"name\":\"A\",\"document\":\"11144477735\"}";
        mockMvc.perform(auth(post("/api/clients")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(auth(post("/api/clients")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void clientNotFoundReturns404() throws Exception {
        mockMvc.perform(auth(get("/api/clients/999999")))
                .andExpect(status().isNotFound());
    }

    @Test
    void vehicleForUnknownClientReturns404() throws Exception {
        mockMvc.perform(auth(post("/api/vehicles"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":999999,\"plate\":\"XYZ9876\",\"brand\":\"Fiat\","
                                + "\"model\":\"Uno\",\"year\":2019,\"color\":\"branco\",\"type\":\"CAR\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void protectedRouteWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/clients")).andExpect(status().isUnauthorized());
    }

    @Test
    void validateEndpointIsPublic() throws Exception {
        // rota pública (a Lambda valida credenciais antes de emitir o token): sem Authorization,
        // credenciais inválidas → 401 do caso de uso (não 401 de segurança/sem token).
        mockMvc.perform(post("/api/users/auth/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"naoexiste@x.com\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signupRequiresAdmin() throws Exception {
        // sem token → 401 (signup exige ADMIN)
        mockMvc.perform(post("/api/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"novo@x.com\",\"password\":\"segredo123\",\"roles\":[\"TECHNICIAN\"]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
