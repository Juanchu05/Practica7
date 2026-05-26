package com.uma.example.springuma.integration;

import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uma.example.springuma.integration.base.AbstractIntegration;
import com.uma.example.springuma.model.Medico;
import com.uma.example.springuma.model.MedicoService;
import com.uma.example.springuma.model.Paciente;

public class PacienteControllerMockMvcIT extends AbstractIntegration {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MedicoService medicoService;

    Paciente paciente;
    Medico medico;

    @BeforeEach
    void setUp() {
        medico = new Medico();
        medico.setNombre("Miguel");
        medico.setId(1L);
        medico.setDni("835");
        medico.setEspecialidad("Ginecologo");

        paciente = new Paciente();
        paciente.setId(1L);
        paciente.setNombre("Maria");
        paciente.setDni("888");
        paciente.setEdad(20);
        paciente.setCita("Ginecologia");
        paciente.setMedico(this.medico);
    }
    private void crearMedico(Medico medico) throws Exception {
        this.mockMvc.perform(post("/medico")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(medico)))
                .andExpect(status().isCreated());
    }
    private void crearPaciente(Paciente paciente) throws Exception {
        mockMvc.perform(post("/paciente")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(paciente)))
                .andExpect(status().isCreated());
    }

    private void getPacienteById(Long id, Paciente expected) throws Exception {
        mockMvc.perform(get("/paciente/" + id))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").exists())
                .andExpect(jsonPath("$").value(expected));
    }

    @Test
    @DisplayName("Crear paciente y recuperarlo por ID pasado por parametro")
    void savePaciente_RecuperaPacientePorId() throws Exception {
        crearMedico(medico);
        crearPaciente(paciente);

        // Obtener paciente por ID usando el método auxiliar proporcionado
        getPacienteById(paciente.getId(), paciente);
    }

    @Test
    @DisplayName("Crear paciente y verificar sus datos concretos en la respuesta JSON")
    void savePaciente_verificaDatosJson() throws Exception {
        crearMedico(medico);
        crearPaciente(paciente);

        mockMvc.perform(get("/paciente/" + paciente.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.nombre").value("Maria"))
                .andExpect(jsonPath("$.dni").value("888"))
                .andExpect(jsonPath("$.edad").value(20))
                .andExpect(jsonPath("$.cita").value("Ginecologia"));
    }

    @Test
    @DisplayName("Actualizar paciente y verificar que los datos cambian")
    void updatePaciente_datosActualizadosCorrectamente() throws Exception {
        crearMedico(medico);
        crearPaciente(paciente);

        paciente.setNombre("Maria Actualizada");
        paciente.setEdad(25);

        mockMvc.perform(put("/paciente")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(paciente)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/paciente/" + paciente.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Maria Actualizada"))
                .andExpect(jsonPath("$.edad").value(25));
    }

    @Test
    @DisplayName("Eliminar paciente existente devuelve 200 OK")
    void deletePaciente_devuelve200Ok() throws Exception {
        crearMedico(medico);
        crearPaciente(paciente);

        mockMvc.perform(delete("/paciente/" + paciente.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Asociar paciente a médico y listar pacientes del médico devuelve lista con 1 elemento")
    void getPacientesMedico_devuelveListaConUnPaciente() throws Exception {
        crearMedico(medico);
        crearPaciente(paciente);

        mockMvc.perform(get("/paciente/medico/" + medico.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Maria"))
                .andExpect(jsonPath("$[0].dni").value("888"));
    }

    @Test
    @DisplayName("Asociar dos pacientes al mismo médico y listar devuelve lista con 2 elementos")
    void getPacientesMedico_dosPacientes_devuelveListaConDos() throws Exception {
        crearMedico(medico);
        crearPaciente(paciente);

        Paciente paciente2 = new Paciente();
        paciente2.setId(2L);
        paciente2.setNombre("Juan");
        paciente2.setDni("999");
        paciente2.setEdad(35);
        paciente2.setCita("Oncologia");
        paciente2.setMedico(medico);
        crearPaciente(paciente2);

        mockMvc.perform(get("/paciente/medico/" + medico.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }


}
