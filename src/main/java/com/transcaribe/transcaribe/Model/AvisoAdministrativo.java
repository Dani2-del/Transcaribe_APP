package com.transcaribe.transcaribe.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "avisos_administrativos")
public class AvisoAdministrativo {

    @Id
    private String id;

    @Field("asunto")
    private String asunto;

    @Field("mensaje")
    private String mensaje;

    @Field("fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Field("destinatarios")
    private List<String> destinatarios = new ArrayList<>();

    @Field("descartada_por")
    private List<String> descartadaPor = new ArrayList<>();

    public AvisoAdministrativo() {
    }

    public AvisoAdministrativo(String asunto, String mensaje, List<String> destinatarios) {
        this.asunto = asunto;
        this.mensaje = mensaje;
        this.destinatarios = new ArrayList<>(destinatarios);
        this.fechaCreacion = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAsunto() { return asunto; }
    public void setAsunto(String asunto) { this.asunto = asunto; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public List<String> getDestinatarios() { return destinatarios; }
    public void setDestinatarios(List<String> destinatarios) { this.destinatarios = destinatarios; }

    public List<String> getDescartadaPor() {
        if (descartadaPor == null) {
            descartadaPor = new ArrayList<>();
        }
        return descartadaPor;
    }

    public void setDescartadaPor(List<String> descartadaPor) { this.descartadaPor = descartadaPor; }
}
