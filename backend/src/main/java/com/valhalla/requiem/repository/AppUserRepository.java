package com.valhalla.requiem.repository;

import com.valhalla.requiem.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, String> { }
