package lk.ijse.spring.smartplantmanagementsystem.repository;

import lk.ijse.spring.smartplantmanagementsystem.entity.Plant;
import lk.ijse.spring.smartplantmanagementsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlantRepository extends JpaRepository<Plant, Long> {
    List<Plant> findByUser(User user);

    Optional<Object> findByUserAndScientificNameIgnoreCase(User user, String name);

    Optional<Plant> findTopByUserOrderByIdDesc(User user);

    @Query("select p from Plant p where p.user = :user and " +
            "(lower(p.scientificName) like lower(concat('%', :q, '%')) or " +
            " lower(p.commonName) like lower(concat('%', :q, '%')))")
    List<Plant> searchForUser(@Param("user") User user, @Param("q") String q);

    List<Plant> findTop1ByUserOrderByIdDesc(User user);
}
