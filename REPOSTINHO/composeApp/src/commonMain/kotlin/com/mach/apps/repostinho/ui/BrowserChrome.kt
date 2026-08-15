package com.mach.apps.repostinho.ui

import androidx.compose.ui.graphics.Color

/**
 * Diz à página qual é a cor da barra do app.
 *
 * Só a web faz alguma coisa aqui. Lá o Compose desenha num canvas, e as faixas de área
 * segura — atrás da barra de status e da barra de gestos — ficam fora dele: quem as pinta
 * é o CSS. Sem este aviso o CSS só teria `prefers-color-scheme` para decidir, ou seja
 * seguiria o tema do SISTEMA, enquanto o app segue o botão de modo noturno da toolbar. Com
 * o aparelho no escuro e o app no claro, a faixa saía azul-noite em cima de uma toolbar
 * azul.
 *
 * No Android e no iOS é no-op de propósito: lá a toolbar desenha por baixo da barra de
 * status, então a cor já é a mesma por construção.
 */
expect fun paintBrowserChrome(color: Color)
