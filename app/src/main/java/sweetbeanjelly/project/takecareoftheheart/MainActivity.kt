package sweetbeanjelly.project.takecareoftheheart

import android.Manifest
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

// 현재 위치 반경 추가, 사용 X
// 마커 자세히
// 마커 자세히 - 주소 복사

class MainActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener {
    private var googleMap: GoogleMap? = null
    private var lat: Double? = null
    private var lon: Double? = null

    private val job = SupervisorJob()
    private lateinit var dialog: Dialog
    private lateinit var dialogAed: Dialog

    // permission
    private var REQUIRED_PERMISSIONS = arrayOf(
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

        val btnDialog = findViewById<TextView>(R.id.menu)
        val btnCall = findViewById<TextView>(R.id.btnEmergencies)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_use)

        dialogAed = Dialog(this)
        dialogAed.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogAed.setContentView(R.layout.dialog_aed)

        btnDialog.setOnClickListener {
            showDialog()
            /*
            val intent = Intent(this@MainActivity, MainDialog::class.java)
            startActivity(intent)
            */
        }
        btnCall.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:119"))
            startActivity(intent)
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

        getLocation()
    }

    private fun showDialog() {
        dialog.show()
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val esc = dialog.findViewById<ImageButton>(R.id.btnEsc)
        esc.setOnClickListener {
            dialog.dismiss()
        }
        val cpr = dialog.findViewById<Button>(R.id.btn1)
        cpr.setOnClickListener {
            val intent = Intent(this@MainActivity, DialogCprActivity::class.java)
            startActivity(intent)
            dialog.dismiss()
        }
        val aed = dialog.findViewById<Button>(R.id.btn2)
        aed.setOnClickListener {
            val intent = Intent(this@MainActivity, DialogAedActivity::class.java)
            startActivity(intent)
            dialog.dismiss()
        }
    }

    private var backPressedTime = 0L

    override fun onBackPressed() {
        if(System.currentTimeMillis() > backPressedTime + 2500) {
            backPressedTime = System.currentTimeMillis()
            Toast.makeText(this, "뒤로가기 버튼을 한번 더 누르면 종료합니다.", Toast.LENGTH_LONG).show()
            return
        }
        if(System.currentTimeMillis() <= backPressedTime + 2500) {
            finishAffinity()
        }
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
        lateinit var markerLat: String
        lateinit var markerLon: String
        lateinit var title: String // 기관명
        lateinit var place : String // AED 설치된 위치
        lateinit var address: String // 주소

        val row = 10 // AED 목록의 수, 기본값 10
        val checkAddress = mutableMapOf<String, String>()

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
                    address = element.getElementsByTagName("buildAddress").item(0).textContent

                    val location = LatLng(markerLat.toDouble(), markerLon.toDouble())
                    val marker = MarkerOptions().position(location).title(title).snippet(place).draggable(true).icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                    val checkData = marker.snippet!!
                    checkAddress[checkData] = address

                    runOnUiThread {

                        if(i == 0) googleMap!!.addMarker(marker).showInfoWindow()
                        googleMap!!.addMarker(marker)
                        googleMap!!.setOnInfoWindowClickListener {
                            val markerCheck = it.snippet!!
                            checkAddress[markerCheck]?.let { address -> showAedDialog(it.title, it.snippet, address) }
                            println("[위치] $markerCheck [주소] ${checkAddress[markerCheck]}")
                        }

                    }
                }
            }
        } catch (e: Exception) {
            println("API ERROR : $e")
        }
    }.start()

    private val bitmap by lazy {
        val bitmap = ResourcesCompat.getDrawable(resources, R.drawable.ic_pin, null)?.toBitmap()
        Bitmap.createScaledBitmap(bitmap!!, 44, 44, false)
    }

    private fun showAedDialog(title: String, content: String, address: String) {
        dialogAed.show()
        dialogAed.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val esc = dialogAed.findViewById<ImageButton>(R.id.btnClose)
        esc.setOnClickListener {
            dialogAed.dismiss()
        }

        val txtTitle = dialogAed.findViewById<TextView>(R.id.txtTitle)
        txtTitle.text = title

        val txtContent = dialogAed.findViewById<TextView>(R.id.txtContent)
        txtContent.text = content

        val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val addressCopy = dialogAed.findViewById<Button>(R.id.btnAddressCopy)
        addressCopy.setOnClickListener {
            val clipData = ClipData.newPlainText("Address", address)
            clipboardManager.setPrimaryClip(clipData)
            println("복사 확인 [주소] $clipData")
            Toast.makeText(this@MainActivity, "주소가 복사 되었습니다.", Toast.LENGTH_SHORT).show()
        }
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

        val circle1 = CircleOptions().center(location)
                .radius(200.0) // 반지름
                .strokeWidth(0f) // 선
                //.fillColor(Color.parseColor("#43303F9F"))

        this.googleMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15F))
        this.googleMap!!.isMyLocationEnabled = true
        // this.googleMap!!.addCircle(circle1)
        this.googleMap!!.uiSettings.isZoomControlsEnabled = true
        this.googleMap!!.uiSettings.isCompassEnabled = false
        this.googleMap!!.uiSettings.isMapToolbarEnabled = false
    }

    companion object {
        private val PERMISSIONS_REQUEST_CODE = 100
    }
}