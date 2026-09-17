package com.example.MenuService.Respose;

import com.example.MenuService.Response.ResturantDetailsResponse;
import lombok.Data;

@Data
public class MenuitemsRespose {

    private Long id;
    private String name;
    private String description;
    private double price;
    private String category;
    private String vegNonVeg;

    private ResturantDetailsResponse resturantDetails;

}
