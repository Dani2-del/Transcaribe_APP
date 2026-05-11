package com.transcaribe.transcaribe.Security;

import java.io.IOException;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.transcaribe.transcaribe.Model.Usuario;
import com.transcaribe.transcaribe.Repository.UsuarioRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final JwtCookieService jwtCookieService;
    private final UsuarioRepository userRepository;

    public OAuth2AuthenticationSuccessHandler(
            JwtService jwtService,
            JwtCookieService jwtCookieService,
            UsuarioRepository userRepository) {
        this.jwtService = jwtService;
        this.jwtCookieService = jwtCookieService;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        if (email == null) throw new RuntimeException("Google no retornó email");

        // Buscar o crear usuario en MongoDB
        Optional<Usuario> existingUser = userRepository.findByCorreo(email);
        Usuario usuario;

        if (existingUser.isPresent()) {
            usuario = existingUser.get();
        } else {
            // Usamos tu modelo de Transcaribe
            usuario = new Usuario(email, "", name != null ? name : email);
            usuario.setVerificado(true); // Si viene de Google, ya está verificado
            userRepository.save(usuario);
        }

        // Crear UserDetails compatible
        var userDetails = User.withUsername(usuario.getCorreo())
                .password("")
                .authorities(usuario.getRole()) // Ya viene con "ROLE_USER" desde el modelo
                .build();

        // Generar JWT y guardarlo en Cookie
        String token = jwtService.generateToken(userDetails);
        jwtCookieService.addJwtCookie(response, token);

        // Actualizar contexto de seguridad
        var authToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // Redirigir a tu menú principal
        response.sendRedirect("/menu");
    }
}