package com.example.Payment_Service.Clinet;

import com.example.Payment_Service.Response.OrderValidationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "Order-Service", url = "http://localhost:9298")
public interface OrderServiceClient {

    @GetMapping("/API/Order/v1/GetOrderById/{OrderId}")
    OrderValidationResponse getOrderById(@PathVariable("OrderId") Long orderId);
}
