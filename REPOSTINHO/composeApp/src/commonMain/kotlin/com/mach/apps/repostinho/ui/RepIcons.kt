package com.mach.apps.repostinho.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Ícones da barra inferior desenhados aqui em vez de vir da material-icons-extended:
 * são quatro, e a dependência pesa alguns MB no APK só por causa deles.
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
}
