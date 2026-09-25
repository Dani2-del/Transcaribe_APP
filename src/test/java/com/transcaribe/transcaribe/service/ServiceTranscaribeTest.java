package com.transcaribe.transcaribe.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.transcaribe.transcaribe.Model.Transaccion;
import com.transcaribe.transcaribe.Model.Usuario;
import com.transcaribe.transcaribe.Repository.TransaccionRepository;
import com.transcaribe.transcaribe.Repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ServiceTranscaribeTest {

    @Mock
    private UsuarioRepository repositorioUsuarios;

    @Mock
    private TransaccionRepository repositorioTransacciones;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void calculaYRegistraElTotalSegunLaCantidadDePasajes() {
        ServiceTranscaribe service = new ServiceTranscaribe(
                repositorioUsuarios,
                new TransaccionService(repositorioTransacciones),
                passwordEncoder,
                null);
        Usuario usuario = new Usuario("pasajero@example.com", "hash", "Pasajero");
        usuario.agregarTarjeta("1234567890", BigDecimal.ZERO);
        when(repositorioTransacciones.save(any(Transaccion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var transaccion = service.recargarPasajesEnTarjetaConRecibo(
                usuario, "1234567890", 10, "PSE", "pasajero.pse@example.com");

        assertTrue(transaccion.isPresent());
        assertEquals(39000.0, transaccion.get().getMonto());
        assertEquals(10, transaccion.get().getCantidadPasajes());
        assertEquals(0, new BigDecimal("39000").compareTo(usuario.getTarjetas().get(0).getSaldo()));
        verify(repositorioTransacciones).save(any(Transaccion.class));
    }

    @Test
    void registraElMontoEscritoEnLaRecargaNormal() {
        ServiceTranscaribe service = new ServiceTranscaribe(
                repositorioUsuarios,
                new TransaccionService(repositorioTransacciones),
                passwordEncoder,
                null);
        Usuario usuario = new Usuario("pasajero@example.com", "hash", "Pasajero");
        usuario.agregarTarjeta("1234567890", BigDecimal.ZERO);
        when(repositorioTransacciones.save(any(Transaccion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var transaccion = service.recargarEnTarjetaConRecibo(
                usuario, "1234567890", 12500.0, "PSE", "pasajero.pse@example.com");

        assertTrue(transaccion.isPresent());
        assertEquals(12500.0, transaccion.get().getMonto());
        assertNull(transaccion.get().getCantidadPasajes());
        assertEquals(0, new BigDecimal("12500").compareTo(usuario.getTarjetas().get(0).getSaldo()));
        verify(repositorioTransacciones).save(any(Transaccion.class));
    }
}
