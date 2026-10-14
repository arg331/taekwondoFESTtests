package com.taekwondo.examenes.security;

import com.taekwondo.examenes.user.User;
import com.taekwondo.examenes.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Lee "Authorization: Bearer &lt;token&gt;" y, si el token es válido, es de la versión de
 * credenciales actual y el usuario sigue activo, deja en el SecurityContext el userId como
 * principal y su rol como authority. Sin token válido la petición sigue como anónima y
 * SecurityConfig decide si la deja pasar. Con contraseña temporal, solo deja pasar
 * «quién soy» y «cambiar mi contraseña» (403 al resto).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ME_PATH = "/api/auth/me";
    private static final String CHANGE_PASSWORD_PATH = "/api/auth/me/password";
    private static final String MUST_CHANGE_PASSWORD = "Tienes que cambiar la contraseña temporal antes de seguir";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            Optional<User> user = jwtService.parseAccessToken(header.substring(BEARER_PREFIX.length()).trim())
                    .flatMap(token -> userRepository.findById(token.userId())
                            // Una sesión emitida antes del último cambio de contraseña ya no vale
                            .filter(u -> u.getCredentialsVersion() == token.credentialsVersion()))
                    .filter(User::isActive);
            if (user.isPresent() && user.get().isMustChangePassword() && !allowedWithTemporaryPassword(request)) {
                rejectUntilPasswordChanged(response);
                return;
            }
            user.ifPresent(u -> SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(u.getId(), null,
                            List.of(new SimpleGrantedAuthority(u.getRole().name())))));
        }
        chain.doFilter(request, response);
    }

    /** Con contraseña temporal solo se puede consultar quién eres y cambiarla (RF-33). */
    private static boolean allowedWithTemporaryPassword(HttpServletRequest request) {
        String path = request.getRequestURI();
        return ("GET".equals(request.getMethod()) && ME_PATH.equals(path))
                || ("PUT".equals(request.getMethod()) && CHANGE_PASSWORD_PATH.equals(path));
    }

    /** Mismo formato que GlobalExceptionHandler; el filtro va antes de los controladores. */
    private static void rejectUntilPasswordChanged(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now() + "\",\"status\":403,"
                + "\"error\":\"Forbidden\",\"message\":\"" + MUST_CHANGE_PASSWORD + "\"}");
    }
}
