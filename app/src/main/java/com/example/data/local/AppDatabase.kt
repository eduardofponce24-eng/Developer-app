package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Expense
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Expense::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expenses_database"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed initial sample expenses for immediate visual preview
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                populateInitialExpenses(database.expenseDao())
                            }
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateInitialExpenses(dao: ExpenseDao) {
            val now = System.currentTimeMillis()
            val day = 24 * 60 * 60 * 1000L
            val sampleExpenses = listOf(
                Expense(
                    title = "Supermercado semanal",
                    amount = 145.80,
                    category = "comida",
                    timestamp = now - (day * 1),
                    note = "Compra de despensa y verduras"
                ),
                Expense(
                    title = "Alquiler / Hipoteca",
                    amount = 450.00,
                    category = "hogar",
                    timestamp = now - (day * 2),
                    note = "Pago mensual"
                ),
                Expense(
                    title = "Cena con amigos",
                    amount = 68.50,
                    category = "comida",
                    timestamp = now - (day * 3),
                    note = "Pizzería céntrica"
                ),
                Expense(
                    title = "Combustible coche",
                    amount = 75.00,
                    category = "transporte",
                    timestamp = now - (day * 4),
                    note = "Tanque lleno"
                ),
                Expense(
                    title = "Factura de Luz e Internet",
                    amount = 92.40,
                    category = "servicios",
                    timestamp = now - (day * 5),
                    note = "Servicios del mes"
                ),
                Expense(
                    title = "Zapatillas deportivas",
                    amount = 89.99,
                    category = "compras",
                    timestamp = now - (day * 6),
                    note = "Descuento en tienda"
                ),
                Expense(
                    title = "Suscripción Cine & Streaming",
                    amount = 24.99,
                    category = "ocio",
                    timestamp = now - (day * 7),
                    note = "Plataformas mensuales"
                ),
                Expense(
                    title = "Farmacia y Vitaminas",
                    amount = 35.20,
                    category = "salud",
                    timestamp = now - (day * 8),
                    note = "Cuidado personal"
                )
            )
            dao.insertAll(sampleExpenses)
        }
    }
}
