package com.example.Customer.Service.Response;

import com.example.Customer.Service.Model.CustomerDetails;
import lombok.Data;

import java.util.List;

@Data
public class CustomerResponse {

    private CustomerDetails customerDetails;
    private List<AddressResponse> addressResponse;

}
