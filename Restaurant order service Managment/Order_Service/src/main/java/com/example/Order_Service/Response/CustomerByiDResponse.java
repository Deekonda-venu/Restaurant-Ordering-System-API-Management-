package com.example.Order_Service.Response;

import lombok.Data;
import java.util.List;

@Data
public class CustomerByiDResponse {

    private Long Customerid;
    private List<OrderdetailsforCustomer> orders;
}
