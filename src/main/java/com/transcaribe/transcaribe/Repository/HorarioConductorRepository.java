package com.transcaribe.transcaribe.Repository;

import com.transcaribe.transcaribe.Model.HorarioConductor;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface HorarioConductorRepository extends MongoRepository<HorarioConductor, String> {

    List<HorarioConductor> findByConductorIdOrderByFechaAscHoraInicioAsc(String conductorId);

    List<HorarioConductor> findAllByOrderByFechaAscHoraInicioAsc();

    Optional<HorarioConductor> findByIdAndConductorId(String id, String conductorId);
}
