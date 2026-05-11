package com.transcaribe.transcaribe.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.transcaribe.transcaribe.Model.Transaccion;
import com.transcaribe.transcaribe.Model.Usuario;
import com.transcaribe.transcaribe.Repository.TransaccionRepository;

@Service
public class TransaccionService {

    private final TransaccionRepository repositorioTransacciones;

    public TransaccionService(TransaccionRepository repositorioTransacciones) {
        this.repositorioTransacciones = repositorioTransacciones;
    }

    public void registrarTransaccion(Usuario usuario, String tipo, double monto) {
        Transaccion transaccion = new Transaccion(monto, LocalDateTime.now(), tipo, usuario);
        repositorioTransacciones.save(transaccion);
    }

    public List<Transaccion> obtenerTransaccionesPorUsuario(Usuario usuario) {
        return repositorioTransacciones.findByUsuario(usuario);
    }
}
