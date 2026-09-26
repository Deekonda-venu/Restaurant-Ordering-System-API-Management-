package com.example.Payment_Service.Service;
import com.example.Payment_Service.Clinet.OrderServiceClient;
import com.example.Payment_Service.Event.PaymentEvent;
import com.example.Payment_Service.Event.PaymentEventPublisher;

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

    @Autowired
    private OrderServiceClient orderServiceClient;

    @Autowired
    private PaymentEventPublisher paymentEventPublisher;

    public PaymentRespose createPayment(paymentRequestBody paymentRequestBody) {

        // 1. Validate the order before processing a payment.
        // If the order does not exist, no payment should be created.
        if (paymentRequestBody.getOrderId() == null) {
            throw new RuntimeException("Order id is required");
        }
        orderServiceClient.getOrderById(paymentRequestBody.getOrderId());

        // 2. Prevent duplicate successful payments for the same order.
        // This protects the business rule: one successful payment per order.
        List<PaymentDetails> existingPayments = paymentDetailsRepo.findByOrderId(paymentRequestBody.getOrderId());
        boolean alreadyPaid = existingPayments.stream()
                .anyMatch(payment -> "SUCCESS".equalsIgnoreCase(payment.getStatus()));
        if (alreadyPaid) {
            throw new RuntimeException("Payment already completed for this order");
        }

        // 3. Make sure the customer exists before assigning the payment to them.
        CustomerRespose customer = customerClinet.getCustomerById(paymentRequestBody.getCustomerId());

        // 4. Build and save the payment record.
        // This is the actual payment transaction in the payment database.
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

        // 5. Publish a payment event to Kafka.
        // Order service listens for this event and updates the order paymentStatus.
        PaymentEvent event = new PaymentEvent();

        event.setEventType(
                "SUCCESS".equalsIgnoreCase(saved.getStatus())
                        ? "PAYMENT_SUCCESS"
                        : "PAYMENT_FAILED"
        );

        event.setPaymentId(saved.getId());
        event.setOrderId(saved.getOrderId());
        event.setCustomerId(saved.getCustomerId());
        event.setAmount(saved.getAmount());
        event.setStatus(saved.getStatus());
        event.setTransactionId(saved.getTransactionId());
        event.setOccurredAt(LocalDateTime.now());

        paymentEventPublisher.publish(event);

        // 6. Return DTO response to the caller.
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
