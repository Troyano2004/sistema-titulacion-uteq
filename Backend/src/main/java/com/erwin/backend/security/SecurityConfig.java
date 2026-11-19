package com.erwin.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                // JwtFilter debe correr ANTES que AnonymousAuthenticationFilter:
                // ese filtro deja el SecurityContext con un Authentication no
                // nulo (anónimo) apenas arranca la cadena, y JwtFilter solo
                // actúa cuando getAuthentication() == null — si corriera
                // después (como pasaba antes, al ser un @Component genérico
                // registrado fuera de esta cadena), nunca llegaría a
                // establecer la autenticación real del JWT.
                .addFilterBefore(new JwtFilter(jwtService), AnonymousAuthenticationFilter.class)

                .authorizeHttpRequests(auth -> auth
                        // ── Autenticación: público ────────────────────────────────
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/logout").permitAll()

                        // ── Forward interno de Spring Boot para rutas no
                        //    mapeadas o métodos no soportados: sin esto, ese
                        //    segundo dispatch (dispatcher type ERROR) llega
                        //    con el SecurityContext vacío — JwtFilter no
                        //    reprocesa dispatches ERROR (OncePerRequestFilter)
                        //    — y anyRequest().authenticated() lo rechaza con
                        //    401 + WWW-Authenticate: Basic en vez de dejar que
                        //    BasicErrorController devuelva el 404/405 real ──
                        .requestMatchers("/error").permitAll()

                        // ── Recovery: accesible aunque la BD esté caída ──────────
                        // La seguridad la maneja RecoveryService con contraseña maestra
                        .requestMatchers("/api/recovery/**").permitAll()
                        .requestMatchers("/recovery.html").permitAll()

                        // ── Autorregistro de cuentas: público hasta la aprobación ─
                        // (la aprobación/rechazo/listado sí requiere ADMIN, ver
                        // SolicitudRegistroController)
                        .requestMatchers("/api/solicitud-registro/correo").permitAll()
                        .requestMatchers("/api/solicitud-registro/verificar").permitAll()
                        .requestMatchers("/api/solicitud-registro/datos").permitAll()

                        // ── Catálogo mínimo requerido por el formulario público
                        //    de registro de estudiantes (selección de carrera) ────
                        .requestMatchers(HttpMethod.GET, "/api/catalogos/carreras").permitAll()

                        // ── Callback de OAuth de Google Drive: lo invoca el
                        //    navegador tras la redirección de Google, sin nuestro
                        //    JWT. Su seguridad la da el parámetro "state" validado
                        //    en GoogleDriveOAuthService, no la sesión del app ────
                        .requestMatchers("/api/backup/oauth/callback").permitAll()

                        // ── Todo lo demás requiere sesión autenticada; el rol
                        //    específico se valida con @PreAuthorize en cada
                        //    controlador ──────────────────────────────────────────
                        .anyRequest().authenticated()
                )

                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((req, res, auth) -> {
                            System.out.println("🔴 LOGOUT realizado -> sesión invalidada");
                            res.setStatus(200);
                        })
                )

                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}