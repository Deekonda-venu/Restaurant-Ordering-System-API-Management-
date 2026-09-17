package com.example.Order_Service.Controller;
import java.util.List;
import org.springframework.web.bind.annotation.PatchMapping;
import com.example.Order_Service.Response.CustomerByiDResponse;
import com.example.Order_Service.Response.ResturantByidResponse;
import com.example.Order_Service.Model.OrderDetails;
import com.example.Order_Service.Response.OrderResponse;
import com.example.Order_Service.Service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
@RestController
@RequestMapping("/API/Order/v1")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/CreateOrder")
    public OrderResponse createOrder(@RequestBody OrderDetails orderDetails) {
        return orderService.createOrder(orderDetails);
    }
    @GetMapping("/GetOrderById/{OrderId}")
    public OrderResponse getOrderById(@PathVariable("OrderId") Long orderId) {
        return orderService.getOrderById(orderId);
    }
    @GetMapping("/GetAllOrders")
    public List<OrderResponse> getAllOrders() {
        return orderService.getallorder();
    }
    @PatchMapping("/orders/{orderId}/status")
    public OrderResponse updateOrderStatus(@PathVariable("orderId") Long orderId, @RequestBody String status) {
        return orderService.updateOrderStatus(orderId, status);
    }
    @GetMapping("/GetAllOrdersByCustomerId/{CustomerId}")
    public CustomerByiDResponse getAllOrdersByCustomerId(@PathVariable("CustomerId") Long customerId) {
        return orderService.getAllOrdersByCustomerId(customerId);
    }
    @GetMapping("/GetAllOrdersByResturantId/{ResturantId}")
    public ResturantByidResponse getAllOrdersByResturant(@PathVariable("ResturantId") Long resturantId) {
        return orderService.getAllOrdersByResturant(resturantId);
    }

    @PostMapping("/orders/{orderId}/cancel")
    public OrderResponse cancelOrder(@PathVariable("orderId") Long orderId) {
        return orderService.cancelOrder(orderId);
    }

    @GetMapping("/orders/{orderId}/Currentstatus")
    public OrderStatusResponse getOrderStatus(@PathVariable("orderId") Long orderId) {
        return orderService.getOrderStatus(orderId);
    }

}
