package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.location.LocationHelper
import com.example.data.model.RouteResult
import com.example.data.model.StationDistance
import com.example.data.model.TransitLine
import com.example.data.model.TransitStation
import com.example.data.model.UserLocationState
import com.example.data.repository.TransitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransitViewModel(application: Application) : AndroidViewModel(application) {

  val repository = TransitRepository(application)
  private val locationHelper = LocationHelper(application)

  private val _userLocation = MutableStateFlow(TransitRepository.PRESET_LOCATIONS[0])
  val userLocation: StateFlow<UserLocationState> = _userLocation.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedLineFilter = MutableStateFlow<String?>(null)
  val selectedLineFilter: StateFlow<String?> = _selectedLineFilter.asStateFlow()

  private val _selectedStation = MutableStateFlow<TransitStation?>(null)
  val selectedStation: StateFlow<TransitStation?> = _selectedStation.asStateFlow()

  private val _routeOrigin = MutableStateFlow<TransitStation?>(null)
  val routeOrigin: StateFlow<TransitStation?> = _routeOrigin.asStateFlow()

  private val _routeDestination = MutableStateFlow<TransitStation?>(null)
  val routeDestination: StateFlow<TransitStation?> = _routeDestination.asStateFlow()

  private val _routeResult = MutableStateFlow<RouteResult?>(null)
  val routeResult: StateFlow<RouteResult?> = _routeResult.asStateFlow()

  private val _isLocating = MutableStateFlow(false)
  val isLocating: StateFlow<Boolean> = _isLocating.asStateFlow()

  val allLines: List<TransitLine> = repository.getAllLines()
  val allStations: List<TransitStation> = repository.getAllStations()

  val nearestStations: StateFlow<List<StationDistance>> = combine(
    _userLocation,
    _selectedLineFilter,
    _searchQuery
  ) { loc, lineFilter, query ->
    repository.getNearestStations(
      userLat = loc.lat,
      userLon = loc.lon,
      limit = 40,
      lineFilter = lineFilter,
      searchQuery = query
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  init {
    // Check if permission already available and fetch real location
    if (locationHelper.hasLocationPermission()) {
      refreshRealLocation()
    }
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setLineFilter(lineId: String?) {
    _selectedLineFilter.value = if (_selectedLineFilter.value == lineId) null else lineId
  }

  fun selectStation(station: TransitStation?) {
    _selectedStation.value = station
  }

  fun setLocation(preset: UserLocationState) {
    _userLocation.value = preset
  }

  fun refreshRealLocation() {
    if (!locationHelper.hasLocationPermission()) return

    _isLocating.value = true
    viewModelScope.launch {
      locationHelper.getCurrentLocation { loc ->
        _isLocating.value = false
        if (loc != null) {
          _userLocation.value = UserLocationState(
            lat = loc.latitude,
            lon = loc.longitude,
            label = "Minha Localização (GPS)",
            isSimulated = false
          )
        }
      }
    }
  }

  fun setRouteOrigin(station: TransitStation?) {
    _routeOrigin.value = station
    recalculateRoute()
  }

  fun setRouteDestination(station: TransitStation?) {
    _routeDestination.value = station
    recalculateRoute()
  }

  fun swapRouteEndpoints() {
    val temp = _routeOrigin.value
    _routeOrigin.value = _routeDestination.value
    _routeDestination.value = temp
    recalculateRoute()
  }

  fun setRouteFromNearestTo(destination: TransitStation) {
    val nearest = nearestStations.value.firstOrNull { it.station.id != destination.id }?.station
    if (nearest != null) {
      _routeOrigin.value = nearest
      _routeDestination.value = destination
      recalculateRoute()
    }
  }

  private fun recalculateRoute() {
    val orig = _routeOrigin.value
    val dest = _routeDestination.value
    if (orig != null && dest != null) {
      viewModelScope.launch {
        _routeResult.value = repository.findRoute(orig.id, dest.id)
      }
    } else {
      _routeResult.value = null
    }
  }
}

