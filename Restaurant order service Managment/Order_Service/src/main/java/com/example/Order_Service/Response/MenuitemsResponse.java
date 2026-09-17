package com.example.Order_Service.Response;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
@Data
@JsonPropertyOrder({"id", "name", "description", "price", "category", "vegNonVeg"})
public class MenuitemsResponse {

    private Long id;
    private String name;
    private String description;
    private double price;
    private String category;
    private String vegNonVeg;


}
