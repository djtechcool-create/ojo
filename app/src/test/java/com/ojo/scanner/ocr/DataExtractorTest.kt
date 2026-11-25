package com.ojo.scanner.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataExtractorTest {

    private fun valuesOf(text: String, type: DataType) =
        DataExtractor.extract(text).filter { it.type == type }.map { it.value }

    @Test
    fun detectaCorreo() {
        assertEquals(listOf("juan.perez@correo.com"), valuesOf("Contacto: juan.perez@correo.com.", DataType.EMAIL))
    }

    @Test
    fun detectaEnlace() {
        assertEquals(listOf("https://ejemplo.mx/pago"), valuesOf("Paga en https://ejemplo.mx/pago.", DataType.URL))
        assertEquals(listOf("www.sat.gob.mx"), valuesOf("Visita www.sat.gob.mx", DataType.URL))
    }

    @Test
    fun detectaCurp() {
        assertEquals(listOf("GOMC800101HDFRRL09"), valuesOf("CURP: GOMC800101HDFRRL09", DataType.CURP))
    }

    @Test
    fun detectaRfcSinConfundirConCurp() {
        val text = "RFC GOMC800101AB1 CURP GOMC800101HDFRRL09"
        assertEquals(listOf("GOMC800101AB1"), valuesOf(text, DataType.RFC))
        assertEquals(listOf("GOMC800101HDFRRL09"), valuesOf(text, DataType.CURP))
    }

    @Test
    fun detectaFechas() {
        val text = "Emitido el 15/03/2024, vence 2024-04-30 y se firmó el 3 de marzo de 2024"
        assertEquals(
            listOf("15/03/2024", "2024-04-30", "3 de marzo de 2024"),
            valuesOf(text, DataType.FECHA),
        )
    }

    @Test
    fun detectaTelefonos() {
        val text = "Tel. 55 1234 5678 o +52 (33) 3123-4567"
        assertEquals(listOf("55 1234 5678", "+52 (33) 3123-4567"), valuesOf(text, DataType.TELEFONO))
    }

    @Test
    fun detectaMontos() {
        val text = "Total: $1,250.50 y abono de 300.00 MXN"
        assertEquals(listOf("$1,250.50", "300.00 MXN"), valuesOf(text, DataType.MONTO))
    }

    @Test
    fun noConfundeFechaNiCodigoPostalConTelefono() {
        val text = "Fecha 15/03/2024 CP 44100"
        assertTrue(valuesOf(text, DataType.TELEFONO).isEmpty())
    }

    @Test
    fun respetaElOrdenDelTexto() {
        val tipos = DataExtractor.extract("a@b.com 15/03/2024 55 1234 5678").map { it.type }
        assertEquals(listOf(DataType.EMAIL, DataType.FECHA, DataType.TELEFONO), tipos)
    }

    // Texto tal como sale de la guía de remisión de Ecuador
    private val guia = """
        RUC: 1793191092001
        GUIA DE REMISÓN
        No. 001-999-000002337
        Número de Autorización
        2411202606179319109200120019990000023370000232914
        Fecha y Hora de Autorización:
        2026-11-24T07:50:43-05:00
        Fecha Fin Transporte: 24/11/2026
        EXTRUIDO BOLITA CEREAL SABOR POLLO A LA BRASA 25GR SICAE2024-DEL46-26
    """.trimIndent()

    @Test
    fun leeGuiaDeRemisionDeEcuador() {
        val datos = DataExtractor.extract(guia)
        assertEquals(
            listOf(
                DetectedData(DataType.RUC, "1793191092001"),
                DetectedData(DataType.COMPROBANTE, "001-999-000002337"),
                DetectedData(DataType.CLAVE_ACCESO, "2411202606179319109200120019990000023370000232914"),
                DetectedData(DataType.FECHA, "2026-11-24T07:50:43-05:00"),
                DetectedData(DataType.FECHA, "24/11/2026"),
            ),
            datos,
        )
    }

    @Test
    fun codigoDeBarrasVaPrimeroYNoSeRepite() {
        val clave = "2411202606179319109200120019990000023370000232914"
        val datos = DataExtractor.extract(guia, barcodes = listOf(clave))
        assertEquals(DetectedData(DataType.CLAVE_ACCESO, clave), datos.first())
        assertEquals(1, datos.count { it.value == clave })
    }

    @Test
    fun codigoDeBarrasGenerico() {
        val datos = DataExtractor.extract("", barcodes = listOf("7501234567890"))
        assertEquals(listOf(DetectedData(DataType.CODIGO_BARRAS, "7501234567890")), datos)
    }
}
