package it.sanges.ilturno.data

import android.content.Context

object DepartmentPreferences {
    fun enabled(context: Context): Boolean = context.getSharedPreferences("department_settings", Context.MODE_PRIVATE).getBoolean("grouped", true)
    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences("department_settings", Context.MODE_PRIVATE).edit().putBoolean("grouped", enabled).apply()
    }
}
