package com.uma.example.springuma.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uma.example.springuma.integration.base.AbstractIntegration;
import com.uma.example.springuma.model.Medico;

public class MedicoControllerMockMvcIT extends AbstractIntegration {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Medico medico;

    @BeforeEach
    void setUp() {
        medico = new Medico();
        medico.setId(1L);
        medico.setDni("835");
        medico.setNombre("Miguel");
        medico.setEspecialidad("Ginecologia");
    }

    private void crearMedico(Medico medico) throws Exception {
        this.mockMvc.perform(post("/medico")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(medico)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Crear médico devuelve 201 Created")
    void crearMedico_devuelve201Created() throws Exception {
        crearMedico(medico);
    }

    @Test
    @DisplayName("Crear médico y recuperarlo por ID devuelve datos correctos")
    void crearMedico_recuperarPorId_devuelveDatosCorrectos() throws Exception {
        crearMedico(medico);

        mockMvc.perform(get("/medico/" + medico.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.nombre").value("Miguel"))
                .andExpect(jsonPath("$.dni").value("835"))
                .andExpect(jsonPath("$.especialidad").value("Ginecologia"));
    }

    @Test
    @DisplayName("Actualizar médico existente devuelve 204 No Content")
    void actualizarMedico_devuelve204NoContent() throws Exception {
        crearMedico(medico);

        medico.setNombre("Miguel Actualizado");
        medico.setEspecialidad("Cardiologia");

        mockMvc.perform(put("/medico")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(medico)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Actualizar médico y verificar que los datos cambian correctamente")
    void actualizarMedico_datosActualizadosCorrectamente() throws Exception {
        crearMedico(medico);

        medico.setNombre("Pedro Nuevo");
        medico.setEspecialidad("Oncologia");

        mockMvc.perform(put("/medico")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(medico)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/medico/" + medico.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Pedro Nuevo"))
                .andExpect(jsonPath("$.especialidad").value("Oncologia"));
    }

    @Test
    @DisplayName("Eliminar médico existente devuelve 200 OK")
    void eliminarMedico_devuelve200Ok() throws Exception {
        crearMedico(medico);

        mockMvc.perform(delete("/medico/" + medico.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Buscar médico por DNI devuelve el médico correcto")
    void buscarMedicoPorDni_devuelveMedicoCorecto() throws Exception {
        crearMedico(medico);

        mockMvc.perform(get("/medico/dni/" + medico.getDni()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.nombre").value("Miguel"))
                .andExpect(jsonPath("$.dni").value("835"));
    }

    @Test
    @DisplayName("Buscar médico por DNI inexistente devuelve 404 Not Found")
    void buscarMedicoPorDniInexistente_devuelve404() throws Exception {
        mockMvc.perform(get("/medico/dni/DNI_NO_EXISTE"))
                .andExpect(status().isNotFound());
    }

}
