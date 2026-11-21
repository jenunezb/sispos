package proyecto.utils;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Component
public class JWTUtils {
    public static final String PREFIJO_MESERO = "mesero.";

    @Value("${jwt.secret}")
    private String claveSecreta;
    public String generarToken(String email, Map<String, Object> claims){
        Instant now = Instant.now();
        return Jwts.builder()
                .addClaims(claims)
                .setSubject(email)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(5L, ChronoUnit.DAYS)))
                .signWith( getKey() )
                .compact();
    }
    public Jws<Claims> parseJwt(String jwtString) throws ExpiredJwtException,
            UnsupportedJwtException, MalformedJwtException, IllegalArgumentException {
        return Jwts.parserBuilder()
                .setSigningKey( getKey() )
                .build()
                .parseClaimsJws(jwtString);
    }

    public String generarTokenMesero(Long meseroId, String claveSesion) {
        Instant ahora = Instant.now();
        return PREFIJO_MESERO + Jwts.builder()
                .setSubject(meseroId.toString())
                .claim("claveSesion", claveSesion)
                .setIssuedAt(Date.from(ahora))
                .setExpiration(Date.from(ahora.plus(12, ChronoUnit.HOURS)))
                .signWith(claveMesero(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parsearTokenMesero(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(claveMesero())
                .build()
                .parseClaimsJws(token.substring(PREFIJO_MESERO.length()))
                .getBody();
    }

    private Key claveMesero() {
        try {
            String algoritmo = SignatureAlgorithm.HS256.getJcaName();
            Mac mac = Mac.getInstance(algoritmo);
            mac.init(getKey());
            byte[] derivada = mac.doFinal("sispos/mesero/v1".getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(derivada, algoritmo);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo configurar la firma de meseros", ex);
        }
    }

    private Key getKey(){
        return new SecretKeySpec(Base64.getDecoder().decode(claveSecreta),
                SignatureAlgorithm.HS256.getJcaName());
    }
}
