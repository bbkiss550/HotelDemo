package com.hotel.repository;

import com.hotel.model.AppUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    boolean existsByUsername(String username);

    @Query("""
            select u from AppUser u
            where lower(u.username) like lower(concat('%', :q, '%'))
               or lower(u.fullName) like lower(concat('%', :q, '%'))
            order by u.username
            """)
    List<AppUser> search(String q);
}
