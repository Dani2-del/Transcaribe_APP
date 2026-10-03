package com.transcaribe.transcaribe.service;

import com.transcaribe.transcaribe.Model.AvisoAdministrativo;
import com.transcaribe.transcaribe.Model.Usuario;
import com.transcaribe.transcaribe.Repository.AvisoAdministrativoRepository;
import com.transcaribe.transcaribe.Repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AvisoAdministrativoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AvisoAdministrativoService.class);
    private static final int MAX_ASUNTO = 120;
    private static final int MAX_MENSAJE = 3000;
    private static final Pattern CORREO_VALIDO =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Set<String> ROLES_NOTIFICABLES = Set.of(
            Usuario.ROLE_USER, Usuario.ROLE_CONDUCTOR, Usuario.ROLE_ADMIN);

    private final AvisoAdministrativoRepository avisoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;

    public AvisoAdministrativoService(AvisoAdministrativoRepository avisoRepository,
                                      UsuarioRepository usuarioRepository,
                                      EmailService emailService) {
        this.avisoRepository = avisoRepository;
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
    }

    public List<Usuario> obtenerDestinatariosElegibles() {
        return usuarioRepository.findAll().stream()
                .filter(this::esDestinatarioElegible)
                .sorted((primero, segundo) -> nombreParaMostrar(primero)
                        .compareToIgnoreCase(nombreParaMostrar(segundo)))
                .toList();
    }

    public ResultadoEnvio crearAviso(String asunto, String mensaje, boolean paraTodos,
                                     List<String> rolesSeleccionados, List<String> idsSeleccionados) {
        String asuntoValidado = asunto == null ? "" : asunto.trim();
        String mensajeValidado = mensaje == null ? "" : mensaje.trim();
        if (!StringUtils.hasText(asuntoValidado) || asuntoValidado.length() > MAX_ASUNTO) {
            throw new IllegalArgumentException("El asunto es obligatorio y debe tener máximo 120 caracteres.");
        }
        if (!StringUtils.hasText(mensajeValidado) || mensajeValidado.length() > MAX_MENSAJE) {
            throw new IllegalArgumentException("El mensaje es obligatorio y debe tener máximo 3000 caracteres.");
        }

        Set<String> roles = validarRoles(rolesSeleccionados);
        List<Usuario> destinatarios = paraTodos
                ? obtenerDestinatariosElegibles().stream()
                        .filter(usuario -> roles.contains(usuario.getRole()))
                        .toList()
                : obtenerDestinatariosSeleccionados(idsSeleccionados, roles);
        if (destinatarios.isEmpty()) {
            throw new IllegalArgumentException("No hay cuentas activas y verificadas para los tipos seleccionados.");
        }

        List<String> idsDestinatarios = destinatarios.stream()
                .map(Usuario::getId)
                .distinct()
                .toList();
        AvisoAdministrativo aviso = avisoRepository.save(
                new AvisoAdministrativo(asuntoValidado, mensajeValidado, idsDestinatarios));

        int enviados = 0;
        List<String> correosFallidos = new ArrayList<>();
        for (Usuario destinatario : destinatarios) {
            try {
                emailService.enviarAvisoAdministrativo(
                        destinatario.getCorreo(), nombreParaMostrar(destinatario),
                        asuntoValidado, mensajeValidado);
                enviados++;
            } catch (IllegalStateException exception) {
                LOGGER.warn("No se pudo enviar el aviso administrativo {} a {}",
                        aviso.getId(), destinatario.getId(), exception);
                correosFallidos.add(destinatario.getCorreo());
            }
        }

        return new ResultadoEnvio(aviso.getId(), destinatarios.size(), enviados, correosFallidos);
    }

    public List<AvisoAdministrativo> obtenerPendientes(String usuarioId) {
        if (!StringUtils.hasText(usuarioId)) {
            return List.of();
        }
        return avisoRepository.findPendientesParaUsuario(usuarioId);
    }

    public List<AvisoAdministrativo> obtenerAvisosRecientes() {
        return avisoRepository.findTop2ByOrderByFechaCreacionDesc();
    }

    public List<AvisoAdministrativo> obtenerHistorialAvisos() {
        return avisoRepository.findAllByOrderByFechaCreacionDesc();
    }

    public void descartarParaUsuario(String avisoId, String usuarioId) {
        AvisoAdministrativo aviso = avisoRepository.findById(avisoId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el aviso solicitado."));
        if (!aviso.getDestinatarios().contains(usuarioId)) {
            throw new SecurityException("El usuario no es destinatario de este aviso.");
        }
        if (aviso.getDescartadaPor().add(usuarioId)) {
            avisoRepository.save(aviso);
        }
    }

    private List<Usuario> obtenerDestinatariosSeleccionados(List<String> idsSeleccionados,
                                                              Set<String> rolesSeleccionados) {
        Set<String> ids = idsSeleccionados == null
                ? Set.of()
                : idsSeleccionados.stream()
                        .filter(StringUtils::hasText)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("Selecciona al menos un destinatario.");
        }

        List<Usuario> encontrados = usuarioRepository.findAllById(ids);
        if (encontrados.size() != ids.size()
                || encontrados.stream().anyMatch(u -> !esDestinatarioElegible(u)
                || !rolesSeleccionados.contains(u.getRole()))) {
            throw new IllegalArgumentException("La selección contiene cuentas inexistentes o no elegibles.");
        }
        return encontrados;
    }

    private Set<String> validarRoles(List<String> rolesSeleccionados) {
        Set<String> roles = rolesSeleccionados == null
                ? Set.of()
                : rolesSeleccionados.stream()
                        .filter(StringUtils::hasText)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        if (roles.isEmpty() || !ROLES_NOTIFICABLES.containsAll(roles)) {
            throw new IllegalArgumentException("Selecciona al menos un tipo de cuenta válido.");
        }
        return roles;
    }

    private boolean esDestinatarioElegible(Usuario usuario) {
        return usuario != null
                && usuario.isActivo()
                && usuario.isVerificado()
                && StringUtils.hasText(usuario.getId())
                && StringUtils.hasText(usuario.getCorreo())
                && CORREO_VALIDO.matcher(usuario.getCorreo().trim()).matches();
    }

    private String nombreParaMostrar(Usuario usuario) {
        return StringUtils.hasText(usuario.getNombre()) ? usuario.getNombre().trim() : usuario.getCorreo();
    }

    public record ResultadoEnvio(String avisoId, int totalDestinatarios, int correosEnviados,
                                 List<String> correosFallidos) {
        public ResultadoEnvio {
            correosFallidos = List.copyOf(correosFallidos);
        }
    }
}
