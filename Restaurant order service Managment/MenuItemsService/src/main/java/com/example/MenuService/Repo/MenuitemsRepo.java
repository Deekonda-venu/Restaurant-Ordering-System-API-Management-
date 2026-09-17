package com.example.MenuService.Repo;
import java.util.List;
import com.example.MenuService.Model.MenuItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface MenuitemsRepo extends JpaRepository<MenuItems, Long>{
    List<MenuItems> findByResturantId(Long resturantId);
    List<MenuItems> findByResturantIdAndCategory(Long resturantId, String category);
    List<MenuItems> findByResturantIdAndVegNonVeg(Long resturantId, String vegNonVeg);
    List<MenuItems> findByResturantIdAndCategoryAndVegNonVeg(Long resturantId, String category, String vegNonVeg);


}
