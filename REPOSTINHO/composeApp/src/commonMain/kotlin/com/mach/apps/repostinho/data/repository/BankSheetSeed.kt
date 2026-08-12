package com.mach.apps.repostinho.data.repository

import com.mach.apps.repostinho.data.model.CaixinhaLine
import com.mach.apps.repostinho.data.model.MemberBalance
import com.mach.apps.repostinho.data.model.Movement
import com.mach.apps.repostinho.data.model.TransactionType

/*
 * Retrato da planilha "Banco atual.xlsm" (abas Movimentações, Saldos_pessoas e
 * Saldos_caixinha), tirado em 11/08/2026.
 *
 * Arquivo gerado a partir da planilha, não escrito à mão: são 25 colunas de pesos por
 * lançamento, e transcrever isso manualmente erraria em silêncio. Enquanto a fonte ao vivo
 * não existe, atualizar significa regerar este arquivo.
 */
internal object BankSheetSeed {

    val BALANCES: List<MemberBalance> = listOf(
        MemberBalance("Lameu", -173590L, 75523L, 232712L, -16401L, isFormer = false),
        MemberBalance("Leozin", 20342L, 106379L, 31989L, -54048L, isFormer = false),
        MemberBalance("Pico", 196507L, 103601L, 48100L, 141007L, isFormer = false),
        MemberBalance("LL", -24159L, 94991L, 98710L, -20440L, isFormer = false),
        MemberBalance("Du", -243101L, 83148L, 154816L, -171433L, isFormer = false),
        MemberBalance("Michel", -187754L, 75795L, 75076L, -188473L, isFormer = false),
        MemberBalance("Peter", -79939L, 75611L, 80000L, -75550L, isFormer = false),
        MemberBalance("Gab", 134076L, 78673L, 5490L, 60893L, isFormer = false),
        MemberBalance("Anaju", -1583L, 1317L, 0L, -2900L, isFormer = false),
        MemberBalance("Gu", -81347L, 83144L, 162929L, -1561L, isFormer = false),
        MemberBalance("Benê", 7833L, 0L, 0L, 7833L, isFormer = false),
        MemberBalance("Picasso", -115909L, 69622L, 40000L, -145530L, isFormer = false),
        MemberBalance("Mixirica", -5200L, 70139L, 0L, -75339L, isFormer = false),
        MemberBalance("Massa", -87238L, 64908L, 152200L, 54L, isFormer = false),
        MemberBalance("VK", -31269L, 76979L, 66788L, -41460L, isFormer = false),
        MemberBalance("Prazer", -66987L, 67791L, 66987L, -67791L, isFormer = false),
        MemberBalance("Cansado", -114998L, 70511L, 103900L, -81609L, isFormer = false),
        MemberBalance("Lucas", -347965L, 0L, 0L, -347965L, isFormer = true),
        MemberBalance("Anhê", -1583L, 1317L, 0L, -2900L, isFormer = true),
        MemberBalance("Nicole", -12068L, 2268L, 0L, -14336L, isFormer = true),
        MemberBalance("Gui", -7009L, 0L, 0L, -7009L, isFormer = true),
        MemberBalance("Key", -1491L, 0L, 0L, -1491L, isFormer = true),
        MemberBalance("Raquel", -538656L, 0L, 0L, -538656L, isFormer = true),
        MemberBalance("Carlos", -160992L, 1198L, 0L, -162190L, isFormer = true),
        MemberBalance("Helena", -15445L, 0L, 10490L, -4955L, isFormer = true),
    )

    val CAIXINHA: List<CaixinhaLine> = listOf(
        CaixinhaLine("Faturas não pagas", 0L, 0L, 0L, isTotal = false),
        CaixinhaLine("Dinheiro físico", 10000L, 0L, 10000L, isTotal = false),
        CaixinhaLine("Saldo da Nubank", -372126L, 128667L, -243459L, isTotal = false),
        CaixinhaLine("Aluguel lançado não pago", -704888L, 0L, -704888L, isTotal = false),
        CaixinhaLine("A receber das pessoas", 1939525L, -127273L, 1812252L, isTotal = false),
        CaixinhaLine("Outros", 7102L, 0L, 7102L, isTotal = false),
        CaixinhaLine("Total (saldo real)", 879613L, 1394L, 881007L, isTotal = true),
    )

    val MOVEMENTS: List<Movement> = listOf(
        Movement(
            id = "row2",
            description = "Aluguel junho pago em 18/07",
            type = TransactionType.COLETIVO,
            payer = "Caix. Déb/PIX",
            valueCents = 704888L,
            weights = mapOf("Lameu" to 0.8, "Leozin" to 1.1, "Pico" to 1.1, "LL" to 1.1, "Du" to 1.0, "Michel" to 1.0, "Peter" to 1.0, "Gab" to 0.95, "Gu" to 0.95, "Mixirica" to 0.88, "Massa" to 0.88, "VK" to 1.0, "Cansado" to 0.88, "Prazer" to 0.8, "Picasso" to 0.8),
            totalWeight = 14.240000000000002
        ),

        Movement(
            id = "291",
            description = "Aluguel prazer 13/07",
            type = TransactionType.ENTRADA,
            payer = "Prazer",
            valueCents = 66987L,
            weights = emptyMap(),
            totalWeight = 0.0
        ),

        Movement(
            id = "292",
            description = "Aluguel Labubu (13/07)",
            type = TransactionType.ENTRADA,
            payer = "Lameu",
            valueCents = 170000L,
            weights = emptyMap(),
            totalWeight = 0.0
        ),

        Movement(
            id = "293",
            description = "Aluguel VK 13/07",
            type = TransactionType.ENTRADA,
            payer = "VK",
            valueCents = 31269L,
            weights = emptyMap(),
            totalWeight = 0.0
        ),

        Movement(
            id = "294",
            description = "Aluguel",
            type = TransactionType.ENTRADA,
            payer = "Massa",
            valueCents = 87300L,
            weights = emptyMap(),
            totalWeight = 0.0
        ),

        Movement(
            id = "295",
            description = "Mensalidade ARU Julho",
            type = TransactionType.COLETIVO,
            payer = "Du",
            valueCents = 4500L,
            weights = mapOf("Lameu" to 1.0, "Leozin" to 1.0, "Pico" to 1.0, "LL" to 1.0, "Du" to 1.0, "Michel" to 1.0, "Peter" to 1.0, "Gab" to 1.0, "Gu" to 1.0, "Mixirica" to 1.0, "Massa" to 1.0, "VK" to 1.0, "Cansado" to 1.0, "Prazer" to 1.0, "Picasso" to 1.0),
            totalWeight = 15.0
        ),

        Movement(
            id = "296",
            description = "Aluguel piter",
            type = TransactionType.ENTRADA,
            payer = "Peter",
            valueCents = 80000L,
            weights = emptyMap(),
            totalWeight = 0.0
        ),

        Movement(
            id = "297",
            description = "Pf",
            type = TransactionType.PRIVADO,
            payer = "LL",
            valueCents = 9893L,
            weights = mapOf("Pico" to 1.0),
            totalWeight = 1.0
        ),

        Movement(
            id = "298",
            description = "aluguel M",
            type = TransactionType.ENTRADA,
            payer = "Michel",
            valueCents = 55076L,
            weights = emptyMap(),
            totalWeight = 0.0
        ),

        Movement(
            id = "299",
            description = "Netflix (Julho)",
            type = TransactionType.PRIVADO,
            payer = "Leozin",
            valueCents = 5990L,
            weights = mapOf("Lameu" to 1.0, "Leozin" to 1.0, "LL" to 2.0, "Carlos" to 1.0),
            totalWeight = 5.0
        ),

        Movement(
            id = "300",
            description = "MEGA LIXO (14/07)",
            type = TransactionType.PRIVADO,
            payer = "Leozin",
            valueCents = 8900L,
            weights = mapOf("Lameu" to 1.0, "Leozin" to 7.0, "Peter" to 1.0, "Gab" to 5.0, "Picasso" to 5.0),
            totalWeight = 19.0
        ),

        Movement(
            id = "301",
            description = "Strogonoff",
            type = TransactionType.PRIVADO,
            payer = "Leozin",
            valueCents = 9509L,
            weights = mapOf("Lameu" to 1.0, "Leozin" to 2.0, "Pico" to 1.0, "LL" to 1.0, "Gab" to 1.0, "Mixirica" to 1.0, "Massa" to 1.0, "Picasso" to 1.0, "Nicole" to 1.0),
            totalWeight = 10.0
        ),

        Movement(
            id = "302",
            description = "Refri (14/07)",
            type = TransactionType.PRIVADO,
            payer = "Leozin",
            valueCents = 2200L,
            weights = mapOf("Leozin" to 1.0, "Picasso" to 1.0),
            totalWeight = 2.0
        ),

        Movement(
            id = "303",
            description = "Projeto saúde",
            type = TransactionType.PRIVADO,
            payer = "Pico",
            valueCents = 30000L,
            weights = mapOf("Leozin" to 1.0, "LL" to 1.0, "Du" to 1.0),
            totalWeight = 3.0
        ),

        Movement(
            id = "304",
            description = "Refris e Pães Churrasco (Aniverssário da Rep)",
            type = TransactionType.PRIVADO,
            payer = "VK",
            valueCents = 13371L,
            weights = mapOf("Lameu" to 1.0, "Leozin" to 2.0, "Pico" to 2.0, "LL" to 1.0, "Du" to 1.0, "Michel" to 1.0, "Peter" to 1.0, "Gab" to 1.0, "Gu" to 1.0, "Mixirica" to 1.0, "VK" to 1.0, "Cansado" to 1.0, "Prazer" to 1.0, "Picasso" to 1.0),
            totalWeight = 16.0
        ),

        Movement(
            id = "305",
            description = "Aluguel Cansado",
            type = TransactionType.ENTRADA,
            payer = "Cansado",
            valueCents = 103900L,
            weights = emptyMap(),
            totalWeight = 0.0
        ),

        Movement(
            id = "306",
            description = "Marmita pague menos",
            type = TransactionType.PRIVADO,
            payer = "VK",
            valueCents = 2250L,
            weights = mapOf("Cansado" to 1.0),
            totalWeight = 1.0
        ),

        Movement(
            id = "307",
            description = "Sorvete oxxo",
            type = TransactionType.PRIVADO,
            payer = "Pico",
            valueCents = 7900L,
            weights = mapOf("Leozin" to 2.0, "Pico" to 1.0, "Anaju" to 1.0, "Anhê" to 1.0, "Nicole" to 1.0),
            totalWeight = 6.0
        ),

        Movement(
            id = "308",
            description = "aluguel gu",
            type = TransactionType.ENTRADA,
            payer = "Gu",
            valueCents = 78200L,
            weights = emptyMap(),
            totalWeight = 0.0
        ),

        Movement(
            id = "309",
            description = "YouTube Premium Agosto",
            type = TransactionType.PRIVADO,
            payer = "Leozin",
            valueCents = 5390L,
            weights = mapOf("Leozin" to 1.0, "Pico" to 1.0, "LL" to 1.0, "Du" to 1.0, "Gab" to 1.0, "Massa" to 1.0),
            totalWeight = 6.0
        ),

        Movement(
            id = "row22",
            description = "Carne Churrasco (Aniverssário da Rep)",
            type = TransactionType.PRIVADO,
            payer = "Lameu",
            valueCents = 19090L,
            weights = mapOf("Lameu" to 1.0, "Leozin" to 1.0, "Pico" to 2.0, "LL" to 1.0, "Michel" to 1.0, "Gab" to 1.0, "Gu" to 1.0, "Mixirica" to 1.0, "VK" to 1.0, "Cansado" to 1.0, "Prazer" to 1.0, "Picasso" to 1.0),
            totalWeight = 13.0
        ),

        Movement(
            id = "row23",
            description = "Pagmenos Churrasco (Aniverssário da Rep)",
            type = TransactionType.PRIVADO,
            payer = "Lameu",
            valueCents = 34643L,
            weights = mapOf("Lameu" to 1.0, "Leozin" to 2.0, "Pico" to 2.0, "LL" to 1.0, "Du" to 1.0, "Michel" to 1.0, "Peter" to 1.0, "Gab" to 1.0, "Gu" to 1.0, "Mixirica" to 1.0, "VK" to 1.0, "Cansado" to 1.0, "Prazer" to 1.0, "Picasso" to 1.0),
            totalWeight = 16.0
        ),

        Movement(
            id = "row24",
            description = "Carvão Churrasco (Aniverssário da Rep)",
            type = TransactionType.PRIVADO,
            payer = "Lameu",
            valueCents = 3999L,
            weights = mapOf("Lameu" to 1.0, "Leozin" to 2.0, "Pico" to 2.0, "LL" to 1.0, "Du" to 1.0, "Michel" to 1.0, "Peter" to 1.0, "Gab" to 1.0, "Gu" to 1.0, "Mixirica" to 1.0, "VK" to 1.0, "Cansado" to 1.0, "Prazer" to 1.0, "Picasso" to 1.0),
            totalWeight = 16.0
        ),

        Movement(
            id = "row25",
            description = "Fita dupla face e tela para churrasqueira",
            type = TransactionType.COLETIVO,
            payer = "Lameu",
            valueCents = 3500L,
            weights = mapOf("Lameu" to 1.0, "Leozin" to 1.0, "Pico" to 1.0, "LL" to 1.0, "Du" to 1.0, "Michel" to 1.0, "Peter" to 1.0, "Gab" to 1.0, "Gu" to 1.0, "Mixirica" to 1.0, "Massa" to 1.0, "VK" to 1.0, "Cansado" to 1.0, "Prazer" to 1.0, "Picasso" to 1.0),
            totalWeight = 15.0
        ),

        Movement(
            id = "row26",
            description = "Impressões do mural de moradores",
            type = TransactionType.COLETIVO,
            payer = "Lameu",
            valueCents = 1480L,
            weights = mapOf("Lameu" to 1.0, "Leozin" to 1.0, "Pico" to 1.0, "LL" to 1.0, "Du" to 1.0, "Michel" to 1.0, "Peter" to 1.0, "Gab" to 1.0, "Gu" to 1.0, "Mixirica" to 1.0, "Massa" to 1.0, "VK" to 1.0, "Cansado" to 1.0, "Prazer" to 1.0, "Picasso" to 1.0),
            totalWeight = 15.0
        ),

        Movement(
            id = "row27",
            description = "Água com vencimento em 07/08",
            type = TransactionType.COLETIVO,
            payer = "Caix. Déb/PIX",
            valueCents = 161484L,
            weights = mapOf("Lameu" to 1.0, "Leozin" to 1.0, "Pico" to 1.0, "LL" to 1.0, "Du" to 1.0, "Michel" to 1.0, "Peter" to 1.0, "Gab" to 1.0, "Gu" to 1.0, "Mixirica" to 1.0, "Massa" to 1.0, "VK" to 1.0, "Cansado" to 1.0, "Prazer" to 1.0, "Picasso" to 1.0),
            totalWeight = 15.0
        ),

        Movement(
            id = "row28",
            description = "Cerveja Alcorridas (Aniverssário da Rep)",
            type = TransactionType.PRIVADO,
            payer = "Pico",
            valueCents = 10200L,
            weights = mapOf("Lameu" to 1.0, "Pico" to 2.0, "LL" to 1.0, "Michel" to 1.0, "Peter" to 1.0, "Gab" to 1.0, "Gu" to 1.0, "Mixirica" to 1.0, "VK" to 1.0, "Picasso" to 1.0),
            totalWeight = 11.0
        ),

        Movement(
            id = "row29",
            description = "CPFL Julho",
            type = TransactionType.COLETIVO,
            payer = "Du",
            valueCents = 87249L,
            weights = mapOf("Lameu" to 1.0, "Leozin" to 1.0, "Pico" to 1.0, "LL" to 1.0, "Du" to 1.0, "Michel" to 1.0, "Peter" to 1.0, "Gab" to 1.0, "Gu" to 1.0, "Mixirica" to 1.0, "Massa" to 1.0, "VK" to 1.0, "Cansado" to 1.0, "Prazer" to 1.0, "Picasso" to 1.0),
            totalWeight = 15.0
        ),

        Movement(
            id = "row30",
            description = "Correção energia Julho",
            type = TransactionType.COLETIVO,
            payer = "Caix. Déb/PIX",
            valueCents = -65000L,
            weights = mapOf("Lameu" to 1.0, "Leozin" to 1.0, "Pico" to 1.0, "LL" to 1.0, "Du" to 1.0, "Michel" to 1.0, "Peter" to 1.0, "Gab" to 1.0, "Gu" to 1.0, "Mixirica" to 1.0, "Massa" to 1.0, "VK" to 1.0, "Cansado" to 1.0, "Prazer" to 1.0, "Picasso" to 1.0),
            totalWeight = 15.0
        ),

        Movement(
            id = "row31",
            description = "Aluguel Picasso",
            type = TransactionType.ENTRADA,
            payer = "Picasso",
            valueCents = 40000L,
            weights = emptyMap(),
            totalWeight = 0.0
        ),

        Movement(
            id = "row32",
            description = "Rendimentos Conta Mercado Pago",
            type = TransactionType.ENTRADA,
            payer = "Ext. (PIX)",
            valueCents = 1394L,
            weights = emptyMap(),
            totalWeight = 0.0
        ),

        Movement(
            id = "row33",
            description = "Camiseta Repostinho 2026",
            type = TransactionType.PRIVADO,
            payer = "Helena",
            valueCents = 10490L,
            weights = mapOf("Gu" to 1.0),
            totalWeight = 1.0
        ),

        Movement(
            id = "row34",
            description = "CPFL Agosto",
            type = TransactionType.COLETIVO,
            payer = "Du",
            valueCents = 63067L,
            weights = mapOf("Lameu" to 1.0, "Leozin" to 1.0, "Pico" to 1.0, "LL" to 1.0, "Du" to 1.0, "Michel" to 1.0, "Peter" to 1.0, "Gab" to 1.0, "Gu" to 1.0, "Mixirica" to 1.0, "Massa" to 1.0, "VK" to 1.0, "Cansado" to 1.0, "Prazer" to 1.0, "Picasso" to 1.0),
            totalWeight = 15.0
        ),

        Movement(
            id = "310",
            description = "Ragu",
            type = TransactionType.PRIVADO,
            payer = "LL",
            valueCents = 1299L,
            weights = mapOf("Peter" to 1.0),
            totalWeight = 1.0
        ),

        Movement(
            id = "311",
            description = "Pagmenos 31/07",
            type = TransactionType.PRIVADO,
            payer = "LL",
            valueCents = 1828L,
            weights = mapOf("Gab" to 1.0),
            totalWeight = 1.0
        ),

        Movement(
            id = "312",
            description = "Shampoo",
            type = TransactionType.PRIVADO,
            payer = "LL",
            valueCents = 2990L,
            weights = mapOf("Lameu" to 1.0),
            totalWeight = 1.0
        ),

        Movement(
            id = "313",
            description = "subway",
            type = TransactionType.PRIVADO,
            payer = "LL",
            valueCents = 1720L,
            weights = mapOf("Lameu" to 1.0),
            totalWeight = 1.0
        ),

        Movement(
            id = "314",
            description = "Churrasco (Carvao e farofa)",
            type = TransactionType.PRIVADO,
            payer = "LL",
            valueCents = 5796L,
            weights = mapOf("Lameu" to 1.0, "Leozin" to 1.0, "Pico" to 1.0, "LL" to 1.0, "Michel" to 1.0, "Gab" to 1.0, "Gu" to 1.0, "Mixirica" to 1.0, "VK" to 1.0, "Cansado" to 1.0, "Prazer" to 1.0, "Picasso" to 1.0),
            totalWeight = 12.0
        ),

        Movement(
            id = "315",
            description = "Aluguel agosto",
            type = TransactionType.ENTRADA,
            payer = "LL",
            valueCents = 75184L,
            weights = emptyMap(),
            totalWeight = 0.0
        ),

        Movement(
            id = "316",
            description = "sinuca agosto",
            type = TransactionType.COLETIVO,
            payer = "Michel",
            valueCents = 20000L,
            weights = mapOf("Lameu" to 1.0, "Leozin" to 1.0, "Pico" to 1.0, "LL" to 1.0, "Du" to 1.0, "Michel" to 1.0, "Peter" to 1.0, "Gab" to 1.0, "Gu" to 1.0, "Mixirica" to 1.0, "Massa" to 1.0, "VK" to 1.0, "Cansado" to 1.0, "Prazer" to 1.0, "Picasso" to 1.0),
            totalWeight = 15.0
        ),

        Movement(
            id = "row42",
            description = "Internet Agosto",
            type = TransactionType.COLETIVO,
            payer = "VK",
            valueCents = 16198L,
            weights = mapOf("Lameu" to 1.0, "Leozin" to 1.0, "Pico" to 1.0, "LL" to 1.0, "Du" to 1.0, "Michel" to 1.0, "Peter" to 1.0, "Gab" to 1.0, "Gu" to 1.0, "Mixirica" to 1.0, "Massa" to 1.0, "VK" to 1.0, "Cansado" to 1.0, "Prazer" to 1.0, "Picasso" to 1.0),
            totalWeight = 15.0
        ),

        Movement(
            id = "317",
            description = "Spotify",
            type = TransactionType.PRIVADO,
            payer = "Gab",
            valueCents = 2000L,
            weights = mapOf("LL" to 1.0, "Michel" to 1.0, "Peter" to 1.0),
            totalWeight = 3.0
        ),

        Movement(
            id = "318",
            description = "Camiseta rep 2/3",
            type = TransactionType.PRIVADO,
            payer = "Gab",
            valueCents = 3490L,
            weights = mapOf("Prazer" to 1.0),
            totalWeight = 1.0
        ),

        Movement(
            id = "row45",
            description = "Aluguel Major",
            type = TransactionType.ENTRADA,
            payer = "Gu",
            valueCents = 84729L,
            weights = emptyMap(),
            totalWeight = 0.0
        ),

        Movement(
            id = "row46",
            description = "Mega lixo (09/07)",
            type = TransactionType.PRIVADO,
            payer = "Caix. Déb/PIX",
            valueCents = 8900L,
            weights = mapOf("Lameu" to 1.0, "Leozin" to 1.0, "Pico" to 1.0),
            totalWeight = 3.0
        ),

        Movement(
            id = "319",
            description = "Batata bronco 11/08",
            type = TransactionType.PRIVADO,
            payer = "VK",
            valueCents = 3700L,
            weights = mapOf("Pico" to 1.0, "VK" to 1.0),
            totalWeight = 2.0
        ),

        Movement(
            id = "320",
            description = "Aluguel",
            type = TransactionType.ENTRADA,
            payer = "Massa",
            valueCents = 64900L,
            weights = emptyMap(),
            totalWeight = 0.0
        ),

    )
}
