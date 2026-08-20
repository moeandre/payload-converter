package io.payloadconverter.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Teste ponta-a-ponta via API HTTP, usando o fluxo de exemplo empacotado em src/main/resources/mappings. */
@SpringBootTest
@AutoConfigureMockMvc
class ConversionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveListarFluxosDisponiveis() throws Exception {
        mockMvc.perform(get("/convert"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'sistemaA-para-sistemaB')]").exists());
    }

    @Test
    void deveConverterViaApi() throws Exception {
        String payload = """
                {
                  "documento": { "numero": "12345678900", "tipo": "CPF" },
                  "cliente": { "primeiroNome": "Ana", "sobrenome": "Souza", "nascimento": "1990-05-20" },
                  "veiculo": { "valorFipe": 45000.0 },
                  "condutor": { "classeBonus": 5 },
                  "produtos": [ { "codigo": "COB-CASCO", "qtd": 1 } ]
                }
                """;

        mockMvc.perform(post("/convert/sistemaA-para-sistemaB")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroCpf").value("12345678900"))
                .andExpect(jsonPath("$.cliente.tipoPessoa").value("PESSOA_FISICA"))
                .andExpect(jsonPath("$.itens[0].sku").value("COB-CASCO"));
    }

    @Test
    void deveRetornar404ParaFluxoInexistente() throws Exception {
        mockMvc.perform(post("/convert/nao-existe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornar422QuandoCampoObrigatorioAusente() throws Exception {
        mockMvc.perform(post("/convert/sistemaA-para-sistemaB")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documento\":{\"tipo\":\"CPF\"}}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
