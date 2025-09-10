package lk.ijse.spring.smartplantmanagementsystem.repository;

import lk.ijse.spring.smartplantmanagementsystem.entity.Post;
import lk.ijse.spring.smartplantmanagementsystem.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    Page<Post> findByUser(User user, Pageable pageable);

    @Query("select p from Post p where " +
            "(lower(p.title) like lower(concat('%', :q, '%')) or " +
            " lower(cast(p.content as string)) like lower(concat('%', :q, '%')) or " +
            " lower(cast(p.tags as string)) like lower(concat('%', :q, '%')))")
    Page<Post> searchAll(@Param("q") String q, Pageable pageable);

    @Query("select p from Post p where p.user = :user and " +
            "(lower(p.title) like lower(concat('%', :q, '%')) or " +
            " lower(cast(p.content as string)) like lower(concat('%', :q, '%')) or " +
            " lower(cast(p.tags as string)) like lower(concat('%', :q, '%')))")
    Page<Post> searchMine(@Param("user") User user, @Param("q") String q, Pageable pageable);

    @Query(
            value = "select trim(jt.tag) as tag, count(p.id) as cnt " +
                    "from post p " +
                    "join json_table( replace(p.tags, ',', '\",\"'), '$[*]' " +
                    "   columns(tag varchar(255) path '$') " +
                    ") jt " +
                    "group by jt.tag",
            nativeQuery = true
    )
    List<Object[]> countTags();
}
