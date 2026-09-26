package com.example.Kitchen_Serivce.Controller;

import com.example.Kitchen_Serivce.Model.KitchenOrder;
import com.example.Kitchen_Serivce.Service.KitchenService;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/kitchen")
public class KitchenController {

    private final KitchenService kitchenService;

    public KitchenController(KitchenService kitchenService) {
        this.kitchenService = kitchenService;
    }

    @PatchMapping("/orders/{orderId}/status")
    public KitchenOrder updateStatus(@PathVariable Long orderId,
                                     @RequestBody Map<String, String> body) {
        return kitchenService.updateStatus(orderId, body.get("status"));
    }
}