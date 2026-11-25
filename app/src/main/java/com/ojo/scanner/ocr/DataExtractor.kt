package com.ojo.scanner.ocr

/** Tipo de dato reconocido dentro del texto. */
enum class DataType(val label: String) {
    CLAVE_ACCESO("Clave de acceso"),
    CODIGO_BARRAS("Código de barras"),
    RUC("RUC"),
    COMPROBANTE("No. comprobante"),
    EMAIL("Correo"),
    URL("Enlace"),
    CURP("CURP"),
    RFC("RFC"),
    FECHA("Fecha"),
    MONTO("Monto"),
    TELEFONO("Teléfono"),
}

data class DetectedData(val type: DataType, val value: String)

/**
 * Busca datos estructurados (clave de acceso, RUC, número de comprobante, correos,
 * teléfonos, fechas, montos, enlaces...) en el texto reconocido.
 * Es Kotlin puro para poder probarlo sin Android.
 */
object DataExtractor {

    private const val MESES =
        "enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|setiembre|octubre|noviembre|diciembre|" +
            "ene|feb|mar|abr|may|jun|jul|ago|sep|sept|oct|nov|dic"

    // El orden importa: los primeros tienen prioridad cuando dos coincidencias se enciman.
    private val patterns: List<Pair<DataType, Regex>> = listOf(
        DataType.EMAIL to Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}"""),
        DataType.URL to Regex("""(?:https?://|www\.)[^\s,;]+""", RegexOption.IGNORE_CASE),
        // Ecuador (SRI): clave de acceso / número de autorización de 49 dígitos
        DataType.CLAVE_ACCESO to Regex("""(?<!\d)\d{49}(?!\d)"""),
        // Ecuador: RUC de 13 dígitos que termina en 001
        DataType.RUC to Regex("""(?<!\d)\d{10}001(?!\d)"""),
        // Ecuador: número de factura / guía, p. ej. 001-999-000002337
        DataType.COMPROBANTE to Regex("""(?<![\d-])\d{3}-\d{3}-\d{9}(?![\d-])"""),
        DataType.CURP to Regex("""\b[A-Z][AEIOUX][A-Z]{2}\d{6}[HM][A-Z]{5}[A-Z0-9]\d\b"""),
        DataType.RFC to Regex("""\b[A-ZÑ&]{3,4}\d{6}[A-Z0-9]{3}\b"""),
        // Fecha y hora ISO: 2026-11-24T07:50:43-05:00
        DataType.FECHA to Regex("""\b\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2})?(?:[+-]\d{2}:\d{2}|Z)?"""),
        DataType.FECHA to Regex(
            """\b\d{1,2}\s+(?:de\s+)?(?:$MESES)\.?\s+(?:de(?:l)?\s+)?\d{4}\b""",
            RegexOption.IGNORE_CASE,
        ),
        DataType.FECHA to Regex("""\b\d{4}-\d{1,2}-\d{1,2}\b"""),
        DataType.FECHA to Regex("""\b\d{1,2}[/.-]\d{1,2}[/.-](?:\d{4}|\d{2})\b"""),
        DataType.MONTO to Regex("""\$\s?\d{1,3}(?:[,.]\d{3})*(?:[.,]\d{1,2})?|\$\s?\d+(?:[.,]\d{1,2})?"""),
        DataType.MONTO to Regex(
            """\b\d{1,3}(?:[,.]\d{3})*(?:[.,]\d{2})?\s?(?:MXN|USD|EUR|pesos|dólares|euros)\b""",
            RegexOption.IGNORE_CASE,
        ),
        // No empieza a mitad de otro número o fecha (p. ej. el año de "15/03/2024").
        DataType.TELEFONO to Regex("""(?<![\d/.,-])(?:\+\d{1,3}[\s.-]?)?(?:\(\d{2,3}\)[\s.-]?)?\d{2,4}(?:[\s.-]?\d{2,4}){1,3}(?!\d)"""),
    )

    /**
     * @param text texto reconocido por OCR.
     * @param barcodes valores leídos de códigos de barras / QR (son más confiables que el OCR,
     *   por eso van primero).
     */
    fun extract(text: String, barcodes: List<String> = emptyList()): List<DetectedData> {
        val fromBarcodes = barcodes.filter { it.isNotBlank() }.map { raw ->
            val value = raw.trim()
            val type = if (value.length == 49 && value.all { it.isDigit() }) DataType.CLAVE_ACCESO else DataType.CODIGO_BARRAS
            DetectedData(type, value)
        }
        return (fromBarcodes + extractFromText(text)).distinctBy { it.value }
    }

    private fun extractFromText(text: String): List<DetectedData> {
        val taken = mutableListOf<IntRange>()
        val found = mutableListOf<Pair<Int, DetectedData>>()

        for ((type, regex) in patterns) {
            for (match in regex.findAll(text)) {
                val range = match.range
                if (taken.any { it.first <= range.last && range.first <= it.last }) continue

                val value = cleanValue(type, match.value)
                if (!isValid(type, value)) continue

                taken += range
                found += range.first to DetectedData(type, value)
            }
        }

        return found
            .sortedBy { it.first }
            .map { it.second }
            .distinct()
    }

    private fun cleanValue(type: DataType, raw: String): String {
        val trimmed = raw.trim()
        return when (type) {
            DataType.URL -> trimmed.trimEnd('.', ')', ']')
            DataType.EMAIL -> trimmed.trimEnd('.')
            else -> trimmed
        }
    }

    private fun isValid(type: DataType, value: String): Boolean = when (type) {
        // Un teléfono tiene entre 8 y 13 dígitos (con lada o código de país).
        DataType.TELEFONO -> value.count { it.isDigit() } in 8..13
        else -> value.isNotBlank()
    }
}
