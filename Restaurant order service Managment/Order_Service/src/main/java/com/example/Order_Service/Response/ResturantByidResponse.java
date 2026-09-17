package com.example.Order_Service.Response;

import lombok.Data;
import java.util.List;

@Data
public class ResturantByidResponse {

    private Long Resturantid;
    private List<OrderdetailsByResturantIdRespose> orders;
}
