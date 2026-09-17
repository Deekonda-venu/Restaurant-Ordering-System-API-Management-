package com.example.Customer.Service.Service;

import com.example.Customer.Service.Model.AddressDetails;
import com.example.Customer.Service.Model.CustomerDetails;
import com.example.Customer.Service.Repo.AddressRespositary;
import com.example.Customer.Service.Repo.CustomerRespositary;
import com.example.Customer.Service.Response.AddressResponse;
import com.example.Customer.Service.Response.CustomerResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerService {

    @Autowired
    private CustomerRespositary customerRespositary;

    @Autowired
    private AddressRespositary addressRespositary;

    public CustomerDetails createCustomer(CustomerDetails customerDetails){
        return customerRespositary.save(customerDetails);
    }
    public List<CustomerDetails> getAllCustomerDetails(){
        return customerRespositary.findAll();
    }
    public CustomerResponse getCustomerDetailsById(Long id){
        CustomerDetails customerdetails = customerRespositary.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        List<AddressDetails> addressDetailsList = addressRespositary.findByCustomerId(id);

        List<AddressResponse> addressResponseList = new ArrayList<>();
        for (AddressDetails addressdetails : addressDetailsList) {
            addressResponseList.add(new AddressResponse(
                    addressdetails.getId(),
                    addressdetails.getAddressLine1(),
                    addressdetails.getAddressLine2(),
                    addressdetails.getCity(),
                    addressdetails.getState(),
                    addressdetails.getPostalCode(),
                    addressdetails.getAddressType()));
        }

        CustomerResponse response = new CustomerResponse();
        response.setCustomerDetails(customerdetails);
        response.setAddressResponse(addressResponseList);
        return response;
    }
    public CustomerDetails updateFullCustomerDetails(Long id ,CustomerDetails customerDetails){
        CustomerDetails customerdetails = customerRespositary.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        customerdetails.setFirstName(customerDetails.getFirstName());
        customerdetails.setLastName(customerDetails.getLastName());
        customerdetails.setEmail(customerDetails.getEmail());
        customerdetails.setPhone(customerDetails.getPhone());
        return customerRespositary.save(customerdetails);
    }
    public void deleteCustomerDetails(Long id){
        CustomerDetails customerdetails = customerRespositary.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        customerRespositary.delete(customerdetails);
    }

    public AddressDetails createAddressDetails(AddressDetails addressDetails){
        return addressRespositary.save(addressDetails);
    }
    public CustomerDetails getCustomerById(Long id){
        return customerRespositary.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
    }
    public void deleteAddressByCustomerId(Long CustomerId, Long Address_id){
        AddressDetails addressdetails = addressRespositary.findById(Address_id)
                .orElseThrow(() -> new RuntimeException("Address not found with id: " + Address_id));
        addressRespositary.delete(addressdetails);
    }
    public AddressResponse getAddressByCustomerAndAddressId(Long customerId, Long addressId){
        AddressDetails address = addressRespositary.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found with id: " + addressId));

        if (!address.getCustomerId().equals(customerId)) {
            throw new RuntimeException("Address " + addressId + " does not belong to customer " + customerId);
        }

        return new AddressResponse(
                address.getId(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getAddressType());
    }


}
