package lk.ijse.spring.smartplantmanagementsystem.repository;

import lk.ijse.spring.smartplantmanagementsystem.entity.ExpertProfile;
import lk.ijse.spring.smartplantmanagementsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExpertProfileRepository extends JpaRepository<ExpertProfile, Long> {
    Optional<ExpertProfile> findByUser(User user);

    @Query("""
    select p from ExpertProfile p
    where lower(p.about) like concat('%', :q, '%')
       or lower(p.specializations) like concat('%', :q, '%')
       or lower(p.education) like concat('%', :q, '%')
       or lower(p.experience) like concat('%', :q, '%')
       or lower(p.user.email) like concat('%', :q, '%')
  """)
    List<ExpertProfile> searchAll(@Param("q") String q);
}
