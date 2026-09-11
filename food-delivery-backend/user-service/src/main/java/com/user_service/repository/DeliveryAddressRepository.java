package com.user_service.repository;

import com.user_service.entity.DeliveryAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryAddressRepository extends JpaRepository<DeliveryAddress, Long> {

	List<DeliveryAddress> findByProfile_IdOrderByDefaultAddressDescIdAsc(Long profileId);

	List<DeliveryAddress> findByProfile_Id(Long profileId);
}
