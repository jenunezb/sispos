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
import proyecto.dto.MensajeDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Component
@RequiredArgsConstructor
public class FiltroToken extends OncePerRequestFilter {
    private final JWTUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain
            chain) throws ServletException, IOException {

        // Configuración de cabeceras para CORS
        res.addHeader("Access-Control-Allow-Origin", "*");
        res.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        res.addHeader("Access-Control-Allow-Headers", "Origin, Accept, Content-Type, Authorization");
                res.addHeader("Access-Control-Allow-Credentials", "true");
        if (req.getMethod().equals("OPTIONS")) {
            res.setStatus(HttpServletResponse.SC_OK);
        }else {
            String requestURI = req.getRequestURI();
            String token = getToken(req);
            boolean error = true;
        try{
            if (requestURI.startsWith("/api/vendedor") || requestURI.startsWith("/api/administrador") || requestURI.startsWith("/api/sedes") ) {
                if(token != null) {
                    Jws<Claims> jws = jwtUtils.parseJwt(token);
                    String rol = (String) jws.getBody().get("rol");

                    boolean noAutorizado =
                            (requestURI.startsWith("/api/vendedor") && !rol.equals("vendedor")) ||
                                    (requestURI.startsWith("/api/administrador") && !rol.equals("administrador"));

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

        }catch (MalformedJwtException | SignatureException e){
            crearRespuestaError("El token es incorrecto",
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, res);
        }catch (ExpiredJwtException e ){
            crearRespuestaError("El token está vencido",
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, res);
        }catch (Exception e){
            crearRespuestaError(e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR,

                    res);
        }
        if(!error){
            chain.doFilter(req, res);
        }
        }
    }
    private String getToken(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if(header != null && header.startsWith("Bearer "))
            return header.replace("Bearer ", "");
        return null;
    }
    private void crearRespuestaError(String mensaje, int codigoError, HttpServletResponse
            response) throws IOException {
        MensajeDTO<String> dto = new MensajeDTO<>(true, mensaje);
        response.setContentType("application/json");
        response.setStatus(codigoError);
        response.getWriter().write(new ObjectMapper().writeValueAsString(dto));
        response.getWriter().flush();
        response.getWriter().close();
    }
}