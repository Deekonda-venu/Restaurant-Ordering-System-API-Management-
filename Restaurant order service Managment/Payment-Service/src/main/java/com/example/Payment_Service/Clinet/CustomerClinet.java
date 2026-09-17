package com.example.Payment_Service.Clinet;

import com.example.Payment_Service.Response.CustomerRespose;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "Customer-Service", url = "http://localhost:9293")
public interface CustomerClinet {

    @GetMapping("/API/customer/v1/GetCustomerById/{id}")
    CustomerRespose getCustomerById(@PathVariable("id") Long id);
}
