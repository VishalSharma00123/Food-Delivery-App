package com.user_service.repository;

import com.user_service.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

	Optional<UserProfile> findByAuthUserId(Long authUserId);

	boolean existsByAuthUserId(Long authUserId);

	@Query("select distinct p from UserProfile p left join fetch p.addresses where p.id = :id")
	Optional<UserProfile> findByIdWithAddresses(@Param("id") Long id);

	@Query("select distinct p from UserProfile p left join fetch p.addresses where p.authUserId = :authUserId")
	Optional<UserProfile> findByAuthUserIdWithAddresses(@Param("authUserId") Long authUserId);
}
