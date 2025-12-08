package com.example.splitwise.repo;

import com.example.splitwise.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventRepo extends JpaRepository<Event, Long> {
    @Query("select e from Event e " +
            "left join fetch e.splits s " +
            "left join fetch s.user u " +
            "left join fetch e.creator c " +
            "where e.id = :id")
    Optional<Event> findByIdWithSplitsAndUsers(@Param("id") Long id);

    @Query("""
    select distinct e
    from Event e
    join e.splits s
    where s.user.id = :userId
    """)
    List<Event> findEventsByUser(@Param("userId") Long userId);


    @Query("select e from Event e left join fetch e.splits where e.id = :id")
    Event findWithSplitsById(@Param("id") Long id);
}
