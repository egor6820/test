package com.example.tossday.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Ocean (Blue) ──────────────────────────────────────────────────────────────
private val OceanLight = lightColorScheme(
    primary              = Color(0xFF1565C0),
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFD3E4FF),
    onPrimaryContainer   = Color(0xFF001D45),
    secondary            = Color(0xFF4F6680),
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFD5E4F7),
    onSecondaryContainer = Color(0xFF0A1E30),
    background           = Color(0xFFF6F8FA),
    onBackground         = Color(0xFF1A1C1E),
    surface              = Color(0xFFFFFFFF),
    onSurface            = Color(0xFF1A1C1E),
    surfaceVariant       = Color(0xFFECF0F4),
    onSurfaceVariant     = Color(0xFF44474E),
    outline              = Color(0xFFCAD0D6),
    outlineVariant       = Color(0xFFDEE3EA),
    inverseSurface       = Color(0xFF2E3135),
    inverseOnSurface     = Color(0xFFF0F0F3),
    inversePrimary       = Color(0xFFA5C8FF),
    surfaceTint          = Color(0xFF1565C0),
    error                = Color(0xFFBA1A1A),
    onError              = Color.White,
    errorContainer       = Color(0xFFFFDAD6),
    onErrorContainer     = Color(0xFF410002),
)
private val OceanDark = darkColorScheme(
    primary              = Color(0xFFA5C8FF),
    onPrimary            = Color(0xFF003063),
    primaryContainer     = Color(0xFF00428C),
    onPrimaryContainer   = Color(0xFFD3E4FF),
    secondary            = Color(0xFFB5CAE0),
    onSecondary          = Color(0xFF1F3348),
    secondaryContainer   = Color(0xFF364B60),
    onSecondaryContainer = Color(0xFFD1E5F7),
    background           = Color(0xFF0F1117),
    onBackground         = Color(0xFFE2E4E8),
    surface              = Color(0xFF1A1D24),
    onSurface            = Color(0xFFE2E4E8),
    surfaceVariant       = Color(0xFF252A34),
    onSurfaceVariant     = Color(0xFFC4C7CE),
    outline              = Color(0xFF3A3D45),
    outlineVariant       = Color(0xFF43474F),
    inverseSurface       = Color(0xFFE2E4E8),
    inverseOnSurface     = Color(0xFF2E3135),
    inversePrimary       = Color(0xFF1565C0),
    surfaceTint          = Color(0xFFA5C8FF),
    error                = Color(0xFFFFB4AB),
    onError              = Color(0xFF690005),
    errorContainer       = Color(0xFF93000A),
    onErrorContainer     = Color(0xFFFFDAD6),
)

// ── Forest (Green) ───────────────────────────────────────────────────────────
private val ForestLight = lightColorScheme(
    primary              = Color(0xFF2E7D32),
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFB8F0BE),
    onPrimaryContainer   = Color(0xFF002109),
    secondary            = Color(0xFF4E6651),
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFD0EDCF),
    onSecondaryContainer = Color(0xFF0B2011),
    background           = Color(0xFFF5F9F4),
    onBackground         = Color(0xFF191C19),
    surface              = Color(0xFFFFFFFF),
    onSurface            = Color(0xFF191C19),
    surfaceVariant       = Color(0xFFE5F1E5),
    onSurfaceVariant     = Color(0xFF424940),
    outline              = Color(0xFFC5CCBF),
    outlineVariant       = Color(0xFFDAE5D4),
    inverseSurface       = Color(0xFF2D312D),
    inverseOnSurface     = Color(0xFFEEF2EB),
    inversePrimary       = Color(0xFF82D887),
    surfaceTint          = Color(0xFF2E7D32),
    error                = Color(0xFFBA1A1A),
    onError              = Color.White,
    errorContainer       = Color(0xFFFFDAD6),
    onErrorContainer     = Color(0xFF410002),
)
private val ForestDark = darkColorScheme(
    primary              = Color(0xFF82D887),
    onPrimary            = Color(0xFF003910),
    primaryContainer     = Color(0xFF005319),
    onPrimaryContainer   = Color(0xFFB8F0BE),
    secondary            = Color(0xFFB5CCAF),
    onSecondary          = Color(0xFF213525),
    secondaryContainer   = Color(0xFF374D3A),
    onSecondaryContainer = Color(0xFFD0EDCF),
    background           = Color(0xFF101410),
    onBackground         = Color(0xFFDFE5D9),
    surface              = Color(0xFF1A1E1A),
    onSurface            = Color(0xFFDFE5D9),
    surfaceVariant       = Color(0xFF232A24),
    onSurfaceVariant     = Color(0xFFC0CAB9),
    outline              = Color(0xFF3A4438),
    outlineVariant       = Color(0xFF424940),
    inverseSurface       = Color(0xFFDFE5D9),
    inverseOnSurface     = Color(0xFF2D312D),
    inversePrimary       = Color(0xFF2E7D32),
    surfaceTint          = Color(0xFF82D887),
    error                = Color(0xFFFFB4AB),
    onError              = Color(0xFF690005),
    errorContainer       = Color(0xFF93000A),
    onErrorContainer     = Color(0xFFFFDAD6),
)

// ── Lavender (Purple) ────────────────────────────────────────────────────────
private val LavenderLight = lightColorScheme(
    primary              = Color(0xFF6A1B9A),
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFEFD5FF),
    onPrimaryContainer   = Color(0xFF280049),
    secondary            = Color(0xFF62527A),
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFE8D9FF),
    onSecondaryContainer = Color(0xFF1F0D33),
    background           = Color(0xFFF9F5FF),
    onBackground         = Color(0xFF1D1A22),
    surface              = Color(0xFFFFFFFF),
    onSurface            = Color(0xFF1D1A22),
    surfaceVariant       = Color(0xFFF2EAF8),
    onSurfaceVariant     = Color(0xFF4A4356),
    outline              = Color(0xFFCDC5D8),
    outlineVariant       = Color(0xFFE2D9EE),
    inverseSurface       = Color(0xFF312E38),
    inverseOnSurface     = Color(0xFFF4EFF9),
    inversePrimary       = Color(0xFFD9A9FF),
    surfaceTint          = Color(0xFF6A1B9A),
    error                = Color(0xFFBA1A1A),
    onError              = Color.White,
    errorContainer       = Color(0xFFFFDAD6),
    onErrorContainer     = Color(0xFF410002),
)
private val LavenderDark = darkColorScheme(
    primary              = Color(0xFFD9A9FF),
    onPrimary            = Color(0xFF3E0068),
    primaryContainer     = Color(0xFF580090),
    onPrimaryContainer   = Color(0xFFEFD5FF),
    secondary            = Color(0xFFCFBFE8),
    onSecondary          = Color(0xFF32234A),
    secondaryContainer   = Color(0xFF493A61),
    onSecondaryContainer = Color(0xFFE8D9FF),
    background           = Color(0xFF130E1A),
    onBackground         = Color(0xFFE8E0F0),
    surface              = Color(0xFF1C1724),
    onSurface            = Color(0xFFE8E0F0),
    surfaceVariant       = Color(0xFF29222F),
    onSurfaceVariant     = Color(0xFFCCC4D9),
    outline              = Color(0xFF453E52),
    outlineVariant       = Color(0xFF4A4356),
    inverseSurface       = Color(0xFFE8E0F0),
    inverseOnSurface     = Color(0xFF312E38),
    inversePrimary       = Color(0xFF6A1B9A),
    surfaceTint          = Color(0xFFD9A9FF),
    error                = Color(0xFFFFB4AB),
    onError              = Color(0xFF690005),
    errorContainer       = Color(0xFF93000A),
    onErrorContainer     = Color(0xFFFFDAD6),
)

// ── Neutral (Charcoal) ───────────────────────────────────────────────────────
private val NeutralLight = lightColorScheme(
    primary              = Color(0xFF37474F),
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFCEDDE6),
    onPrimaryContainer   = Color(0xFF0D1E26),
    secondary            = Color(0xFF546E7A),
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFD8E8F0),
    onSecondaryContainer = Color(0xFF112229),
    background           = Color(0xFFF5F7F9),
    onBackground         = Color(0xFF1A1C1E),
    surface              = Color(0xFFFFFFFF),
    onSurface            = Color(0xFF1A1C1E),
    surfaceVariant       = Color(0xFFECEFF1),
    onSurfaceVariant     = Color(0xFF44474E),
    outline              = Color(0xFFB0BEC5),
    outlineVariant       = Color(0xFFCFD8DC),
    inverseSurface       = Color(0xFF2E3135),
    inverseOnSurface     = Color(0xFFF0F0F3),
    inversePrimary       = Color(0xFF90A4AE),
    surfaceTint          = Color(0xFF37474F),
    error                = Color(0xFFBA1A1A),
    onError              = Color.White,
    errorContainer       = Color(0xFFFFDAD6),
    onErrorContainer     = Color(0xFF410002),
)
private val NeutralDark = darkColorScheme(
    primary              = Color(0xFF90A4AE),
    onPrimary            = Color(0xFF0D1E26),
    primaryContainer     = Color(0xFF1E3540),
    onPrimaryContainer   = Color(0xFFCEDDE6),
    secondary            = Color(0xFFB0C4CE),
    onSecondary          = Color(0xFF1B2F38),
    secondaryContainer   = Color(0xFF2F454F),
    onSecondaryContainer = Color(0xFFD8E8F0),
    background           = Color(0xFF0F1113),
    onBackground         = Color(0xFFE0E2E4),
    surface              = Color(0xFF191B1E),
    onSurface            = Color(0xFFE0E2E4),
    surfaceVariant       = Color(0xFF242730),
    onSurfaceVariant     = Color(0xFFC2C6CB),
    outline              = Color(0xFF3A3F44),
    outlineVariant       = Color(0xFF44474E),
    inverseSurface       = Color(0xFFE0E2E4),
    inverseOnSurface     = Color(0xFF2E3135),
    inversePrimary       = Color(0xFF37474F),
    surfaceTint          = Color(0xFF90A4AE),
    error                = Color(0xFFFFB4AB),
    onError              = Color(0xFF690005),
    errorContainer       = Color(0xFF93000A),
    onErrorContainer     = Color(0xFFFFDAD6),
)

enum class AppTheme { OCEAN, FOREST, LAVENDER, NEUTRAL }

@Composable
fun TossDayTheme(
    appTheme: AppTheme = AppTheme.OCEAN,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.OCEAN    -> if (darkTheme) OceanDark    else OceanLight
        AppTheme.FOREST   -> if (darkTheme) ForestDark   else ForestLight
        AppTheme.LAVENDER -> if (darkTheme) LavenderDark else LavenderLight
        AppTheme.NEUTRAL  -> if (darkTheme) NeutralDark  else NeutralLight
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
