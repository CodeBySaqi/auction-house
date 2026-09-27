package com.auctionhouse.repository;

import com.auctionhouse.model.AuctionImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuctionImageRepository extends JpaRepository<AuctionImage, Long> {

    List<AuctionImage> findByAuctionIdOrderBySortOrderAscIdAsc(@Param("auctionId") Long auctionId);

    @Modifying
    @Query("DELETE FROM AuctionImage i WHERE i.auction.id = :auctionId")
    void deleteByAuctionId(@Param("auctionId") Long auctionId);
}
