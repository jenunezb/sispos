package proyecto.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import proyecto.dto.MensajeDTO;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class FiltroToken extends OncePerRequestFilter {

    private final JWTUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String origin = req.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            res.setHeader("Access-Control-Allow-Origin", origin);
            res.setHeader("Vary", "Origin");
        }
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        res.setHeader("Access-Control-Allow-Headers", "Origin, Accept, Content-Type, Authorization");
        res.setHeader("Access-Control-Allow-Credentials", "true");

        if (req.getMethod().equals("OPTIONS")) {
            res.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String requestURI = req.getRequestURI();
        String token = getToken(req);
        boolean error = true;

        try {
            if (requestURI.startsWith("/api/vendedor") || requestURI.startsWith("/api/administrador")
                    || requestURI.startsWith("/api/superadmin")
                    || requestURI.startsWith("/api/produccion")
                    || requestURI.startsWith("/api/sedes") || requestURI.startsWith("/api/inventario")) {
                if (token != null) {
                    Jws<Claims> jws = jwtUtils.parseJwt(token);
                    String rol = (String) jws.getBody().get("rol");
                    Boolean esSuperAdmin = jws.getBody().get("esSuperAdmin", Boolean.class);

                    boolean noAutorizado =
                            (requestURI.startsWith("/api/vendedor") && !rol.equals("vendedor")) ||
                                    (requestURI.startsWith("/api/administrador") && !rol.equals("administrador")) ||
                                    (requestURI.startsWith("/api/superadmin") && (!rol.equals("administrador") || !Boolean.TRUE.equals(esSuperAdmin))) ||
                                    (requestURI.startsWith("/api/produccion") && !rol.equals("produccion"));

                    if (noAutorizado) {
                        crearRespuestaError("No tiene los permisos para acceder a este recurso",
                                HttpServletResponse.SC_FORBIDDEN, res);
                    } else {
                        error = false;
                    }

                } else {
                    crearRespuestaError("No hay un Token",
                            HttpServletResponse.SC_FORBIDDEN, res);
                }
            } else {
                error = false;
            }

        } catch (MalformedJwtException | SignatureException e) {
            crearRespuestaError("El token es incorrecto",
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, res);
        } catch (ExpiredJwtException e) {
            crearRespuestaError("El token esta vencido",
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, res);
        } catch (Exception e) {
            crearRespuestaError(e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, res);
        }

        if (!error) {
            chain.doFilter(req, res);
        }
    }

    private String getToken(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.replace("Bearer ", "");
        }
        return null;
    }

    private void crearRespuestaError(String mensaje, int codigoError, HttpServletResponse response) throws IOException {
        MensajeDTO<String> dto = new MensajeDTO<>(true, mensaje);
        response.setContentType("application/json");
        response.setStatus(codigoError);
        response.getWriter().write(new ObjectMapper().writeValueAsString(dto));
        response.getWriter().flush();
        response.getWriter().close();
    }
}
