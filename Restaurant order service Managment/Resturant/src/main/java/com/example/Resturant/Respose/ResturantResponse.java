package com.example.Resturant.Respose;

import com.example.Resturant.Model.ResturantStatus;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResturantResponse {

    private Long id;
    private String resturantName;
    private String description;
    private String phone;
    private String email;
    private String address;
    private String city;
    private LocalTime openingTime;
    private LocalTime closingTime;
//    @Enumerated(EnumType.STRING)
    private ResturantStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<MenuItemsResponse> menuItems;



//    MenuItemsDetails menuItems;

}
