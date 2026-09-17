package com.example.MenuService.Service;
import com.example.MenuService.Clinet.Resturantclient;
import com.example.MenuService.Model.MenuItems;
import com.example.MenuService.Repo.MenuitemsRepo;
import com.example.MenuService.Respose.MenuitemsRespose;
import com.example.MenuService.Response.ResturantDetailsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.MenuService.Respose.MenuitemsFullRespose;
import com.example.MenuService.Response.ResturantFullDetailsResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MenuitemsService {

    @Autowired
    private MenuitemsRepo menuitemsRepo;

    @Autowired
    private Resturantclient resturantclient;

    public MenuItems saveMenuItems(MenuItems menuItems) {
        resturantclient.getResturantById(menuItems.getResturantId());
        return menuitemsRepo.save(menuItems);
    }

    public List<MenuitemsRespose> getAllFooditems() {
        List<MenuItems> menuItemsList = menuitemsRepo.findAll();

        List<ResturantDetailsResponse> resturantDetailsResponses = resturantclient.getAllResturant().getBody();
        Map<Long, ResturantDetailsResponse> resturantMap = resturantDetailsResponses.stream()
                .collect(Collectors.toMap(ResturantDetailsResponse::getId, r -> r));

        List<MenuitemsRespose> result = new ArrayList<>();
        for (MenuItems menuItem : menuItemsList) {
            MenuitemsRespose respose = new MenuitemsRespose();
            respose.setId(menuItem.getId());
            respose.setName(menuItem.getName());
            respose.setDescription(menuItem.getDescription());
            respose.setPrice(menuItem.getPrice());
            respose.setCategory(menuItem.getCategory());
            respose.setVegNonVeg(menuItem.getVegNonVeg());
            respose.setResturantDetails(resturantMap.get(menuItem.getResturantId()));
            result.add(respose);
        }
        return result;
    }

    public MenuitemsFullRespose getMenuitemById(Long id) {
        MenuItems menuitems = menuitemsRepo.findById(id).orElseThrow(() -> new RuntimeException("MenuItem not found with id: " + id));
        ResturantFullDetailsResponse resturantFullDetailsResponse = resturantclient.getResturantById(menuitems.getResturantId()).getBody();
        MenuitemsFullRespose respose = new MenuitemsFullRespose();
        respose.setId(menuitems.getId());
        respose.setName(menuitems.getName());
        respose.setDescription(menuitems.getDescription());
        respose.setPrice(menuitems.getPrice());
        respose.setCategory(menuitems.getCategory());
        respose.setVegNonVeg(menuitems.getVegNonVeg());
        respose.setResturantDetails(resturantFullDetailsResponse);
        return respose;
    }

    public MenuItems updateMenuitemsById(Long id, MenuItems menuitem) {
        MenuItems menuitems = menuitemsRepo.findById(id).orElseThrow(() -> new RuntimeException("MenuItem not found with id: " + id));
        menuitems.setName(menuitem.getName());
        menuitems.setDescription(menuitem.getDescription());
        menuitems.setPrice(menuitem.getPrice());
        menuitems.setCategory(menuitem.getCategory());
        menuitems.setVegNonVeg(menuitem.getVegNonVeg());
        menuitems.setResturantId(menuitem.getResturantId());
        return menuitemsRepo.save(menuitems);
    }

    public void DeleteMenuitemsById(Long id){
        MenuItems menuitems = menuitemsRepo.findById(id).orElseThrow(() -> new RuntimeException("MenuItem not found with id: " + id));
        menuitemsRepo.delete(menuitems);

    }
    public MenuItems patchMenuitemsById(Long id, MenuItems menuitem){
        MenuItems menuitems = menuitemsRepo.findById(id).orElseThrow(() -> new RuntimeException("MenuItem not found with id: " + id));
        if (menuitem.getName() != null) menuitems.setName(menuitem.getName());
        if (menuitem.getDescription() != null) menuitems.setDescription(menuitem.getDescription());
        if (menuitem.getPrice() != 0) menuitems.setPrice(menuitem.getPrice());
        if (menuitem.getCategory() != null) menuitems.setCategory(menuitem.getCategory());
        if (menuitem.getVegNonVeg() != null) menuitems.setVegNonVeg(menuitem.getVegNonVeg());
        if (menuitem.getResturantId() != null) menuitems.setResturantId(menuitem.getResturantId());
        return menuitemsRepo.save(menuitems);
    }
    public List<MenuItems> getMenuItemsByResturant(Long resturantId, String category, Boolean veg) {
        if (category != null && veg != null) {
            return menuitemsRepo.findByResturantIdAndCategoryAndVegNonVeg(resturantId, category, veg ? "Veg" : "NonVeg");
        } else if (category != null) {
            return menuitemsRepo.findByResturantIdAndCategory(resturantId, category);
        } else if (veg != null) {
            return menuitemsRepo.findByResturantIdAndVegNonVeg(resturantId, veg ? "Veg" : "NonVeg");
        }
        return menuitemsRepo.findByResturantId(resturantId);
    }
    public List<MenuItems> getMenuItemsByResturant(Long resturantId){
        return menuitemsRepo.findByResturantId(resturantId);
    }
}
