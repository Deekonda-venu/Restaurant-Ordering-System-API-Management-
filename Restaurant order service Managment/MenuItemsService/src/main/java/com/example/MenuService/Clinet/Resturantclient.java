package com.example.MenuService.Clinet;

import com.example.MenuService.Response.ResturantDetailsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.example.MenuService.Response.ResturantFullDetailsResponse;

import java.util.List;

@FeignClient(name = "Resturant", url = "http://localhost:9191")
public interface Resturantclient {

    @GetMapping("/API/resturant/v1/GetResturnentdetailsById/{id}")
    ResponseEntity<ResturantFullDetailsResponse> getResturantById(@PathVariable("id") Long id);

    @GetMapping("/API/resturant/v1/GetAllResturnentdetails")
    ResponseEntity<List<ResturantDetailsResponse>> getAllResturant();
}
