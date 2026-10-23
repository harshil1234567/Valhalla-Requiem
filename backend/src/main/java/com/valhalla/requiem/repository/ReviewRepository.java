package com.valhalla.requiem.repository;

import com.valhalla.requiem.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsBySteamIdAndGameId(String steamId, String gameId);
}
