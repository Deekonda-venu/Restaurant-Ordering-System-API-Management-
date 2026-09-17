package com.example.Order_Service.Clinet;

import com.example.Order_Service.Response.MenuitemsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "MenuService", url = "http://localhost:9292")
public interface Menuitemsclinet {

    @GetMapping("/API/menuitems/v1/GetFooditemsById/{ID}")
    MenuitemsResponse getMenuitemById(@PathVariable("ID") Long id);
}
