package com.auctionhouse.repository;

import com.auctionhouse.model.Slide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SlideRepository extends JpaRepository<Slide, Long> {
    List<Slide> findByActiveTrueOrderBySortOrderAsc();
    List<Slide> findAllByOrderBySortOrderAsc();
}
