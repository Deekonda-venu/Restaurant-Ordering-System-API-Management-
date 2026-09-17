package com.example.Order_Service.Repositary;

import com.example.Order_Service.Model.OrderDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDetailesRepo extends JpaRepository<OrderDetails, Long> {
    List<OrderDetails> findByCustomerId(Long customerId);
    List<OrderDetails> findByRestaurantId(Long restaurantId);
}
