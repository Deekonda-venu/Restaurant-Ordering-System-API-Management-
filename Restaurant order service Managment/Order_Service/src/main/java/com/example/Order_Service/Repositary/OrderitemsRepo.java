package com.example.Order_Service.Repositary;

import com.example.Order_Service.Model.OrderItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderitemsRepo extends JpaRepository<OrderItems, Long> {
    List<OrderItems> findByOrderId(Long orderId);
}
