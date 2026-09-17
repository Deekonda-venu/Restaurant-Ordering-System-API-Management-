package com.example.Order_Service.Response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderdetailsforCustomer {

    private Long orderid;
    private Long resturantid;
    private String Status;
    private String Paymentstatus;
    private BigDecimal totalamount;
    private String CreatedAt;
}
