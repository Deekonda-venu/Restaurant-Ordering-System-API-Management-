package com.example.Payment_Service.Contoller;

import com.example.Payment_Service.Model.paymentRequestBody;
import com.example.Payment_Service.Response.PaymentRespose;
import com.example.Payment_Service.Service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/API/Payments/v1")
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @PostMapping("/payment")
    public PaymentRespose createPayment(@RequestBody paymentRequestBody paymentRequestBody) {
        return paymentService.createPayment(paymentRequestBody);
    }
    @GetMapping("/payment/{id}")
    public PaymentRespose getPaymentDetailsById(@PathVariable Long id) {
        return paymentService.getPaymentDetailsById(id);
    }

    @GetMapping("/order/{orderId}")
    public List<PaymentRespose> getPaymentsByOrderId(@PathVariable Long orderId) {
        return paymentService.getPaymentsByOrderId(orderId);
    }

    @PostMapping("/{paymentId}/refund")
    public PaymentRespose refundPayment(@PathVariable Long paymentId) {
        return paymentService.refundPayment(paymentId);
    }
}
