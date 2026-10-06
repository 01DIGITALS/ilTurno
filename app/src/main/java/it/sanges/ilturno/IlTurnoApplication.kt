package it.sanges.ilturno

import android.app.Application
import androidx.room.Room
import it.sanges.ilturno.data.local.AppDatabase
import it.sanges.ilturno.data.repository.ShiftRepository
import it.sanges.ilturno.util.DebugDiagnostics

class IlTurnoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DebugDiagnostics.configure(this)
        DebugDiagnostics.event("application.create sdk=${android.os.Build.VERSION.SDK_INT}")
    }
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "ilturno.db")
            .addMigrations(AppDatabase.MIGRATION_1_2).build()
    }
    val repository: ShiftRepository by lazy { ShiftRepository(database) }
}
