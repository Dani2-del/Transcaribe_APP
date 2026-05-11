package com.transcaribe.transcaribe.Config;

import com.transcaribe.transcaribe.Security.JwtAuthenticationFilter;
import com.transcaribe.transcaribe.Security.OAuth2AuthenticationSuccessHandler;
import com.transcaribe.transcaribe.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          PasswordEncoder passwordEncoder,
                          JwtAuthenticationFilter jwtAuthFilter,
                          OAuth2AuthenticationSuccessHandler oauth2SuccessHandler) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtAuthFilter = jwtAuthFilter;
        this.oauth2SuccessHandler = oauth2SuccessHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) 
            
            .authorizeHttpRequests(auth -> auth
                // --- RUTAS PÚBLICAS TOTALMENTE CORREGIDAS ---
                .requestMatchers(
                    "/",                     // Raíz
                    "/login",                // Página de login
                    "/registro",             // Registro de usuario
                    "/verificar-otp/**",     // Verificación de cuenta nueva
                    "/olvido-password",      // Formulario para pedir el código
                    "/verificar-codigo-recuperar-password/**", // Vista de ingreso de código
                    "/validar-codigo-restablecimiento",        // Acción POST de validación
                    "/restablecer-password",                   // Vista de nueva contraseña
                    "/confirmar-restablecimiento",             // Acción POST de guardado final
                    "/style.css",            // Estilos
                    "/error/**",             // Páginas de error
                    "/images/**",            // Imágenes estáticas
                    "/js/**"                 // Scripts
                ).permitAll()

                // Rutas protegidas por rol
                .requestMatchers("/choose-view").hasAnyRole("ADMIN", "MODERADOR")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/mod/**").hasRole("MODERADOR")

                // Cualquier otra ruta requiere autenticación
                .anyRequest().authenticated()
            )

            // --- CONFIGURACIÓN DE OAUTH2 (GOOGLE) CORREGIDA ---
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login") // CAMBIADO de "/" a "/login" para evitar el bucle
                .successHandler(oauth2SuccessHandler)
            )

            // --- CONFIGURACIÓN DE LOGIN TRADICIONAL ---
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("correo")
                .passwordParameter("password")
                .successHandler((request, response, authentication) -> {
                    var roles = authentication.getAuthorities();
                    boolean isStaff = roles.stream().anyMatch(r -> 
                        r.getAuthority().equals("ROLE_ADMIN") || 
                        r.getAuthority().equals("ROLE_MODERADOR"));

                    if (isStaff) {
                        response.sendRedirect("/choose-view");
                    } else {
                        response.sendRedirect("/menu");
                    }
                })
                .failureHandler((request, response, exception) -> {
                    // Si el usuario no ha verificado su cuenta (isVerificado = false)
                    if (exception instanceof DisabledException || 
                        (exception.getCause() != null && exception.getCause() instanceof DisabledException)) {
                        String correo = request.getParameter("correo");
                        response.sendRedirect("/verificar-otp?correo=" + correo);
                    } else {
                        // Enviamos el error directamente al login, no a la raíz
                        response.sendRedirect("/login?error=true");
                    }
                })
                .permitAll()
            )

            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login")
                .deleteCookies("JWT_TOKEN")
                .invalidateHttpSession(true)
                .permitAll()
            )

            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1) 
                .expiredUrl("/login?expired=true")
            )

            .exceptionHandling(exception -> exception
                .accessDeniedPage("/error/403")
            );

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(userDetailsService);
        auth.setPasswordEncoder(passwordEncoder);
        return auth;
    }
}