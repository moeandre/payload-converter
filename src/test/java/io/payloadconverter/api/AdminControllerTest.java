package io.payloadconverter.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveRecarregarConfiguracoesSobDemanda() throws Exception {
        mockMvc.perform(post("/admin/mappings/reload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recarregado").value(true))
                .andExpect(jsonPath("$.fluxos[0]").value("sistemaA-para-sistemaB"));
    }
}
