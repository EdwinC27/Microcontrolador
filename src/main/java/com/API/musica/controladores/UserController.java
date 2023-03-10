package com.API.musica.controladores;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.API.musica.entidades.User;
import com.API.musica.repositorios.RepositorioUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import static com.API.musica.configuracion.PasswordUtils.verifyPassword;

@RestController
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(Consulta.class);
    @Autowired
    private RepositorioUser usuarioRepository;

    @Value("${keySecret}")
    public String key;

    @Value("${setIdEncriptacion}")
    private String setId;

    @PostMapping("user")
    @Operation(summary = "Obtener token necesario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token generador correctamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros incorrectos o faltantes"),
            @ApiResponse(responseCode = "500", description = "Error al general token")
    })
    public User login(@RequestBody User consultaUser) {
        if (validarUsuario(consultaUser)) {
            String token = getJWTToken(consultaUser.getUserName());
            consultaUser.setToken(token);

            guardarMitoken(consultaUser.getUserName(), token);

            return consultaUser;

        } else {
            consultaUser.setToken("Error usuario o contraseña incorrecta");
            return consultaUser;
        }
    }

    public boolean validarUsuario(User userJson) {
        try {
            User usuarioBaseDatos = usuarioRepository.findByuserName(userJson.getUserName());

            LOGGER.debug("userJson: " + userJson.getUserName());
            LOGGER.debug("usuarioBaseDatos: " + usuarioBaseDatos.getUserName());
            LOGGER.debug("passwordJson: " + userJson.getPassword());
            LOGGER.debug("PasswordBD: " + usuarioBaseDatos.getPassword());

            return verifyPassword(userJson.getPassword(), usuarioBaseDatos.getPassword());
        } catch (Exception e) {
            return false;
        }
    }

    private String getJWTToken(String username) {
        long MILISEGUNDO_HORA = 3_600_000;

        List<GrantedAuthority> grantedAuthorities = AuthorityUtils
                .commaSeparatedStringToAuthorityList("ROLE_USER");

        String token = Jwts
                .builder()
                .setId(setId)
                .setSubject(username)
                .claim("authorities",
                        grantedAuthorities.stream()
                                .map(GrantedAuthority::getAuthority)
                                .collect(Collectors.toList()))
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + MILISEGUNDO_HORA))
                .signWith(SignatureAlgorithm.HS512,
                        key.getBytes()).compact();

        return token;
    }

    public void guardarMitoken(String userName, String token) {
        User userGuardar = usuarioRepository.findByuserName(userName);
        userGuardar.setToken(token);
        usuarioRepository.save(userGuardar);
    }
}