package com.example.Resturant.Repo;

import com.example.Resturant.Model.ResturantDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ResturentRepo extends JpaRepository<ResturantDetails, Long> {
}
