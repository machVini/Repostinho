package com.mach.apps.repostinho

/*
 * Base de fusos horários IANA para o navegador.
 *
 * O `kotlinx-datetime` lê os fusos do sistema no JVM e no Native, mas o navegador não
 * expõe essa base. Sem ela, `TimeZone.of("America/Sao_Paulo")` lança ao construir o
 * ChoreRepository e o app inteiro morre antes da primeira tela.
 *
 * Declarar a dependência npm não basta: ela fica disponível para o yarn, mas nada a
 * importa, e o webpack não inclui módulo que ninguém referencia. Esta declaração externa
 * é o import — e a `val` abaixo existe só para o bundler não remover tudo por eliminação
 * de código morto.
 */
@JsModule("@js-joda/timezone")
external object JsJodaTimeZoneModule : JsAny

private val database: JsAny = JsJodaTimeZoneModule

/** Chamado na abertura para garantir que o módulo seja carregado antes de qualquer data. */
fun loadTimeZoneDatabase(): Boolean = database !== null
