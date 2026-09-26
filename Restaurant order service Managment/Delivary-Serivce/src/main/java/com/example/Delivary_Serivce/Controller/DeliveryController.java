package com.example.Delivary_Serivce.Controller;

import com.example.Delivary_Serivce.Model.Delivery;
import com.example.Delivary_Serivce.Service.DeliveryService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {
    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    // This endpoint assigns a driver to a delivery that is already ready for pickup.
    @PostMapping("/{orderId}/assign-driver")
    public Delivery assignDriver(@PathVariable Long orderId,
                                @RequestBody Map<String, Long> body) {
        return deliveryService.assignDriver(orderId, body.get("driverId"));
    }

    // This endpoint updates the delivery lifecycle status and publishes a Kafka event.
    @PatchMapping("/{orderId}/status")
    public Delivery updateStatus(@PathVariable Long orderId,
                                @RequestBody Map<String, String> body) {
        return deliveryService.updateStatus(orderId, body.get("status"));
    }
}
