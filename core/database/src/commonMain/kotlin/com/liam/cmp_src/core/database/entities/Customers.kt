package com.liam.cmp_src.core.database.entities

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey
    val id: String,
    val ownerId: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val companyName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val region: String? = null,
    val postalCode: String? = null,
    val countryCode: String? = null,
    val notes: String? = null,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?

)