package com.example.Order_Service.Clinet;

import com.example.Order_Service.Response.ResturantResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "Resturant", url = "http://localhost:9191")
public interface ResturnatClinet {

    @GetMapping("/API/resturant/v1/GetResturnentdetailsById/{id}")
    ResturantResponse getResturentDetailsById(@PathVariable("id") Long id);
}
