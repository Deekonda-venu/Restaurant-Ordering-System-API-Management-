package com.example.MenuService.Model;
import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@Entity
@Table(name = "menu_items")
@Data
@JsonPropertyOrder({"id", "name", "description", "price", "category", "vegNonVeg", "resturantId"})
public class MenuItems {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private double price;
    private String category;
    private String vegNonVeg;
    private Long resturantId;



}
