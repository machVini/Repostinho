package com.mach.apps.repostinho.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Ícones das barras desenhados aqui em vez de vir da material-icons-extended:
 * são poucos, e a dependência pesa alguns MB no APK só por causa deles.
 */
private fun icon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(pathData).toNodes(),
            fill = SolidColor(Color.Black)
        )
    }.build()

object RepIcons {

    val Home: ImageVector by lazy {
        icon("Home", "M10,20v-6h4v6h5v-8h3L12,3 2,12h3v8z")
    }

    val Bank: ImageVector by lazy {
        icon(
            "Bank",
            "M21,18v1c0,1.1 -0.9,2 -2,2H5c-1.11,0 -2,-0.9 -2,-2V5c0,-1.1 0.89,-2 2,-2h14" +
                "c1.1,0 2,0.9 2,2v1h-9c-1.11,0 -2,0.9 -2,2v8c0,1.1 0.89,2 2,2h9zM12,16h10V8H12v8z" +
                "M16,13.5c-0.83,0 -1.5,-0.67 -1.5,-1.5s0.67,-1.5 1.5,-1.5 1.5,0.67 1.5,1.5" +
                " -0.67,1.5 -1.5,1.5z"
        )
    }

    val Tasks: ImageVector by lazy {
        icon(
            "Tasks",
            "M3,5h2v2H3V5zM3,11h2v2H3v-2zM3,17h2v2H3v-2zM7,5h14v2H7V5zM7,11h14v2H7v-2z" +
                "M7,17h14v2H7v-2z"
        )
    }

    val Person: ImageVector by lazy {
        icon(
            "Person",
            "M12,12c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4 -4,1.79 -4,4 1.79,4 4,4z" +
                "M12,14c-2.67,0 -8,1.34 -8,4v2h16v-2c0,-2.66 -5.33,-4 -8,-4z"
        )
    }

    val Calendar: ImageVector by lazy {
        icon(
            "Calendar",
            "M17,12h-5v5h5v-5zM16,1v2H8V1H6v2H5c-1.11,0 -1.99,0.9 -1.99,2L3,19" +
                "c0,1.1 0.89,2 2,2h14c1.1,0 2,-0.9 2,-2V5c0,-1.1 -0.9,-2 -2,-2h-1V1h-2z" +
                "M19,19H5V8h14v11z"
        )
    }

    val Back: ImageVector by lazy {
        icon("Back", "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z")
    }

    /** Lua: aparece no tema claro, como convite para escurecer. */
    val DarkMode: ImageVector by lazy {
        icon(
            "DarkMode",
            "M12,3c-4.97,0 -9,4.03 -9,9s4.03,9 9,9 9,-4.03 9,-9c0,-0.46 -0.04,-0.92 -0.1,-1.36" +
                " -0.98,1.37 -2.58,2.26 -4.4,2.26 -2.98,0 -5.4,-2.42 -5.4,-5.4" +
                " 0,-1.81 0.89,-3.42 2.26,-4.4 -0.44,-0.06 -0.9,-0.1 -1.36,-0.1z"
        )
    }

    /** Sol: aparece no tema escuro, como convite para clarear. */
    val LightMode: ImageVector by lazy {
        icon(
            "LightMode",
            "M6.76,4.84l-1.8,-1.79 -1.41,1.41 1.79,1.79 1.42,-1.41zM4,10.5H1v2h3v-2z" +
                "M11,0.55v2.91h2V0.55h-2zM20.45,4.46l-1.41,-1.41 -1.79,1.79 1.41,1.41 1.79,-1.79z" +
                "M17.24,18.16l1.79,1.8 1.41,-1.41 -1.8,-1.79 -1.4,1.4zM20,10.5v2h3v-2h-3z" +
                "M12,5.5c-3.31,0 -6,2.69 -6,6s2.69,6 6,6 6,-2.69 6,-6 -2.69,-6 -6,-6z" +
                "M11,22.45h2v-2.9h-2v2.9zM3.55,18.54l1.41,1.41 1.79,-1.8 -1.41,-1.41 -1.79,1.8z"
        )
    }
}
