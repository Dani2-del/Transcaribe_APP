package com.transcaribe.transcaribe.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;
import java.time.LocalTime;

@Document(collection = "horarios_conductor")
public class HorarioConductor {

    @Id
    private String id;

    @Field("conductor_id")
    private String conductorId;

    @Field("ruta")
    private String ruta;

    @Field("fecha")
    private LocalDate fecha;

    @Field("hora_inicio")
    private LocalTime horaInicio;

    @Field("hora_fin")
    private LocalTime horaFin;

    public static final String ESTADO_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_EN_CURSO = "EN_CURSO";
    public static final String ESTADO_FINALIZADA = "FINALIZADA";

    @Field("estado")
    private String estado = ESTADO_PENDIENTE;

    public HorarioConductor() {
    }

    public HorarioConductor(String conductorId, String ruta, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        this.conductorId = conductorId;
        this.ruta = ruta;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.estado = ESTADO_PENDIENTE;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getConductorId() { return conductorId; }
    public void setConductorId(String conductorId) { this.conductorId = conductorId; }

    public String getRuta() { return ruta; }
    public void setRuta(String ruta) { this.ruta = ruta; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }

    public String getEstado() { return estado != null ? estado : ESTADO_PENDIENTE; }
    public void setEstado(String estado) { this.estado = estado; }
}
