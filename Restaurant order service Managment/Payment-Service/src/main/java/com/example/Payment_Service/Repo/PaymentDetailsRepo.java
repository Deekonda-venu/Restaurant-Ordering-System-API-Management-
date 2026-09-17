package com.example.Payment_Service.Repo;

import com.example.Payment_Service.Model.PaymentDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentDetailsRepo extends JpaRepository<PaymentDetails, Long> {

    List<PaymentDetails> findByOrderId(Long orderId);

}
