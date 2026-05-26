package com.uma.example.springuma.integration;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uma.example.springuma.integration.base.AbstractIntegration;
import com.uma.example.springuma.model.Imagen;
import com.uma.example.springuma.model.Informe;
import com.uma.example.springuma.model.Medico;
import com.uma.example.springuma.model.Paciente;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class InformeControllerWebTestClientIT extends AbstractIntegration {

    @LocalServerPort
    private Integer port;

    private WebTestClient testClient;

    private Medico medico;
    private Paciente paciente;
    private Imagen imagen;
    private Informe informe;

    @PostConstruct
    public void init() {
        testClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofMillis(300000)).build();
    }

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
        paciente.setMedico(medico);

        imagen = new Imagen();
        imagen.setId(1L);
        imagen.setPaciente(paciente);

        // Crea médico
        testClient.post().uri("/medico")
                .body(Mono.just(medico), Medico.class)
                .exchange()
                .expectStatus().isCreated();

        // Crea paciente
        testClient.post().uri("/paciente")
                .body(Mono.just(paciente), Paciente.class)
                .exchange()
                .expectStatus().isCreated();

        // Crea imagen
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("image", new FileSystemResource(Paths.get("src/test/resources/healthy.png").toFile()));
        builder.part("paciente", paciente);

        testClient.post().uri("/imagen")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchange()
                .expectStatus().isOk();

    }

    // ── Métodos auxiliares ─────────────────────────────────────────────────────

    /**
     * Obtiene el ID real de la imagen subida en setUp() consultando las imágenes del paciente.
     */
    private long obtenerIdImagenSubida() throws Exception {
        FluxExchangeResult<String> result = testClient.get()
                .uri("/imagen/paciente/" + paciente.getId())
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class);

        String body = result.getResponseBody().blockFirst();
        assertNotNull(body);

        ObjectMapper mapper = new ObjectMapper();
        List<Imagen> imagenes = mapper.readValue(body, new TypeReference<List<Imagen>>() {});
        assertFalse(imagenes.isEmpty(), "Debe existir al menos una imagen subida en setUp()");

        return imagenes.get(0).getId();
    }

    /**
     * Crea un informe asociado a la imagen con el ID dado y devuelve el informe persistido.
     */
    private Informe crearYRecuperarInforme(long imagenId, String prediccion, String contenido) throws Exception {
        Imagen imagenRef = new Imagen();
        imagenRef.setId(imagenId);
        imagenRef.setPaciente(paciente);

        Informe nuevoInforme = new Informe(prediccion, contenido, imagenRef);

        testClient.post().uri("/informe")
                .body(Mono.just(nuevoInforme), Informe.class)
                .exchange()
                .expectStatus().isCreated();

        FluxExchangeResult<String> result = testClient.get()
                .uri("/informe/imagen/" + imagenId)
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class);

        String body = result.getResponseBody().blockFirst();
        assertNotNull(body);

        ObjectMapper mapper = new ObjectMapper();
        List<Informe> informes = mapper.readValue(body, new TypeReference<List<Informe>>() {});
        assertFalse(informes.isEmpty(), "Debe existir el informe recién creado");

        return informes.get(0);
    }

    // ── Tests ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Crear informe asociado a una imagen devuelve 201 Created")
    void crearInforme_devuelve201Created() throws Exception {
        long imagenId = obtenerIdImagenSubida();

        Imagen imagenRef = new Imagen();
        imagenRef.setId(imagenId);
        imagenRef.setPaciente(paciente);

        Informe nuevoInforme = new Informe("No cancer", "Imagen sin indicios de cáncer", imagenRef);

        testClient.post().uri("/informe")
                .body(Mono.just(nuevoInforme), Informe.class)
                .exchange()
                .expectStatus().isCreated();
    }


    @Test
    @DisplayName("Listar informes de una imagen devuelve lista con 1 elemento")
    void listarInformesDeImagen_devuelveListaConUno() throws Exception {
        long imagenId = obtenerIdImagenSubida();
        crearYRecuperarInforme(imagenId, "No cancer", "Resultado normal");

        testClient.get().uri("/informe/imagen/" + imagenId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Informe.class)
                .hasSize(1);
    }

    @Test
    @DisplayName("Eliminar informe existente devuelve 204 No Content")
    void eliminarInforme_devuelve204NoContent() throws Exception {
        long imagenId = obtenerIdImagenSubida();
        Informe informeCreado = crearYRecuperarInforme(imagenId, "Cancer", "Imagen con anomalías detectadas");

        testClient.delete().uri("/informe/" + informeCreado.getId())
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("Crear dos informes para la misma imagen y listarlos devuelve lista con 2 elementos")
    void crearDosInformes_listarTodos_devuelveListaConDos() throws Exception {
        long imagenId = obtenerIdImagenSubida();

        Imagen imagenRef = new Imagen();
        imagenRef.setId(imagenId);
        imagenRef.setPaciente(paciente);

        Informe informe1 = new Informe("No cancer", "Primer análisis: sin riesgo", imagenRef);
        testClient.post().uri("/informe")
                .body(Mono.just(informe1), Informe.class)
                .exchange()
                .expectStatus().isCreated();

        Informe informe2 = new Informe("No cancer", "Segunda revisión: confirma sin riesgo", imagenRef);
        testClient.post().uri("/informe")
                .body(Mono.just(informe2), Informe.class)
                .exchange()
                .expectStatus().isCreated();

        testClient.get().uri("/informe/imagen/" + imagenId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Informe.class)
                .hasSize(2);
    }
}