package com.valhalla.requiem.repository;

import com.valhalla.requiem.domain.Game;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, String> { }
