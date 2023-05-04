package com.example.futbol

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

// Variables para almacenar la anchura y altura de la pantalla
private var auxAnchura: Int? = null
private var auxAltura: Int? = null
// Variables para el marcador del juego
private var contUno: Int = 0
private var contDos: Int = 0

// Actividad principal de la aplicación
@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private var sA: Sensor? = null
    private var sB: Sensor? = null
    private lateinit var sensorManager: SensorManager
    lateinit var viewDibujo: ProcessClass

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ocultar la barra de navegación
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN

        // Ocultar la barra de título
        supportActionBar?.hide()

        // Establecer la vista de la actividad en pantalla completa
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        // Establecer el contenido de la actividad
        // Obtener la altura y anchura de la pantalla del dispositivo
        val metricDsp = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metricDsp)
        auxAltura = metricDsp.heightPixels
        auxAnchura = metricDsp.widthPixels

        // Crear una instancia de la clase ProcessClass
        viewDibujo = ProcessClass(this)
        setContentView(viewDibujo)

        // Obtener el servicio de sensores
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Detectar si el dispositivo tiene un sensor de gravedad y usarlo si está disponible
        if (sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null) {
            val gravSensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_GRAVITY)
            // Usar la versión 3 del sensor de gravedad de Google
            sB =
                gravSensors.firstOrNull { it.vendor.contains("Google LLC") && it.version == 3 }
        }
        // Si el sensor de gravedad no está disponible, usar el acelerómetro
        if (sB == null) {
            // Use the accelerometer.
            sB = if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            } else {
                null
            }
        }
        // Usar el acelerómetro para el sensor A
        sA = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onResume() {
        // Registrar el sensor A con el sensorEventListener
        super.onResume()
        sA?.also {
            sensorManager.registerListener(
                viewDibujo, it, SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    override fun onPause() {
        // Pausar la detección del sensor al pausar la actividad
        super.onPause()
        sensorManager.unregisterListener(viewDibujo)
    }


    override fun onDestroy() {
        // Detener el listener cuando la actividad sea destruida
        super.onDestroy()
        sensorManager.unregisterListener(viewDibujo)
    }

}
//Toma un Context como parámetro y lo utiliza para inicializar la vista.
class ProcessClass(ctx: Context) : View(ctx), SensorEventListener {
    var auxX = auxAnchura!! / 2f
    var auxY = auxAltura!! / 2f
    var auxAcX: Float = 0f
    var auxAcY: Float = 0f
    var auxVelX: Float = 0.0f
    var auxVelY: Float = 0.0f
    var auxRad = 50f

    //Dibuja los puntos
    val map = BitmapFactory.decodeResource(resources, R.drawable.fondo)

    val cnvaRect = Rect(0, 0, auxAnchura!!, auxAltura!!)
    val mapRect = RectF(0f, 0f, map.width.toFloat(), map.height.toFloat())

    var paintPelota = Paint()
    var paintMarcador = Paint()
    private var auxGrav = FloatArray(3)
    private var auxLinAcc = FloatArray(3)

    //Se llama desde el constructor y se utiliza para inicializar los objetos Paint utilizados
    // para dibujar la pelota y el marcador.
    init {
        paintPelota.color = Color.WHITE
        paintMarcador.color = Color.BLACK
        paintMarcador.textSize = 90f
        paintMarcador.style = Paint.Style.FILL_AND_STROKE
    }

    @SuppressLint("DrawAllocation")
    //Se dibuja la imagen de fondo, la pelota y el marcador.
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        mapRect.offsetTo(
            cnvaRect.centerX() - mapRect.width() / 2,
            cnvaRect.centerY() - mapRect.height() / 2
        )

        canvas!!.drawBitmap(map, null, cnvaRect, null)
        canvas.drawCircle(auxX, auxY, auxRad, paintPelota)
        /*canvas.rotate(90f, 50F,50f)*/
        canvas.drawText("[ $contUno : $contDos ]", auxAnchura!! / 2.6f, auxAltura!! / 3f, paintMarcador)

        invalidate()
    }
    //Procesa la aceleración lineal de la pelota y se actualiza su posición.
    override fun onSensorChanged(event: SensorEvent?) {
        val auxAlp = 0.8f
        auxGrav[0] = auxAlp * auxGrav[0] + (1 - auxAlp) * event!!.values[0]
        auxGrav[1] = auxAlp * auxGrav[1] + (1 - auxAlp) * event.values[1]
        auxGrav[2] = auxAlp * auxGrav[2] + (1 - auxAlp) * event.values[2]

        auxLinAcc[0] = event.values[0] - auxGrav[0]   //x
        auxLinAcc[1] = event.values[1] - auxGrav[1]    //y
        auxLinAcc[2] = event.values[2] - auxGrav[2]   //z

        processPelota(auxLinAcc[0], auxLinAcc[1] * -1)
    }
    //Se llama cuando la precisión de un sensor ha cambiado.
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
        //Se utiliza para procesar la aceleración lineal de la pelota y actualizar su posición.
        // Este método llama a los métodos cambioX, cambioY y anotacion.
    private fun processPelota(auxOrX: Float, auxOrY: Float) {
        auxAcX = auxOrX
        auxAcY = auxOrY
        cambioX()
        cambioY()
        anotacion()
    }
    //Actualizar la posición X de la pelota y su velocidad, en función de la aceleración lineal.
    fun cambioX() {
        if (auxX < auxAnchura!! - auxRad && auxX > 0 + auxRad) {
            auxVelX -= auxAcX * 3f
            auxX += auxVelX
        } else if (auxX >= auxAnchura!! - auxRad) {
            auxX = auxAnchura!! - auxRad * 2 + 1
            auxVelX -= auxAcX * 3f
            auxX += auxVelX
        } else if (auxX <= 0 + auxRad) {
            auxX = auxRad * 2 + 1
            auxVelX -= auxAcX * 3f
            auxX += auxVelX
        }
    }
    //Actualizar la posición Y de la pelota y su velocidad, en función de la aceleración lineal.
    fun cambioY() {
        if (auxY < auxAltura!! - auxRad && auxY > 0 + auxRad) {
            auxVelY -= auxAcY * 3f
            auxY += auxVelY
        } else if (auxY >= auxAltura!! - auxRad) {
            auxY = auxAltura!! - auxRad * 3 + 50f
            auxVelY -= auxAcY * 3f
            auxY += auxVelY
        } else if (auxY <= 0 + auxRad) {
            auxY = auxRad * 3 + 50f
            auxVelY -= auxAcY * 3f
            auxY += auxVelY
        }
    }
    //Incrementa el contador si se ha detectado un gol
    fun anotacion() {
        if (auxY >= auxAltura!! - auxRad * 2 && (auxX <= auxAnchura!! / 2f + 50 && auxX >= auxAnchura!! / 2f - 50)) {
            contUno++
            auxX = auxAnchura!! / 2f
            auxY = auxAltura!! / 2f
        }

        if (auxY <= 0 + auxRad * 2 && (auxX <= auxAnchura!! / 2f + 50 && auxX >= auxAnchura!! / 2f - 50)) {
            contDos++
            auxX = auxAnchura!! / 4f
            auxY = auxAltura!! / 2f
        }
    }
}