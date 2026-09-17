package com.example.MenuService.Respose;

import com.example.MenuService.Response.ResturantFullDetailsResponse;
import lombok.Data;

@Data
public class MenuitemsFullRespose {
    private Long id;
    private String name;
    private String description;
    private double price;
    private String category;
    private String vegNonVeg;
    private ResturantFullDetailsResponse resturantDetails;
}
