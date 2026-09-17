package com.example.Resturant.Respose;

import lombok.Data;

@Data
public class MenuItemsResponse {

    private Long id;
    private String name;
    private String description;
    private double price;
    private String category;
    private String vegNonVeg;
    private Long resturantId;
}
