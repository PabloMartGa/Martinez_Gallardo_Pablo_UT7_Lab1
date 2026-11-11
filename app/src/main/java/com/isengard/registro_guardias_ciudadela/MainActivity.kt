package com.isengard.registro_guardias_ciudadela

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * Pantalla única de la app: consola del oficial de guardia.
 * Guarda y restaura los datos del soldado en turno usando SharedPreferences.
 */
class MainActivity : AppCompatActivity() {

    // Constantes: así evitamos escribir mal los nombres en distintos sitios
    companion object {
        // Nombre del archivo físico -> se crea como RegistroGuardia.xml (carpeta privada de la app)
        private const val PREFS_NAME = "RegistroGuardia"

        // Claves del "diccionario" (clave-valor) obligatorias
        private const val KEY_SOLDADO = "KEY_SOLDADO" // String  -> nombre del efectivo
        private const val KEY_RANGO = "KEY_RANGO"     // Int     -> 0 Recluta, 1 Capitán, 2 Veterano
        private const val KEY_ESTADO = "KEY_ESTADO"   // Boolean -> true En Guardia, false En Reposo

        // Valores posibles del rango
        private const val RANGO_RECLUTA = 0
        private const val RANGO_CAPITAN = 1
        private const val RANGO_VETERANO = 2
    }

    // Referencias a los componentes visuales del layout
    private lateinit var etSoldado: EditText
    private lateinit var rgRango: RadioGroup
    private lateinit var swEstado: SwitchMaterial
    private lateinit var btnGuardar: Button

    // Objeto para leer/escribir el archivo RegistroGuardia.xml
    private lateinit var prefs: SharedPreferences

    // onCreate: se ejecuta al crear la Activity. Aquí solo preparamos cosas, NO cargamos datos.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Muestra el layout activity_main.xml

        // Enlazamos cada variable con su componente del layout mediante el id
        etSoldado = findViewById(R.id.etSoldado)
        rgRango = findViewById(R.id.rgRango)
        swEstado = findViewById(R.id.swEstado)
        btnGuardar = findViewById(R.id.btnGuardar)

        // Abrimos (o creamos) el archivo RegistroGuardia.xml en modo privado:
        // solo esta app puede leerlo
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Al pulsar el botón se guardan los datos
        btnGuardar.setOnClickListener { guardarDatos() }
    }

    // onResume: se ejecuta al abrir la app y también al volver a primer plano
    // (tras una llamada, bloqueo de pantalla, etc.). Aquí se restaura el formulario.
    override fun onResume() {
        super.onResume()
        cargarDatos()
    }

    /**
     * Lee los datos guardados en disco y los pinta en el formulario.
     * Es "defensivo": si no hay nada guardado o el dato es raro, usa valores por defecto seguros.
     */
    private fun cargarDatos() {
        // Si la clave no existe devuelve el valor por defecto (el segundo parámetro)
        val soldado = prefs.getString(KEY_SOLDADO, "") ?: ""
        // coerceIn(0, 2) obliga a que el rango sea siempre 0, 1 o 2 (evita datos corruptos)
        val rango = prefs.getInt(KEY_RANGO, RANGO_RECLUTA).coerceIn(RANGO_RECLUTA, RANGO_VETERANO)
        val enGuardia = prefs.getBoolean(KEY_ESTADO, false) // Por defecto: En Reposo

        // Rellenamos la interfaz con los datos leídos
        etSoldado.setText(soldado)
        rgRango.check(rangoAIdRadio(rango)) // Marca el radio que corresponde al rango
        swEstado.isChecked = enGuardia
    }

    /**
     * Valida el formulario, guarda los datos en disco y, solo si todo fue bien,
     * limpia el EditText.
     */
    private fun guardarDatos() {
        // trim() quita espacios sobrantes al principio y al final
        val soldado = etSoldado.text.toString().trim()

        // Control defensivo: el nombre no puede estar vacío
        if (soldado.isEmpty()) {
            etSoldado.error = getString(R.string.error_nombre_vacio) // Mensaje de error en el campo
            return // Salimos sin guardar nada
        }

        // Leemos el resto de valores de la interfaz
        val rango = idRadioARango(rgRango.checkedRadioButtonId)
        val enGuardia = swEstado.isChecked

        // Editor: sirve para modificar el archivo. Primero metemos todos los datos...
        val editor = prefs.edit()
        editor.putString(KEY_SOLDADO, soldado)
        editor.putInt(KEY_RANGO, rango)
        editor.putBoolean(KEY_ESTADO, enGuardia)
        // ...y los confirmamos con apply(): actualiza la memoria al instante
        // y escribe en el disco en segundo plano (asíncrono, no bloquea la pantalla)
        editor.apply()

        // Comprobación defensiva: leemos de vuelta lo que acabamos de guardar
        // y verificamos que coincide con lo que pretendíamos escribir
        val guardadoOk = prefs.getString(KEY_SOLDADO, null) == soldado &&
                prefs.getInt(KEY_RANGO, -1) == rango &&
                prefs.getBoolean(KEY_ESTADO, !enGuardia) == enGuardia

        if (guardadoOk) {
            // Solo ahora, con la transacción procesada, limpiamos el EditText
            etSoldado.text.clear()
            Toast.makeText(this, R.string.msg_guardado, Toast.LENGTH_SHORT).show()
        } else {
            // Si algo falló NO borramos lo escrito, para no perder los datos del soldado
            Toast.makeText(this, R.string.msg_error_guardado, Toast.LENGTH_LONG).show()
        }
    }

    // Convierte el número de rango (0, 1, 2) en el id del RadioButton correspondiente
    private fun rangoAIdRadio(rango: Int): Int = when (rango) {
        RANGO_CAPITAN -> R.id.rbCapitan
        RANGO_VETERANO -> R.id.rbVeterano
        else -> R.id.rbRecluta
    }

    // Convierte el id del RadioButton marcado en el número de rango (0, 1, 2)
    private fun idRadioARango(idRadio: Int): Int = when (idRadio) {
        R.id.rbCapitan -> RANGO_CAPITAN
        R.id.rbVeterano -> RANGO_VETERANO
        else -> RANGO_RECLUTA // Si no hay nada marcado, Recluta por defecto
    }
}