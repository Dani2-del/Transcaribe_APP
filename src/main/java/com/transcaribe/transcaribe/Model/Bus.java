package com.transcaribe.transcaribe.Model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "buses")
public class Bus {

    @Id
    private String id;

    @Field("placa")
    @Indexed(unique = true)
    private String placa;

    @Field("rutas_disponibles")
    private List<String> rutas; // Ejemplo: ["T101", "X104", "Ruta Circular"]

    @Field("activo")
    private boolean activo = true;

    public Bus() {}

    public Bus(String placa, List<String> rutas) {
        this.placa = placa;
        this.rutas = rutas;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }
    public void setPlaca(String placa) {
        this.placa = placa;
    }
    public void setRutas(List<String> rutas) {
        this.rutas = rutas;
    }

    public boolean isActivo() {
        return this.activo;
    }

    public boolean getActivo() {
        return this.activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    
    public List<String> getRutas() { return rutas; }
    public String getPlaca() { return placa; }
}