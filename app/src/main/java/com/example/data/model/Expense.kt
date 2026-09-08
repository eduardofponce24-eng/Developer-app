package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ui.theme.CategoryBills
import com.example.ui.theme.CategoryEducation
import com.example.ui.theme.CategoryEntertainment
import com.example.ui.theme.CategoryFood
import com.example.ui.theme.CategoryHealth
import com.example.ui.theme.CategoryHome
import com.example.ui.theme.CategoryOther
import com.example.ui.theme.CategoryShopping
import com.example.ui.theme.CategoryTransport

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)

enum class ExpenseCategory(
    val id: String,
    val displayName: String,
    val color: Color
) {
    FOOD("comida", "Comida & Alimentos", CategoryFood),
    TRANSPORT("transporte", "Transporte & Movilidad", CategoryTransport),
    HOME("hogar", "Hogar & Vivienda", CategoryHome),
    BILLS("servicios", "Servicios & Facturas", CategoryBills),
    SHOPPING("compras", "Compras & Ropa", CategoryShopping),
    ENTERTAINMENT("ocio", "Ocio & Diversión", CategoryEntertainment),
    HEALTH("salud", "Salud & Bienestar", CategoryHealth),
    EDUCATION("educacion", "Educación", CategoryEducation),
    OTHER("otros", "Otros Gastos", CategoryOther);

    val icon: ImageVector
        get() = when (this) {
            FOOD -> Icons.Default.Fastfood
            TRANSPORT -> Icons.Default.DirectionsCar
            HOME -> Icons.Default.Home
            BILLS -> Icons.Default.Receipt
            SHOPPING -> Icons.Default.ShoppingBag
            ENTERTAINMENT -> Icons.Default.SportsEsports
            HEALTH -> Icons.Default.Favorite
            EDUCATION -> Icons.Default.School
            OTHER -> Icons.Default.MoreHoriz
        }

    companion object {
        fun fromId(id: String): ExpenseCategory {
            return entries.find { it.id.equals(id, ignoreCase = true) || it.displayName.equals(id, ignoreCase = true) }
                ?: OTHER
        }
    }
}
