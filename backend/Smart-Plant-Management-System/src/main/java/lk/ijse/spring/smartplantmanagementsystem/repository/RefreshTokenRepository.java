package lk.ijse.spring.smartplantmanagementsystem.repository;

import lk.ijse.spring.smartplantmanagementsystem.entity.RefreshToken;
import lk.ijse.spring.smartplantmanagementsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByUser(User user);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RefreshToken rt WHERE rt.token = :token")
    void deleteByToken(@Param("token") String token);
}
