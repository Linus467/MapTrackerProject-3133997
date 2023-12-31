package com.griffith.maptrackerproject.Services

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.griffith.maptrackerproject.DB.Locations
import com.griffith.maptrackerproject.DB.LocationsDAO
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.sql.Date
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : Service(), LocationListener {
    private lateinit var locationManager: LocationManager
    private val locationUpdateInterval: Long = 5000 // 5 seconds
    private val locationUpdateDistance: Float = 10f // 10 meters
    private var isInitialized: Boolean = false

    var locationsUpdatesActive: Boolean = false

    @Inject
    lateinit var locationsDAO: LocationsDAO

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        isInitialized = true
    }

    fun startLocationUpdates() {
        if (!isInitialized) {
            onCreate()
        }

        Log.d("LocationUpdates", "started")

        val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        Log.d("LocationUpdates", "Permission check result: $permissionCheck")

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, locationUpdateInterval, locationUpdateDistance, this)
            locationsUpdatesActive = true
        } else {
            // Handle the case where the permission is not granted
            Log.e("LocationService", "Location permission not granted. Cannot start location updates.")
        }
    }
    fun stopLocationUpdates(){
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationManager.removeUpdates(this)
        locationsUpdatesActive = false
        Log.d("LocationUpdates", "paused")
    }
    override fun onLocationChanged(location: Location) {
        val geoPoint = GeoPoint(location.latitude, location.longitude, location.altitude)
        val time = Calendar.getInstance().time
        val loc = Locations(geoPoint.latitude,geoPoint.longitude,geoPoint.altitude, date = Date(time.time))
        saveGeoPoint(loc)
    }

    private fun saveGeoPoint(locations: Locations){
        CoroutineScope(Dispatchers.IO).launch {
            val lastItem = locationsDAO.getLastItem()
            //prevent duplicates of the same location
            if(locations.longitude != lastItem?.longitude && locations.latitude != lastItem?.latitude){
                locationsDAO.insert(locations)
            }
        }
    }

    private val binder = LocalBinder()

    inner class LocalBinder : Binder(){
        fun getService(): LocationService = this@LocationService
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

}