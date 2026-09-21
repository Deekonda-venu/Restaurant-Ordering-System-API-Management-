package com.example.Payment_Service.Service;

import com.example.Payment_Service.Clinet.CustomerClinet;
import com.example.Payment_Service.Clinet.OrderServiceClient;
import com.example.Payment_Service.Event.PaymentEventPublisher;
import com.example.Payment_Service.Model.PaymentDetails;
import com.example.Payment_Service.Model.paymentRequestBody;
import com.example.Payment_Service.Repo.PaymentDetailsRepo;
import com.example.Payment_Service.Response.CustomerRespose;
import com.example.Payment_Service.Response.OrderValidationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceValidationTest {

    @Mock
    private PaymentDetailsRepo paymentDetailsRepo;

    @Mock
    private CustomerClinet customerClinet;

    @Mock
    private OrderServiceClient orderServiceClient;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void createPayment_shouldRejectUnknownOrderIdBeforeSavingPayment() {
        paymentRequestBody request = new paymentRequestBody();
        request.setOrderId(999L);
        request.setCustomerId(12L);
        request.setAmount(new BigDecimal("150.00"));
        request.setPaymentMethod("CARD");
        request.setStatus("SUCCESS");

        CustomerRespose customer = new CustomerRespose();
        customer.setId(12L);
        when(customerClinet.getCustomerById(12L)).thenReturn(customer);
        when(orderServiceClient.getOrderById(999L))
                .thenThrow(new RuntimeException("Order not found"));

        assertThrows(RuntimeException.class, () -> paymentService.createPayment(request));
        verify(paymentDetailsRepo, never()).save(any(PaymentDetails.class));
        verify(paymentEventPublisher, never()).publish(any());
    }

    @Test
    void createPayment_shouldRejectDuplicateSuccessfulPaymentForSameOrder() {
        paymentRequestBody request = new paymentRequestBody();
        request.setOrderId(10L);
        request.setCustomerId(12L);
        request.setAmount(new BigDecimal("150.00"));
        request.setPaymentMethod("CARD");
        request.setStatus("SUCCESS");

        OrderValidationResponse order = new OrderValidationResponse();
        order.setOrderId(10L);
        order.setCustomerId(12L);
        when(orderServiceClient.getOrderById(10L)).thenReturn(order);

        CustomerRespose customer = new CustomerRespose();
        customer.setId(12L);
        when(customerClinet.getCustomerById(12L)).thenReturn(customer);

        PaymentDetails existing = new PaymentDetails();
        existing.setOrderId(10L);
        existing.setStatus("SUCCESS");
        when(paymentDetailsRepo.findByOrderId(10L)).thenReturn(List.of(existing));

        assertThrows(RuntimeException.class, () -> paymentService.createPayment(request));
        verify(paymentDetailsRepo, never()).save(any(PaymentDetails.class));
        verify(paymentEventPublisher, never()).publish(any());
    }
}
