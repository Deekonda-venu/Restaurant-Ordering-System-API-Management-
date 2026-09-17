package com.example.Order_Service.Clinet;

import com.example.Order_Service.Response.AddressRespose;
import com.example.Order_Service.Response.CustomerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "Customer-Service", url = "http://localhost:9293")
public interface CustomerClinet {

    @GetMapping("/API/customer/v1/GetCustomerById/{id}")
    CustomerResponse getCustomerById(@PathVariable("id") Long id);

    @GetMapping("/API/customer/v1/GetCustomerDetailsById/{customer_id}/Addresses/{address_id}")
    AddressRespose getAddressByCustomerAndAddressId(@PathVariable("customer_id") Long customerId,
                                                    @PathVariable("address_id") Long addressId);
}
