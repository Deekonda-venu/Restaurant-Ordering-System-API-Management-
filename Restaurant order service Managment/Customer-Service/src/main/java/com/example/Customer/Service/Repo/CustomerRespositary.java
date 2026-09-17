package com.example.Customer.Service.Repo;

import com.example.Customer.Service.Model.CustomerDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRespositary extends JpaRepository<CustomerDetails, Long> {

}
