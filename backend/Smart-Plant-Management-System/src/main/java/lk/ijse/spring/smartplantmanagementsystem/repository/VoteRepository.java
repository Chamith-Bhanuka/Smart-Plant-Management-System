package lk.ijse.spring.smartplantmanagementsystem.repository;

import lk.ijse.spring.smartplantmanagementsystem.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    Optional<Vote> findByPostIdAndUserId(Long postId, Long userId);
    int countByPostIdAndType(Long postId, Vote.Type type);
}
