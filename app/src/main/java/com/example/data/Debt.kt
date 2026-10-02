package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey val id: String,
    val creditorName: String,
    val contractDate: String,
    val factAmount: Double,
    val penaltyAmount: Double,
    val insuranceAmount: Double = 0.0,
    val planAmount: Double,
    val status: String,
    val isMortgage: Boolean = false,
    val deadlineDate: String = "",
    val deadlineType: String = ""
)
