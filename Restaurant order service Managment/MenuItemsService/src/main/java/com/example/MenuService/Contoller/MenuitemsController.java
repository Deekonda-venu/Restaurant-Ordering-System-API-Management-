package com.example.MenuService.Contoller;

import com.example.MenuService.Respose.MenuitemsFullRespose;
import org.springframework.web.bind.annotation.PathVariable;
import com.example.MenuService.Model.MenuItems;
import com.example.MenuService.Respose.MenuitemsRespose;
import com.example.MenuService.Service.MenuitemsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/API/menuitems/v1")
public class MenuitemsController {


    @Autowired
    private MenuitemsService menuitemsService;

    @PostMapping("/saveMenuItems")
    public MenuItems saveMenuItems(@RequestBody MenuItems menuItems) {

        return menuitemsService.saveMenuItems(menuItems);
    }
    @GetMapping("/GetAllFooditems")
    public List<MenuitemsRespose> getAllFooditems() {
        return menuitemsService.getAllFooditems();
    }
    @GetMapping("/GetFooditemsById/{ID}")
    public MenuitemsFullRespose getMenuitemById(@PathVariable Long ID) {
        return menuitemsService.getMenuitemById(ID);
    }
    @PutMapping("/updateMenuitemsById/{ID}")
    public MenuItems updateMenuitemsById(@PathVariable Long ID, @RequestBody MenuItems menuitem) {
        return menuitemsService.updateMenuitemsById(ID, menuitem);
    }
    @DeleteMapping("/deleteMenuitemsById/{ID}")
    public String deleteMenuitemsById(@PathVariable Long ID) {
        menuitemsService.DeleteMenuitemsById(ID);
        return "Menuitem deleted successfully";
    }
    @PatchMapping("/PatchMenuitemsById/{Id}")
    public MenuItems patchMenuitemsById(@PathVariable Long Id, @RequestBody MenuItems menuitem) {
        return menuitemsService.patchMenuitemsById(Id, menuitem);
    }
    @GetMapping("/GetByResturant/{resturantId}")
    public List<MenuItems> getMenuItemsByResturant(
            @PathVariable Long resturantId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean veg) {
        return menuitemsService.getMenuItemsByResturant(resturantId, category, veg);
    }
    @GetMapping("/GetMenuItemsByResturant/{resturantId}")
    public List<MenuItems> getMenuItemsByResturant(@PathVariable Long resturantId) {
        return menuitemsService.getMenuItemsByResturant(resturantId);
    }
}
