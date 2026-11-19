package com.erwin.backend.config;

import com.erwin.backend.entities.Usuario;
import com.erwin.backend.repository.UsuarioRepository;
import com.erwin.backend.security.CryptoUtil;
import com.erwin.backend.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class DbSessionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DbSessionFilter.class);

    public static final String SES_DB_USER = "SES_DB_USER";
    public static final String SES_DB_PASS = "SES_DB_PASS";

    private static final String DEFAULT_USER = "auth_reader";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public DbSessionFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    // ── Excluir rutas de recovery: no necesitan JWT ni sesión ─────────────────
    // Esto es importante cuando la BD está caída — no queremos que este filtro
    // intente hacer nada que dependa de un contexto de BD activo.
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/recovery/") || path.equals("/recovery.html");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest  request,
            HttpServletResponse response,
            FilterChain         filterChain
    ) throws ServletException, IOException {

        String user = null;
        String pass = null;

        // PRIORIDAD 1: resolver las credenciales de BD en el servidor a partir del
        // usuario autenticado en el JWT (Authorization: Bearer ...). El JWT en sí
        // NUNCA transporta la contraseña de BD — solo identifica al usuario; la
        // contraseña se descifra aquí, en cada petición, desde `usuario.password_db_encrypted`.
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtService.isTokenValid(token)) {
                String username = jwtService.extractUsername(token);
                if (username != null && !username.isBlank()) {
                    Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);
                    if (usuarioOpt.isPresent()) {
                        Usuario u = usuarioOpt.get();
                        String dbUser = (u.getUsernameDb() != null && !u.getUsernameDb().isBlank())
                                ? u.getUsernameDb().trim()
                                : u.getUsername();
                        String dbPassEncrypted = u.getPasswordDbEncrypted();
                        if (dbUser != null && dbPassEncrypted != null && !dbPassEncrypted.isBlank()) {
                            user = dbUser;
                            pass = CryptoUtil.decrypt(dbPassEncrypted.trim());
                        }
                    }
                }
            }
        }

        // PRIORIDAD 2 (fallback): leer de sesión HTTP (para clientes con cookie,
        // p.ej. si el header Authorization no llegó en esta petición)
        if (user == null || user.isBlank()) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                String sesUser = (String) session.getAttribute(SES_DB_USER);
                String sesPass = (String) session.getAttribute(SES_DB_PASS);
                if (sesUser != null && !sesUser.isBlank()
                        && sesPass != null && !sesPass.isBlank()) {
                    user = sesUser;
                    pass = sesPass;
                }
            }
        }

        boolean switched = user != null && !user.isBlank();

        if (switched) {
            log.info("🟢 DB SWITCH -> usuario BD = {} | path={}",
                    user, request.getRequestURI());
        } else {
            log.debug("🟡 DB DEFAULT -> usuario BD = {} | path={}",
                    DEFAULT_USER, request.getRequestURI());
        }

        try {
            if (switched) {
                DbContextHolder.set(user, pass);
            }
            filterChain.doFilter(request, response);
        } finally {
            DbContextHolder.clear();
        }
    }
}
