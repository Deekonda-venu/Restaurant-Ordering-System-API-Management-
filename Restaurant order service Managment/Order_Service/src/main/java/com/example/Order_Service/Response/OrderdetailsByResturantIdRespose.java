package com.example.Order_Service.Response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderdetailsByResturantIdRespose {

    private Long orderid;
    private Long Customerid;
    private String Status;
    private String Paymentstatus;
    private BigDecimal totalamount;
    private String CreatedAt;
}
