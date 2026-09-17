package com.example.Order_Service.Response;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
@Data
@JsonPropertyOrder({"id", "resturantName", "description", "phone", "email", "status"})
public class ResturantResponse {

    private Long id;
    private String resturantName;
    private String description;
    private String phone;
    private String email;
//    private String address;
//    private String city;
//    private LocalTime openingTime;
//    private LocalTime closingTime;
    private String status;
//    private LocalDateTime createdAt;
//    private LocalDateTime updatedAt;

}
