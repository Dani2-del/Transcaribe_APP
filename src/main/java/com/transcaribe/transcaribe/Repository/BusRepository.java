package com.transcaribe.transcaribe.Repository;

import com.transcaribe.transcaribe.Model.Bus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusRepository extends MongoRepository<Bus, String> {
    
    // CORRECCIÓN: El nombre del método debe ser findByPlaca 
    // porque en tu modelo la variable se llama "placa".
    Optional<Bus> findByPlaca(String placa);
}