package com.transcaribe.transcaribe.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Tarjeta {

    private String numeroTarjeta;
    private LocalDateTime fechaCreacion;
    private BigDecimal saldo;

    public Tarjeta() {
    }

    public Tarjeta(String numeroTarjeta, BigDecimal saldo) {
        this.numeroTarjeta = numeroTarjeta;
        this.saldo = saldo;
        this.fechaCreacion = LocalDateTime.now();
    }

    // Getters y Setters
    public String getNumeroTarjeta() {
        return numeroTarjeta;
    }

    public void setNumeroTarjeta(String numeroTarjeta) {
        this.numeroTarjeta = numeroTarjeta;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }
}