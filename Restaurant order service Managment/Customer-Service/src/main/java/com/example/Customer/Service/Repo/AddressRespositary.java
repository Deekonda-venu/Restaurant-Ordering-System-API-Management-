package com.example.Customer.Service.Repo;

import com.example.Customer.Service.Model.AddressDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRespositary extends JpaRepository<AddressDetails, Long> {
    List<AddressDetails> findByCustomerId(Long customerId);
}
