package com.example.Order_Service.Response;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
@Data
@JsonPropertyOrder({"id", "addressLine1", "addressLine2", "city", "state", "postalCode", "addressType"})
public class AddressRespose {

    private Long id;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String addressType;
}
