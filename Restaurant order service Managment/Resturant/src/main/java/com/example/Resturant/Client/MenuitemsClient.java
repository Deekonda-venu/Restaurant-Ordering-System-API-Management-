package com.example.Resturant.Client;

import com.example.Resturant.Respose.MenuItemsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "MenuitemsClient", url = "http://localhost:9292")
public interface MenuitemsClient {

    @GetMapping("/API/menuitems/v1/GetMenuItemsByResturant/{resturantId}")
    ResponseEntity<List<MenuItemsResponse>> getMenuItemsByResturant(@PathVariable("resturantId") Long resturantId);

}
