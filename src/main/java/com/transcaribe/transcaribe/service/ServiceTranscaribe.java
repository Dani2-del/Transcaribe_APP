package com.transcaribe.transcaribe.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Random;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.transcaribe.transcaribe.Model.Tarjeta;
import com.transcaribe.transcaribe.Model.Usuario;
import com.transcaribe.transcaribe.Repository.UsuarioRepository;

@Service
public class ServiceTranscaribe {

    private final UsuarioRepository repositorioUsuarios;
    private final TransaccionService servicioTransacciones;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public ServiceTranscaribe(UsuarioRepository repositorioUsuarios,
                               TransaccionService servicioTransacciones,
                               PasswordEncoder passwordEncoder,
                               EmailService emailService) {
        this.repositorioUsuarios = repositorioUsuarios;
        this.servicioTransacciones = servicioTransacciones;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public boolean registrar(String nombre, String correo, String contrasena, String numeroTarjeta) {
        if (repositorioUsuarios.findByCorreo(correo).isPresent()) {
            return false;
        }
        String numeroLimpio = numeroTarjeta.replaceAll("\\D", "");
        if (numeroLimpio.length() != 10) {
            return false;
        }
        
        String codigoOtp = String.format("%06d", new Random().nextInt(1000000));

        Usuario nuevoUsuario = new Usuario(correo, passwordEncoder.encode(contrasena), nombre);
        
        nuevoUsuario.agregarTarjeta(numeroLimpio, BigDecimal.ZERO);
        nuevoUsuario.setCodigoVerificacion(codigoOtp);
        nuevoUsuario.setVerificado(false); 

        repositorioUsuarios.save(nuevoUsuario);

        try {
            emailService.enviarCodigoVerificacion(correo, nombre, codigoOtp);
        } catch (Exception e) {
            System.err.println("Error al enviar código OTP: " + e.getMessage());
        }
        return true;
    }

    public boolean verificarCodigo(String correo, String codigoIntroducido) {
        Optional<Usuario> opt = repositorioUsuarios.findByCorreo(correo);

        if (opt.isPresent()) {
            Usuario u = opt.get();
            if (u.getCodigoVerificacion() != null && u.getCodigoVerificacion().equals(codigoIntroducido)) {
                u.setVerificado(true);
                u.setCodigoVerificacion(null); 
                repositorioUsuarios.save(u);

                String numTarjeta = u.getTarjetas().isEmpty() ? "N/A" : u.getTarjetas().get(0).getNumeroTarjeta();
                try {
                    emailService.enviarCorreoBienvenida(u.getCorreo(), u.getNombre(), numTarjeta);
                } catch (Exception e) {
                    System.err.println("Error al enviar correo bienvenida: " + e.getMessage());
                }
                return true;
            }
        }
        return false;
    }

    public void recargar(Usuario usuario, double monto) {
        if (usuario != null && usuario.isActivo() && monto > 0 && !usuario.getTarjetas().isEmpty()) {
            Tarjeta t = usuario.getTarjetas().get(0);
            t.setSaldo(t.getSaldo().add(BigDecimal.valueOf(monto)));
            
            repositorioUsuarios.save(usuario);
            servicioTransacciones.registrarTransaccion(usuario, "Recarga Directa", monto);
            emailService.enviarNotificacionRecarga(usuario.getCorreo(), usuario.getNombre(), monto, t.getSaldo().toString());
        }
    }

    public void gastar(Usuario usuario, double monto) {
        if (usuario != null && usuario.isActivo() && !usuario.getTarjetas().isEmpty()) {
            Tarjeta t = usuario.getTarjetas().get(0);
            BigDecimal montoBD = BigDecimal.valueOf(monto);
            
            if (t.getSaldo().compareTo(montoBD) >= 0) {
                t.setSaldo(t.getSaldo().subtract(montoBD));
                repositorioUsuarios.save(usuario);
                servicioTransacciones.registrarTransaccion(usuario, "Gasto Directo", monto);
            emailService.enviarNotificacionGasto(usuario.getCorreo(), usuario.getNombre(), monto, t.getSaldo().toString());
            }
        }
    }

    public boolean recargarEnTarjeta(Usuario usuario, String numeroTarjeta, double monto) {
        if (usuario == null || !usuario.isActivo() || monto <= 0) return false;

        return usuario.getTarjetas().stream()
                .filter(t -> t.getNumeroTarjeta().equals(numeroTarjeta))
                .findFirst()
                .map(t -> {
                    t.setSaldo(t.getSaldo().add(BigDecimal.valueOf(monto)));
                    repositorioUsuarios.save(usuario);
                    servicioTransacciones.registrarTransaccion(usuario, "Recarga Tarjeta: " + ocultarNumeroTarjeta(numeroTarjeta), monto);
                    return true;
                }).orElse(false);
    }

    public Tarjeta obtenerTarjetaDeUsuario(Usuario usuario) {
        if (usuario == null || usuario.getTarjetas().isEmpty()) return null;
        return usuario.getTarjetas().get(0);
    }

    public boolean cobrarPasajePorAdmin(String idUsuario, double monto) {
        Optional<Usuario> opt = repositorioUsuarios.findById(idUsuario);
        if (opt.isEmpty()) return false;

        Usuario u = opt.get();
        if (!u.isActivo() || u.getTarjetas().isEmpty()) return false;

        Tarjeta t = u.getTarjetas().get(0);
        BigDecimal montoBD = BigDecimal.valueOf(monto);

        if (t.getSaldo().compareTo(montoBD) >= 0) {
            t.setSaldo(t.getSaldo().subtract(montoBD));
            repositorioUsuarios.save(u);
            servicioTransacciones.registrarTransaccion(u, "Cobro pasaje (Admin)", monto);
            return true;
        }
        return false;
    }

    public boolean cobrarPasaje(String idUsuario, String numeroTarjeta, double monto) {
        Optional<Usuario> opt = repositorioUsuarios.findById(idUsuario);
        if (opt.isEmpty()) return false;

        Usuario u = opt.get();
        return u.getTarjetas().stream()
                .filter(t -> t.getNumeroTarjeta().equals(numeroTarjeta))
                .findFirst()
                .map(t -> {
                    BigDecimal montoBD = BigDecimal.valueOf(monto);
                    if (t.getSaldo().compareTo(montoBD) >= 0) {
                        t.setSaldo(t.getSaldo().subtract(montoBD));
                        repositorioUsuarios.save(u);
                        servicioTransacciones.registrarTransaccion(u, "Gasto pasaje: " + ocultarNumeroTarjeta(numeroTarjeta), monto);
                        return true;
                    }
                    return false;
                }).orElse(false);
    }

        public boolean editarCredenciales(String idUsuarioObjetivo, String nuevoNombre, String nuevoCorreo, String nuevaPassword, Usuario usuarioActual) {
            Optional<Usuario> optUsuario = repositorioUsuarios.findById(idUsuarioObjetivo);
            if (optUsuario.isEmpty()) return false;

            Usuario objetivo = optUsuario.get();
            if (!objetivo.isActivo()) return false;

            String correoAnterior = objetivo.getCorreo();
            boolean huboCambiosCriticos = false;

            if (!Usuario.ROLE_ADMIN.equals(usuarioActual.getRole()) && !usuarioActual.getId().equals(idUsuarioObjetivo)) {
                return false;
            }

            if (nuevoNombre != null && !nuevoNombre.isBlank()) objetivo.setNombre(nuevoNombre);
            
            if (nuevoCorreo != null && !nuevoCorreo.isBlank() && !nuevoCorreo.equals(correoAnterior)) {
                Optional<Usuario> existente = repositorioUsuarios.findByCorreo(nuevoCorreo);
                if (existente.isPresent() && !existente.get().getId().equals(idUsuarioObjetivo)) return false;
                objetivo.setCorreo(nuevoCorreo);
                huboCambiosCriticos = true;
            }
            
            if (nuevaPassword != null && !nuevaPassword.isBlank()) {
                objetivo.setPasswordHash(passwordEncoder.encode(nuevaPassword));
                huboCambiosCriticos = true;
            }

            repositorioUsuarios.save(objetivo);

            if (huboCambiosCriticos) {
                emailService.enviarNotificacionLogin(objetivo.getCorreo(), objetivo.getNombre()); 
                
                if (!correoAnterior.equals(objetivo.getCorreo())) {
                    emailService.enviarNotificacionLogin(correoAnterior, objetivo.getNombre());
                }
            }

            return true;
        }

    public boolean agregarNuevaTarjeta(String idUsuario, String nuevoNumero) {
        Optional<Usuario> opt = repositorioUsuarios.findById(idUsuario);
        if (opt.isPresent()) {
            Usuario u = opt.get();
            boolean existe = u.getTarjetas().stream().anyMatch(t -> t.getNumeroTarjeta().equals(nuevoNumero));
            if (!existe) {
                u.agregarTarjeta(nuevoNumero, BigDecimal.ZERO);
                repositorioUsuarios.save(u);
                return true;
            }
        }
        return false;
    }

    private String ocultarNumeroTarjeta(String numero) {
        if (numero == null || numero.length() < 4) return "****";
        return "******" + numero.substring(numero.length() - 4);
    }


    public String agregarNuevaTarjetaConValidacion(String idUsuario, String nuevoNumero) {
    Optional<Usuario> dueñoExistente = repositorioUsuarios.findByNumeroTarjetaEnLista(nuevoNumero);
    
    if (dueñoExistente.isPresent()) {
        return "La tarjeta ya está registrada por otro usuario.";
    }


    Optional<Usuario> opt = repositorioUsuarios.findById(idUsuario);
    if (opt.isPresent()) {
        Usuario u = opt.get();
        
        boolean yaLaTiene = u.getTarjetas().stream()
                .anyMatch(t -> t.getNumeroTarjeta().equals(nuevoNumero));
        
        if (!yaLaTiene) {
            u.agregarTarjeta(nuevoNumero, BigDecimal.ZERO);
            repositorioUsuarios.save(u);
            return "OK";
        } else {
            return "Ya tienes esta tarjeta agregada.";
        }
    }
    return "Usuario no encontrado.";
}

public boolean eliminarTarjetaDeUsuario(String usuarioId, String numeroTarjeta) {
    return repositorioUsuarios.findById(usuarioId).map(usuario -> {
        if (usuario.getTarjetas().size() > 1) {
            boolean removida = usuario.getTarjetas().removeIf(t -> 
                t.getNumeroTarjeta().equals(numeroTarjeta));
            
            if (removida) {
                repositorioUsuarios.save(usuario); 
                return true;
            }
        }
        return false; 
    }).orElse(false);
}


public void procesarNotificacionLogin(String correo) {
    repositorioUsuarios.findByCorreo(correo).ifPresent(u -> {
        try {
            emailService.enviarNotificacionLogin(u.getCorreo(), u.getNombre());
        } catch (Exception e) {
            System.err.println("No se pudo enviar la alerta de inicio de sesión: " + e.getMessage());
        }
    });
}  
public void actualizarPassword(String idUsuario, String nuevaPassword) {

        repositorioUsuarios.findById(idUsuario).ifPresent(usuario -> {
            String passwordEncriptada = passwordEncoder.encode(nuevaPassword);
            usuario.setPasswordHash(passwordEncriptada);
            
            repositorioUsuarios.save(usuario);
            
            try {
                emailService.enviarNotificacionLogin(usuario.getCorreo(), usuario.getNombre());
            } catch (Exception e) {
                System.err.println("No se pudo enviar la notificación de cambio de clave: " + e.getMessage());
            }
        });
    }

public Usuario buscarPorId(String id) {
    return repositorioUsuarios.findById(id).orElse(null);
}
}