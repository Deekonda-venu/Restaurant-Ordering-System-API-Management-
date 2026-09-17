package com.example.Payment_Service.Response;

import lombok.Data;

@Data
public class CustomerRespose {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
}
