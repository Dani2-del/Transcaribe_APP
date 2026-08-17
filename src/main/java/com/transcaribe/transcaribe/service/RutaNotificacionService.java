package com.transcaribe.transcaribe.service;

import com.transcaribe.transcaribe.Model.Usuario;
import com.transcaribe.transcaribe.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RutaNotificacionService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmailService emailService;

    /**
     * Busca a todos los usuarios que tienen la ruta indicada en sus favoritas
     * y les envía un correo notificando que el bus salió.
     * Retorna la cantidad de usuarios notificados.
     */
    public int notificarInicioRuta(String ruta, String placaBus) {
        List<Usuario> interesados = usuarioRepository.findByRutasFavoritasContaining(ruta);

        for (Usuario usuario : interesados) {
            emailService.enviarNotificacionRutaIniciada(
                    usuario.getCorreo(),
                    usuario.getNombre() != null ? usuario.getNombre() : usuario.getCorreo(),
                    ruta,
                    placaBus
            );
        }

        return interesados.size();
    }
}
