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
}
