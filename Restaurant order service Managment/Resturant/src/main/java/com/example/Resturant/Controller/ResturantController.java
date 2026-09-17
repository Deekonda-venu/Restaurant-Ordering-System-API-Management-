package com.example.Resturant.Controller;

import com.example.Resturant.Model.ResturantDetails;
import com.example.Resturant.Respose.ResturantResponse;
import com.example.Resturant.Service.Resturentservice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/API/resturant/v1")
public class ResturantController {

    @Autowired
    private Resturentservice resturentservice;

    @PostMapping("/AddResturntdetails")
    public ResponseEntity<ResturantDetails> saveResturentDetails(@RequestBody ResturantDetails resturantDetails) {
        return ResponseEntity.ok(resturentservice.saveResturentDetils(resturantDetails));
    }

    @GetMapping("/GetAllResturnentdetails")
    public ResponseEntity<List<ResturantDetails>> getAllResturentDetails() {
        return ResponseEntity.ok(resturentservice.getAllResturentDetails());
    }

    @GetMapping("/GetResturnentdetailsById/{id}")
    public ResponseEntity<ResturantResponse> getResturentDetailsById(@PathVariable Long id) {
        return ResponseEntity.ok(resturentservice.getMenuItemsByResturant(id));
    }

    @PutMapping("/UpdateResturnentDetailsByID/{id}")
    public ResponseEntity<ResturantDetails> updateResturentDetailsById(@PathVariable Long id, @RequestBody ResturantDetails resturantDetails) {
        return ResponseEntity.ok(resturentservice.updateResturentDetailsById(id, resturantDetails));
    }
    @DeleteMapping("/DeleteResturentDetailsById/{id}")
    public ResponseEntity<String> deleteResturentDetailsById(@PathVariable Long id) {
        resturentservice.deleteResturentDetailsById(id);
        return ResponseEntity.ok("Resturant deleted successfully");
    }
    @PatchMapping("/UpdateResturnentDetailsByID/{id}")
    public ResponseEntity<ResturantDetails> patchResturentDetailsById(@PathVariable Long id, @RequestBody ResturantDetails resturantDetails) {
        return ResponseEntity.ok(resturentservice.patchResturentDetailsById(id, resturantDetails));
    }


}
