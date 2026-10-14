package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey val id: String = "",
    val creditorName: String = "",
    val contractDate: String = "",
    val factAmount: Double = 0.0,
    val penaltyAmount: Double = 0.0,
    val insuranceAmount: Double = 0.0,
    val planAmount: Double = 0.0,
    val status: String = "ACTIVE",
    val isMortgage: Boolean = false,
    val deadlineDate: String = "",
    val deadlineType: String = "",
    val legalBasis: String = ""
)
