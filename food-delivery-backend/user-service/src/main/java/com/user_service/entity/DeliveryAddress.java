package com.user_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "delivery_addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAddress {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_profile_id", nullable = false)
	private UserProfile profile;

	@Column(nullable = false, length = 64)
	private String label;

	@Column(nullable = false, length = 255)
	private String line1;

	@Column(length = 255)
	private String line2;

	@Column(nullable = false, length = 120)
	private String city;

	@Column(name = "postal_code", nullable = false, length = 32)
	private String postalCode;

	@Column(name = "default_address", nullable = false)
	@Builder.Default
	private boolean defaultAddress = false;
}
