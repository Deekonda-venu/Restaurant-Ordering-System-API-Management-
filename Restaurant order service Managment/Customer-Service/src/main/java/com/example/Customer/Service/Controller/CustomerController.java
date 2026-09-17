package com.example.Customer.Service.Controller;

import com.example.Customer.Service.Model.AddressDetails;
import com.example.Customer.Service.Model.CustomerDetails;
import com.example.Customer.Service.Response.AddressResponse;
import com.example.Customer.Service.Response.CustomerResponse;
import com.example.Customer.Service.Service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/API/customer/v1")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @PostMapping("/CreateCustomer")
    public CustomerDetails createCustomer(@RequestBody CustomerDetails customerDetails){
        return customerService.createCustomer(customerDetails);
    }

    @GetMapping("/GetAllCustomerDetails")
    public List<CustomerDetails> getAllCustomerDetails(){
        return customerService.getAllCustomerDetails();
    }

    @GetMapping("/GetCustomerDetailsById/{customer_id}/Addresses")
    public CustomerResponse getCustomerDetailsById(@PathVariable("customer_id") Long id){
        return customerService.getCustomerDetailsById(id);
    }
    @GetMapping("/GetCustomerDetailsById/{customer_id}/Addresses/{address_id}")
    public AddressResponse getAddressByCustomerAndAddressId(
            @PathVariable("customer_id") Long customerId,
            @PathVariable("address_id") Long addressId){
        return customerService.getAddressByCustomerAndAddressId(customerId, addressId);
    }

    @PutMapping("/UpdateFullCustomerDetails/{id}")
    public CustomerDetails updateFullCustomerDetails(@PathVariable Long id, @RequestBody CustomerDetails customerDetails){
        return customerService.updateFullCustomerDetails(id, customerDetails);
    }
    @DeleteMapping("/DeleteCustomerDetails/{id}")
    public String deleteCustomerDetails(@PathVariable Long id){
        customerService.deleteCustomerDetails(id);
        return "Customer deleted successfully";
    }
    @PostMapping("/CreateAddressDetails/{Customerid}/Address")
    public AddressDetails createAddressDetails(@RequestBody AddressDetails addressDetails){
        return customerService.createAddressDetails(addressDetails);
    }
    @GetMapping("/GetCustomerById/{id}")
    public CustomerDetails getCustomerById(@PathVariable Long id){
        return customerService.getCustomerById(id);
    }

    @DeleteMapping("/DeleteAddressByCustomerId/{customer_id}/addresses/{address_id}")
    public String deleteAddressByCustomerId(@PathVariable("customer_id") Long customerId, @PathVariable("address_id") Long addressId){
        customerService.deleteAddressByCustomerId(customerId, addressId);
        return "Address deleted successfully";
    }

}
