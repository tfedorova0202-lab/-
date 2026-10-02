package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сущность для хранения сообщений чата в Room.
 * Обеспечивает персистентность переписки с ИИ-юристом при перезапусках приложения.
 */
@Entity(tableName = "chat_messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long
)
