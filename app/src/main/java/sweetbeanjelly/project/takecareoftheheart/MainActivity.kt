package sweetbeanjelly.project.takecareoftheheart

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.*
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

class MainActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener {
    private var googleMap: GoogleMap? = null
    private var lat: Double? = null
    private var lon: Double? = null

    private val job = SupervisorJob()

    // permission
    var REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    private var layout: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        setContentView(R.layout.activity_main)
        layout = findViewById(R.id.mainView)

        val menu = findViewById<ImageView>(R.id.menu)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        menu.setOnClickListener {
            println("확인")
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

        getLocation()
    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                115
            )
            return
        }
        val locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager
        val criteria = Criteria()
        val provider = locationManager.getBestProvider(criteria, true)!!
        val location = locationManager.getLastKnownLocation(provider)
        if (location != null) onLocationChanged(location) else locationManager.requestLocationUpdates(
            provider,
            20000,
            0f,
            this
        )
    }

    override fun onLocationChanged(location: Location) {
        lon = location.longitude
        lat = location.latitude

        readHeart()
    }

    private fun readHeart() = GlobalScope.launch(job) {
        var markerLat = ""
        var markerLon = ""
        var title = "" // 기관명
        var place = "" // AED 설치된 위치
        var address = "" // 주소

        val row = 10 // AED 목록의 수, 기본값 10

        try {
            val url = HeartAPI.url + "WGS84_LON=" + lon + "&WGS84_LAT=" + lat + "&numOfRows=" + row + "&serviceKey=" + HeartAPI.key
            val xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url)

            val itemList = xml.getElementsByTagName("item")

            for (i in 0 until itemList.length) {
                val n: Node = itemList.item(i)

                if (n.nodeType == Node.ELEMENT_NODE) {

                    val element = n as Element
                    val map = mutableMapOf<String, String>()

                    for (j in 0 until element.attributes.length)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                            map.putIfAbsent(
                                element.attributes.item(j).nodeName, element.attributes.item(
                                    j
                                ).nodeValue
                            )

                    markerLat = element.getElementsByTagName("wgs84Lat").item(0).textContent
                    markerLon = element.getElementsByTagName("wgs84Lon").item(0).textContent
                    title = element.getElementsByTagName("org").item(0).textContent
                    place = element.getElementsByTagName("buildPlace").item(0).textContent

                    val location = LatLng(markerLat.toDouble(), markerLon.toDouble())
                    val marker = MarkerOptions().position(location).title(title).snippet(place).draggable(
                        true
                    ).icon(BitmapDescriptorFactory.fromBitmap(bitmap))

                    runOnUiThread {
                        googleMap!!.addMarker(marker)
                    }

                    println("$title 위치 : $place")
                }
            }
        } catch (e: Exception) {
            println("API ERROR : $e")
        }
    }.start()

    private val bitmap by lazy {
        //
        val bitmap = ResourcesCompat.getDrawable(resources, R.drawable.ic_pin, null)?.toBitmap()
        Bitmap.createScaledBitmap(bitmap!!, 44, 44, false)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap

        // permission check
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED && hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
            // permission OK
        }
        else {
            // permission NO
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])) {
                Snackbar.make(layout!!, "위치 접근 권한이 필요합니다.", Snackbar.LENGTH_INDEFINITE).setAction("확인") {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE
                    )
                }.show()
            } else ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                PERMISSIONS_REQUEST_CODE
            )
        }

        val location = LatLng(lat!!, lon!!)

//        val marker = MarkerOptions().position(location).title("현재 위치").draggable(true)
//        this.googleMap!!.addMarker(marker)
        this.googleMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15F))
        this.googleMap!!.isMyLocationEnabled = true
        this.googleMap!!.uiSettings.isZoomControlsEnabled = true
    }

    companion object {
        private val PERMISSIONS_REQUEST_CODE = 100
    }
}