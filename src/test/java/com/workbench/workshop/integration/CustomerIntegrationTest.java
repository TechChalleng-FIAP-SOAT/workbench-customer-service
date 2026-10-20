package com.workbench.workshop.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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

    @Test
    void clientAndVehicleFlow() throws Exception {
        // 1. cria cliente
        MvcResult created = mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"joao silva\",\"document\":\"529.982.247-25\","
                                + "\"email\":\"j@x.com\",\"phone\":\"1199\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("JOAO SILVA"))     // @UpperCase
                .andExpect(jsonPath("$.document").value("52998224725")) // normalizado
                .andReturn();
        long clientId = mapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        // 2. busca cliente por documento (aceita formatado)
        mockMvc.perform(get("/api/clients/document/529.982.247-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clientId));

        // 3. cria veículo vinculado ao cliente
        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":" + clientId + ",\"plate\":\"abc-1234\",\"brand\":\"VW\","
                                + "\"model\":\"Gol\",\"year\":2020,\"color\":\"prata\",\"type\":\"CAR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plate").value("ABC-1234"));

        // 4. busca veículo por placa e lista por cliente
        mockMvc.perform(get("/api/vehicles/plate/ABC1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brand").value("VW"));
        mockMvc.perform(get("/api/vehicles/client/" + clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // 5. lista clientes ativos
        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        // 6. atualiza cliente
        mockMvc.perform(put("/api/clients/" + clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"joao atualizado\",\"email\":\"novo@x.com\",\"phone\":\"2000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("novo@x.com"));

        // 7. busca veículo por id (usa o id retornado na criação)
        long vehicleId = mapper.readTree(mockMvc.perform(get("/api/vehicles/plate/ABC1234"))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        // 8. atualiza veículo
        mockMvc.perform(put("/api/vehicles/" + vehicleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plate\":\"ABC1234\",\"brand\":\"VW\",\"model\":\"Gol G6\",\"year\":2021,"
                                + "\"color\":\"PRETO\",\"type\":\"CAR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("Gol G6"));

        // 9. desativa veículo e cliente (soft delete → 204)
        mockMvc.perform(delete("/api/vehicles/" + vehicleId)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/clients/" + clientId)).andExpect(status().isNoContent());
    }

    @Test
    void vehicleNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/vehicles/888888")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/vehicles/plate/ZZZ9999")).andExpect(status().isNotFound());
    }

    @Test
    void invalidPlateReturns400() throws Exception {
        mockMvc.perform(get("/api/vehicles/plate/1234567")).andExpect(status().isBadRequest());
    }

    @Test
    void invalidDocumentReturns400() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"document\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateDocumentReturns409() throws Exception {
        String body = "{\"name\":\"A\",\"document\":\"11144477735\"}";
        mockMvc.perform(post("/api/clients").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/clients").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void clientNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/clients/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void vehicleForUnknownClientReturns404() throws Exception {
        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":999999,\"plate\":\"XYZ9876\",\"brand\":\"Fiat\","
                                + "\"model\":\"Uno\",\"year\":2019,\"color\":\"branco\",\"type\":\"CAR\"}"))
                .andExpect(status().isNotFound());
    }
}
