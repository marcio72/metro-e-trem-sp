package com.example.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class LocationHelper(private val context: Context) {

  private val fusedClient by lazy {
    LocationServices.getFusedLocationProviderClient(context)
  }

  fun hasLocationPermission(): Boolean {
    val fine = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
  }

  @SuppressLint("MissingPermission")
  fun getCurrentLocation(onLocationReceived: (Location?) -> Unit) {
    if (!hasLocationPermission()) {
      onLocationReceived(null)
      return
    }

    try {
      val cancellationTokenSource = CancellationTokenSource()
      fusedClient.getCurrentLocation(
        Priority.PRIORITY_HIGH_ACCURACY,
        cancellationTokenSource.token
      ).addOnSuccessListener { loc ->
        if (loc != null) {
          onLocationReceived(loc)
        } else {
          // Fallback to getLastLocation
          fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
            if (lastLoc != null) {
              onLocationReceived(lastLoc)
            } else {
              fallbackToLocationManager(onLocationReceived)
            }
          }.addOnFailureListener {
            fallbackToLocationManager(onLocationReceived)
          }
        }
      }.addOnFailureListener {
        fallbackToLocationManager(onLocationReceived)
      }
    } catch (e: Exception) {
      fallbackToLocationManager(onLocationReceived)
    }
  }

  @SuppressLint("MissingPermission")
  private fun fallbackToLocationManager(onLocationReceived: (Location?) -> Unit) {
    try {
      val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
      val gpsLoc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
      val netLoc = lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
      val best = gpsLoc ?: netLoc
      onLocationReceived(best)
    } catch (e: Exception) {
      onLocationReceived(null)
    }
  }
}
