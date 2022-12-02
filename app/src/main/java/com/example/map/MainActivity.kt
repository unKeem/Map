package com.example.map

import android.Manifest.permission.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.map.data.Library
import com.example.map.databinding.ActivityMainBinding
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.OnSuccessListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/*구글 api에 연결되면, 실패하면, 준비되면 콜백*/
class MainActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, OnMapReadyCallback {

    lateinit var binding: ActivityMainBinding

    /*1.위치정보객체참조변수*/
    lateinit var providerClient: FusedLocationProviderClient

    /*2.위치정보를 획득하기 위한 접속요청 객체참조변수*/
    lateinit var apiClient: GoogleApiClient

    /*3.지도정보 객체참조변수*/
    var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /*permission 허용여부 받음*/
        var requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            if (it.all { permission -> permission.value == true }) {
                /*8.현재 사용중인 모바일의 위치정보를 요청*/
                apiClient.connect()
            } else {
                Toast.makeText(this, "Requires permission to use the app", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }/*requestPermissionLauncher*/

        /*4. 지도 정보를 어떤 프래그먼트에 보여줄 것인가를 설정 xml파일에 view에 supportFragmentManager필요*/
        (supportFragmentManager.findFragmentById(R.id.view_map) as SupportMapFragment).getMapAsync(
            this)

        /*5. 위치정보 획득*/
        providerClient = LocationServices.getFusedLocationProviderClient(this)

        /*6. 위치정보 획득을 위한 접속*/
        apiClient = GoogleApiClient.Builder(this)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build()

        /*7.사용자에게 퍼미션요청*/
        if (ContextCompat.checkSelfPermission(this,
                ACCESS_FINE_LOCATION) !== PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this,
                ACCESS_COARSE_LOCATION) !== PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this,
                ACCESS_NETWORK_STATE) !== PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this,
                WRITE_EXTERNAL_STORAGE) !== PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(
                arrayOf(
                    ACCESS_FINE_LOCATION,
                    ACCESS_COARSE_LOCATION,
                    ACCESS_NETWORK_STATE,
                    WRITE_EXTERNAL_STORAGE
                )
            )
        } else {
            /*8.*/
            apiClient.connect()
        }/*permission if/else */
    }/*oncreate*/

    /*9. 콜백함수 Location Provider 준비 요청시 준비 가능한 상태가 되면 onConnected 콜백 : 위치정보획득 할 수 있음*/
    override fun onConnected(p0: Bundle?) {
       /*공공데이터 가져오기*/
        loadLibraries()
    }/*onConnecter*/

    fun loadLibraries(){
        val retrofit = Retrofit.Builder()
            .baseUrl(SeoulOpenAPI.DOMAIN)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // interface SeoulOpenService 로 존재함
        val service = retrofit.create(SeoulOpenService::class.java)

        service.getLibraries(SeoulOpenAPI.API_KEY, 10)  //200은 200개를 가져오겠다. 설정함.
            .enqueue(object : Callback<Library> {
                override fun onResponse(call: Call<Library>, response: Response<Library>) {
                    val result = response.body()
                    showLibraries(result)
                }
                override fun onFailure(call: Call<Library>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "데이터를 가져올수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            })  //end of service.getLibrarys
    }// end of loadLibraries

    //공공데이터 지도 보이기
    fun showLibraries(result: Library?){
        //위도, 경도
        var latlng: LatLng
        //모든위치를 포함한 정보로 보이기
        val latlangBounds = LatLngBounds.builder()

        result?.let{
            for(library in it.SeoulPublicLibraryInfo.row){
                val name = library.LBRRY_NAME
                val phone = library.TEL_NO
                Log.d("kim", "${name}  ${phone}")
                latlng = LatLng(library.XCNTS.toDouble(), library.YDNTS.toDouble())
                moveMap(latlng, library.LBRRY_NAME)
                latlangBounds.include(latlng)
            }
        }

        val bounds = latlangBounds.build()
        val padding = 40 //한영역에 보이는기능에서 주변경계선이 잘렸을때 보이게 할경우
        val camera = CameraUpdateFactory.newLatLngBounds(bounds, padding)
        googleMap?.moveCamera(camera)
    }

    /*사용하고 있던 Location Provider 더이상 이용이 불가능한 사항이 되었을때*/
    override fun onConnectionSuspended(data: Int) {
        Log.d("map", "location provider 더이상 이용 불가능한 상황 ${data}")
    }

    /*location provider가 제공되지 않았을때*/
    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.d("map", "location provider가 제공되지 않음  ${connectionResult.errorMessage}")
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        this.googleMap = googleMap
    }

    /*제공된 위도, 경도값을 중심으로 지도의 위치를 변경*/
    fun moveMap(latLng: LatLng, labraryName: String ?= null) {
        /*10-1. 위도, 경도로 객체 생성*/
        /*10-2카메라 위치정보생성*/
        val cameraPosition = CameraPosition.Builder()
            .target(latLng)
            .zoom(16f)
            .build()
        /*10-3현재 전체지도맵으로 셋팅된 카메라 위치를 내가 지정한 위치와 확대값을 기준으로 지도맵 위치를 변경*/
        googleMap!!.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

        var bitmapDrawable: BitmapDrawable
        /*버전에 따라 마커 부르는 코드가 다름*/
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            bitmapDrawable = getDrawable(R.drawable.ic_marker) as BitmapDrawable
        }else{
            bitmapDrawable = resources.getDrawable(R.drawable.ic_marker) as BitmapDrawable
        }
        //사이즈가 엄청클수 있으므로 사이즈를 조정한다.
        val scaleBitmap = Bitmap.createScaledBitmap(bitmapDrawable.bitmap, 50, 60, false)
        val discriptor = BitmapDescriptorFactory.fromBitmap(scaleBitmap)

        /*10-4.마커 아이콘을 그 위치에 셋팅한다*/
        val markerOptions = MarkerOptions()
//        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))

        markerOptions.icon(discriptor)
        markerOptions.position(latLng)
        labraryName?.let{
            markerOptions.title(labraryName)
        }?:let{
            markerOptions.title("이름없음")
        }
        googleMap!!.addMarker(markerOptions)
    }
}