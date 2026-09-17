package com.example.Payment_Service.Service;

import com.example.Payment_Service.Clinet.CustomerClinet;
import com.example.Payment_Service.Model.PaymentDetails;
import com.example.Payment_Service.Model.paymentRequestBody;
import com.example.Payment_Service.Repo.PaymentDetailsRepo;
import com.example.Payment_Service.Response.CustomerRespose;
import com.example.Payment_Service.Response.PaymentRespose;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    @Autowired
    private PaymentDetailsRepo paymentDetailsRepo;
    @Autowired
    private CustomerClinet customerClinet;

    public PaymentRespose createPayment(paymentRequestBody paymentRequestBody) {

        // 1. validate customer exists
        CustomerRespose customer = customerClinet.getCustomerById(paymentRequestBody.getCustomerId());

        // 2. build and persist payment
        PaymentDetails payment = new PaymentDetails();
        payment.setOrderId(paymentRequestBody.getOrderId());
        payment.setCustomerId(customer.getId());
        payment.setAmount(paymentRequestBody.getAmount());
        payment.setPaymentMethod(paymentRequestBody.getPaymentMethod());
        payment.setStatus(paymentRequestBody.getStatus() != null
                ? paymentRequestBody.getStatus() : "SUCCESS");
        payment.setTransactionId("TXN-" + UUID.randomUUID());
        payment.setCreatedAt(LocalDateTime.now());
        PaymentDetails saved = paymentDetailsRepo.save(payment);

        // 3. build response
        return toResponse(saved);
    }

    public PaymentRespose getPaymentDetailsById(Long id) {
        PaymentDetails payment = paymentDetailsRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + id));
        return toResponse(payment);
    }

    public List<PaymentRespose> getPaymentsByOrderId(Long orderId) {
        return paymentDetailsRepo.findByOrderId(orderId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PaymentRespose refundPayment(Long paymentId) {
        PaymentDetails payment = paymentDetailsRepo.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));
        if (!"SUCCESS".equalsIgnoreCase(payment.getStatus())) {
            throw new RuntimeException("Only SUCCESS payments can be refunded. Current status: " + payment.getStatus());
        }
        payment.setStatus("REFUNDED");
        PaymentDetails saved = paymentDetailsRepo.save(payment);
        return toResponse(saved);
    }

    private PaymentRespose toResponse(PaymentDetails payment) {
        PaymentRespose resp = new PaymentRespose();
        resp.setId(payment.getId());
        resp.setOrderId(payment.getOrderId());
        resp.setCustomerId(payment.getCustomerId());
        resp.setAmount(payment.getAmount());
        resp.setPaymentMethod(payment.getPaymentMethod());
        resp.setStatus(payment.getStatus());
        resp.setTransactionId(payment.getTransactionId());
        resp.setCreatedAt(payment.getCreatedAt());
        return resp;
    }

}
