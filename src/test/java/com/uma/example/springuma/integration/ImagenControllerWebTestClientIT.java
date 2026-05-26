package com.uma.example.springuma.integration;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uma.example.springuma.model.Imagen;
import com.uma.example.springuma.model.Medico;
import com.uma.example.springuma.model.Paciente;
import com.uma.example.springuma.integration.base.AbstractIntegration;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

import org.springframework.web.reactive.function.BodyInserters;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ImagenControllerWebTestClientIT extends AbstractIntegration {

    @LocalServerPort
    private Integer port;

    private WebTestClient testClient;

    private Paciente paciente;
    private Medico medico;

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
    }

    private void subirImagen(String nombreArchivo) {
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("image", new FileSystemResource(Paths.get("src/test/resources/" + nombreArchivo).toFile()));
            builder.part("paciente", paciente);

            testClient.post().uri("/imagen")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .exchange()
                    .expectStatus().isOk();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Tests ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Subir imagen sana (healthy.png) devuelve 200 OK")
    void subirImagenHealthy_devuelve200() {
        subirImagen("healthy.png");
    }

    @Test
    @DisplayName("Subir imagen no sana (no_healthty.png) devuelve 200 OK")
    void subirImagenNoHealthy_devuelve200() {
        subirImagen("no_healthty.png");
    }

    @Test
    @DisplayName("Subir imagen y listar imágenes del paciente devuelve lista con 1 elemento")
    void subirImagen_listarImagenesPaciente_devuelveListaConUno() {
        subirImagen("healthy.png");

        testClient.get().uri("/imagen/paciente/" + paciente.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Imagen.class)
                .hasSize(1);
    }

    @Test
    @DisplayName("Subir imagen y obtener su información por ID devuelve datos correctos")
    void subirImagen_obtenerInfoPorId_devuelveDatosCorrectos() {
        subirImagen("healthy.png");

        FluxExchangeResult<String> listaResult = testClient.get()
                .uri("/imagen/paciente/" + paciente.getId())
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class);

        String body = listaResult.getResponseBody().blockFirst();
        assertNotNull(body);

        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Imagen> imagenes = mapper.readValue(body, new TypeReference<List<Imagen>>() {});
            assertFalse(imagenes.isEmpty(), "La lista de imágenes no debe estar vacía");

            long imagenId = imagenes.get(0).getId();

            testClient.get().uri("/imagen/info/" + imagenId)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.id").isEqualTo(imagenId);
        } catch (Exception e) {
            fail("Error al parsear la lista de imágenes: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Subir imagen y realizar predicción de IA devuelve resultado no nulo")
    void subirImagen_realizarPrediccion_devuelveResultado() {
        subirImagen("healthy.png");

        FluxExchangeResult<String> listaResult = testClient.get()
                .uri("/imagen/paciente/" + paciente.getId())
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class);

        String body = listaResult.getResponseBody().blockFirst();
        assertNotNull(body);

        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Imagen> imagenes = mapper.readValue(body, new TypeReference<List<Imagen>>() {});
            assertFalse(imagenes.isEmpty(), "Debe haber imágenes subidas");

            long imagenId = imagenes.get(0).getId();

            FluxExchangeResult<String> prediccionResult = testClient.get()
                    .uri("/imagen/predict/" + imagenId)
                    .exchange()
                    .expectStatus().isOk()
                    .returnResult(String.class);

            String prediccion = prediccionResult.getResponseBody().blockFirst();
            assertNotNull(prediccion, "La predicción no debe ser nula");
            assertTrue(prediccion.length() > 0, "La predicción debe contener algún resultado del modelo IA");
        } catch (Exception e) {
            fail("Error al realizar la predicción: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Subir imagen y descargarla devuelve bytes con Content-Type image/png")
    void subirImagen_descargarImagen_devuelvePng() {
        subirImagen("healthy.png");

        FluxExchangeResult<String> listaResult = testClient.get()
                .uri("/imagen/paciente/" + paciente.getId())
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class);

        String body = listaResult.getResponseBody().blockFirst();
        assertNotNull(body);

        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Imagen> imagenes = mapper.readValue(body, new TypeReference<List<Imagen>>() {});
            assertFalse(imagenes.isEmpty());

            long imagenId = imagenes.get(0).getId();

            testClient.get().uri("/imagen/" + imagenId)
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(MediaType.IMAGE_PNG);
        } catch (Exception e) {
            fail("Error al descargar la imagen: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Subir imagen y eliminarla devuelve 204 No Content")
    void subirImagen_eliminarImagen_devuelve204() {
        subirImagen("healthy.png");

        FluxExchangeResult<String> listaResult = testClient.get()
                .uri("/imagen/paciente/" + paciente.getId())
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class);

        String body = listaResult.getResponseBody().blockFirst();
        assertNotNull(body);

        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Imagen> imagenes = mapper.readValue(body, new TypeReference<List<Imagen>>() {});
            assertFalse(imagenes.isEmpty());

            long imagenId = imagenes.get(0).getId();

            testClient.delete().uri("/imagen/" + imagenId)
                    .exchange()
                    .expectStatus().isNoContent();
        } catch (Exception e) {
            fail("Error al eliminar la imagen: " + e.getMessage());
        }
    }
}