package com.example.Order_Service.Response;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
@Data
@JsonPropertyOrder({"id", "firstName", "lastName", "email", "phone", "address"})
public class CustomerResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private AddressRespose address;
}
