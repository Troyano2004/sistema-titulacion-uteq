
package com.erwin.backend.service;

import com.erwin.backend.config.DbSessionFilter;
import com.erwin.backend.dtos.LoginRequest;
import com.erwin.backend.dtos.LoginResponse;
import com.erwin.backend.dtos.UsuarioAdminDto;
import com.erwin.backend.entities.Usuario;
import com.erwin.backend.repository.UsuarioRepository;
import com.erwin.backend.repository.UsuarioSpRepository;
import com.erwin.backend.security.CryptoUtil;
import com.erwin.backend.security.JwtService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuthService {

    private final UsuarioRepository    usuarioRepo;
    private final UsuarioSpRepository  usuarioSpRepo;
    private final PasswordEncoder      passwordEncoder;
    private final JwtService           jwtService;
    private final com.erwin.backend.audit.service.AuditService auditService;

    public AuthService(UsuarioRepository usuarioRepo,
                       UsuarioSpRepository usuarioSpRepo,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       com.erwin.backend.audit.service.AuditService auditService) {
        this.usuarioRepo    = usuarioRepo;
        this.usuarioSpRepo  = usuarioSpRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService     = jwtService;
        this.auditService   = auditService;
    }

    public LoginResponse login(LoginRequest req, HttpSession session) {

        if (req == null || req.getUsuarioLogin() == null || req.getPassword() == null) {
            throw new RuntimeException("Faltan datos de login");
        }

        String usernameLogin = req.getUsuarioLogin().trim();
        String passwordApp   = req.getPassword().trim();

        Usuario usuario = usuarioRepo
                .findByUsername(usernameLogin)
                .orElseThrow(() -> new RuntimeException("Usuario no existe"));

        // 1) Validar password del aplicativo (solo BCrypt, ver validarPassword)
        validarPassword(passwordApp, usuario.getPasswordHash());

        // 2) Credenciales BD: se guardan SOLO en la sesión HTTP como fallback.
        //    El JWT nunca las transporta — DbSessionFilter las resuelve en cada
        //    petición a partir del usuario autenticado (ver PRIORIDAD 1 allí).
        String dbUser = (usuario.getUsernameDb() != null && !usuario.getUsernameDb().trim().isEmpty())
                ? usuario.getUsernameDb().trim()
                : (usuario.getUsername() != null ? usuario.getUsername().trim() : null);

        String dbPassEncrypted = usuario.getPasswordDbEncrypted();

        if (dbUser != null && dbPassEncrypted != null && !dbPassEncrypted.trim().isEmpty()) {
            String dbPass = CryptoUtil.decrypt(dbPassEncrypted.trim());
            session.setAttribute(DbSessionFilter.SES_DB_USER, dbUser);
            session.setAttribute(DbSessionFilter.SES_DB_PASS, dbPass);
        } else {
            session.removeAttribute(DbSessionFilter.SES_DB_USER);
            session.removeAttribute(DbSessionFilter.SES_DB_PASS);
        }

        // ✅ 3) Obtener TODOS los roles del usuario desde la vista vw_usuario_admin_v3
        List<String> todosLosRoles = new ArrayList<>();
        String rolPrincipal = "";

        try {
            UsuarioAdminDto dto = usuarioSpRepo.obtenerPorId(usuario.getIdUsuario());
            if (dto != null && dto.getIdsRolApp() != null && dto.getIdsRolApp().length > 0) {
                // rolesApp es algo como "ROLE_COORDINADOR, ROLE_DOCENTE"
                String rolesAppStr = dto.getRolesApp();
                if (rolesAppStr != null && !rolesAppStr.isBlank()) {
                    for (String r : rolesAppStr.split(",")) {
                        String limpio = normalizarRolApp(r.trim());
                        if (!limpio.isEmpty()) todosLosRoles.add(limpio);
                    }
                }
                // Rol principal = el primero de la lista o rol_app_principal
                rolPrincipal = todosLosRoles.isEmpty() ? "" : todosLosRoles.get(0);
            }
        } catch (Exception ignored) {
            // fallback: usar rol_asignado del usuario
        }

        // Fallback si no se encontraron roles
        if (todosLosRoles.isEmpty()) {
            rolPrincipal = convertirRol(usuario.getRolAsignado());
            if (!rolPrincipal.isEmpty()) todosLosRoles.add(rolPrincipal);
        }

        // 4) Generar el JWT ya con los roles como claim (usados por JwtFilter
        //    para construir las GrantedAuthority que evalúa @PreAuthorize)
        String jwtToken = jwtService.generateToken(usernameLogin, todosLosRoles);

        try {
            auditService.registrar(com.erwin.backend.audit.dto.AuditEventDto.builder()
                .entidad("Login").accion("LOGIN")
                .idUsuario(usuario.getIdUsuario())
                .username(usernameLogin)
                .correoUsuario(usuario.getCorreoInstitucional())
                .build());
        } catch (Exception ignored) {}

        return new LoginResponse(
                usuario.getIdUsuario(),
                rolPrincipal,
                todosLosRoles,   // ✅ NUEVO: lista completa de roles
                usuario.getNombres(),
                usuario.getApellidos(),
                jwtToken
        );
    }

    // -------------------------------------------------------
    // Convierte "ROLE_COORDINADOR" o "coordinador" → "ROLE_COORDINADOR"
    // -------------------------------------------------------
    private String normalizarRolApp(String nombre) {
        if (nombre == null || nombre.isBlank()) return "";
        String upper = nombre.trim().toUpperCase();
        if (upper.startsWith("ROLE_")) return upper;
        return "ROLE_" + upper;
    }

    private void validarPassword(String rawPassword, String storedPassword) {
        if (rawPassword == null)
            throw new RuntimeException("Contraseña incorrecta");
        if (storedPassword == null || storedPassword.trim().isEmpty())
            throw new RuntimeException("El usuario no tiene passwordHash configurado");

        String stored = storedPassword.trim();
        boolean esBcrypt = stored.matches("^\\$2[aby]\\$\\d\\d\\$.+");

        // Sin fallback a texto plano: un hash que no sea BCrypt indica una cuenta
        // creada/migrada antes de exigir BCrypt y debe re-hashearse explícitamente,
        // nunca compararse en claro.
        if (!esBcrypt) {
            throw new RuntimeException(
                    "La contraseña de este usuario está en un formato antiguo (no BCrypt) y debe " +
                    "migrarse antes de poder iniciar sesión. Contacte al administrador."
            );
        }

        if (!passwordEncoder.matches(rawPassword, stored)) {
            throw new RuntimeException("Contraseña incorrecta");
        }
    }

    private String convertirRol(String rolAsignado) {
        if (rolAsignado == null || rolAsignado.isBlank()) return "";
        // Cualquier rol (incluidos los nuevos DECANO / SECRETARIO_ABOGADO /
        // DIRECCION_ACADEMICA / GESTOR_USUARIOS / ROL_REPORT) debe llegar como
        // "ROLE_XXX": es el formato que espera Spring Security en
        // hasRole(...)/hasAnyRole(...), y JwtFilter no vuelve a anteponerlo.
        return normalizarRolApp(rolAsignado);
    }
}