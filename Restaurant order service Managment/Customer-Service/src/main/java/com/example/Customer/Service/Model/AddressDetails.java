package com.example.Customer.Service.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@Entity
@Table(name = "addresses")
@Data
@JsonPropertyOrder({"id", "customerId", "addressLine1", "addressLine2", "city", "state", "postalCode", "addressType"})
public class AddressDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long customerId;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String addressType;
}
