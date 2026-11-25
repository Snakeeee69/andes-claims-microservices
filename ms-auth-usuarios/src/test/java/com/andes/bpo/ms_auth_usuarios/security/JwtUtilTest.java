package com.andes.bpo.ms_auth_usuarios.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String SECRET =
            "ClaveDePruebaParaJWTAndesBPO2026SeguraConMasDe32Bytes";

    @BeforeEach
    void preparar() {
        jwtUtil = new JwtUtil();

        ReflectionTestUtils.setField(
                jwtUtil, "secret", SECRET
        );

        ReflectionTestUtils.setField(
                jwtUtil, "expirationMs", 60000L
        );
    }

    private Claims leerToken(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(
                        SECRET.getBytes(StandardCharsets.UTF_8)
                ))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Test
    void generarTokenConDatosCorrectos() {
        String token = jwtUtil.generarToken(
                "12345678K", "ADMINISTRADOR", 10
        );

        Claims datos = leerToken(token);

        assertEquals("12345678K", datos.getSubject());
        assertEquals("ADMINISTRADOR", datos.get("rol"));
        assertEquals(10, datos.get("idUsuario", Integer.class));
    }

    @Test
    void tokenContieneFechaDeExpiracion() {
        String token = jwtUtil.generarToken(
                "12345678K", "ASEGURADO_NATURAL", 10
        );

        Claims datos = leerToken(token);

        assertNotNull(datos.getIssuedAt());
        assertNotNull(datos.getExpiration());

        assertTrue(
                datos.getExpiration().after(datos.getIssuedAt())
        );
    }

    @Test
    void tokenNoPuedeValidarseConOtraClave() {
        String token = jwtUtil.generarToken(
                "12345678K", "LIQUIDADOR", 10
        );

        String otraClave =
                "OtraClaveTotalmenteDiferenteParaPruebasJWTAndesBPO2026";

        assertThrows(
                io.jsonwebtoken.security.SignatureException.class,
                () -> Jwts.parser()
                        .verifyWith(Keys.hmacShaKeyFor(
                                otraClave.getBytes(
                                        StandardCharsets.UTF_8
                                )
                        ))
                        .build()
                        .parseSignedClaims(token)
        );
    }
}
