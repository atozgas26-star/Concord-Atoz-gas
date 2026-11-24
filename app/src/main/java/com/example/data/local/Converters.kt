package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.ProductCategory
import com.example.data.model.TransactionType
import com.example.data.model.UserRole

class Converters {
    @TypeConverter
    fun fromUserRole(role: UserRole): String = role.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = UserRole.valueOf(value)

    @TypeConverter
    fun fromProductCategory(category: ProductCategory): String = category.name

    @TypeConverter
    fun toProductCategory(value: String): ProductCategory = ProductCategory.valueOf(value)

    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
}
