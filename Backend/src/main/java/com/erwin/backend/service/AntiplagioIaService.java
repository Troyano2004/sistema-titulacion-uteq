package com.erwin.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Proxy server-side hacia WasItAIGenerated. La API key vive SOLO aquí
 * (variable de entorno WASITAI_API_KEY), nunca en el frontend — antes estaba
 * hardcodeada en antiplagio-dt2.ts y visible en el bundle compilado.
 */
@Service
public class AntiplagioIaService {

    @Value("${wasitai.api.url}")
    private String apiUrl;

    @Value("${wasitai.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public AntiplagioIaService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<String> verificarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El texto a analizar es obligatorio");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El análisis con IA no está configurado en este servidor (falta WASITAI_API_KEY)");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("content", texto), headers);

        try {
            return restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Error al consultar el servicio de detección de IA: " + ex.getMessage());
        }
    }
}
