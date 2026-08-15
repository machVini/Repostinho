# Regras do R8 para o build de release.
#
# A maior parte das bibliotecas do app (Compose, Ktor, OkHttp, Coil, Firebase, corrotinas)
# publica as próprias regras junto do artefato, e o R8 as aplica sozinho. O que sobra para
# cá é o que depende de código nosso.

# kotlinx.serialization resolve o serializador por reflexão, procurando um `Companion` e uma
# classe `$$serializer` gerada ao lado de cada `@Serializable`. Nomes ofuscados quebram essa
# busca em tempo de execução — e o sintoma é uma exceção só quando a tela que usa o modelo
# abre, não no build. Bloco recomendado pela própria biblioteca, restrito ao nosso pacote.
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.mach.apps.repostinho.**$$serializer { *; }
-keepclassmembers class com.mach.apps.repostinho.** {
    *** Companion;
    <fields>;
}
-keepclasseswithmembers class com.mach.apps.repostinho.** {
    kotlinx.serialization.KSerializer serializer(...);
}
