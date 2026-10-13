package com.example.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Preferência de tema escolhida pelo usuário. */
enum class ThemeMode(val label: String) {
  SYSTEM("Tema do sistema"),
  LIGHT("Tema claro"),
  DARK("Tema escuro");

  fun next(): ThemeMode = entries[(ordinal + 1) % entries.size]

  fun isDark(systemIsDark: Boolean): Boolean = when (this) {
    SYSTEM -> systemIsDark
    LIGHT -> false
    DARK -> true
  }
}

/** Persistência simples da preferência (SharedPreferences — não precisa de DataStore pra um valor). */
object ThemePreferences {
  private const val FILE = "settings"
  private const val KEY = "theme_mode"

  fun load(context: Context): ThemeMode {
    val name = try {
      context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null)
    } catch (e: Exception) {
      null
    }
    return ThemeMode.entries.firstOrNull { it.name == name } ?: ThemeMode.SYSTEM
  }

  fun save(context: Context, mode: ThemeMode) {
    context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, mode.name).apply()
  }
}

/** Cor de texto legível sobre a cor de uma linha (preto em amarelo/coral/ouro, branco em azul/safira...). */
fun contentColorFor(background: Color): Color =
  if (background.luminance() > 0.4f) Color.Black else Color.White
