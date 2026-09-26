package com.example.Delivary_Serivce.Repo;

import com.example.Delivary_Serivce.Model.Delivery;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DeliveryRepo extends MongoRepository<Delivery, String> {
    // This repository lets us find a delivery task for a specific order.
    Optional<Delivery> findByOrderId(Long orderId);
}
