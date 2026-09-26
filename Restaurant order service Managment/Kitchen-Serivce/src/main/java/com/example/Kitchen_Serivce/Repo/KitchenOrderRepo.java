package com.example.Kitchen_Serivce.Repo;

import com.example.Kitchen_Serivce.Model.KitchenOrder;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface KitchenOrderRepo extends MongoRepository<KitchenOrder, String> {
    Optional<KitchenOrder> findByOrderId(Long orderId);
}