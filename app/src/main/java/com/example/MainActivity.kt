package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.TransitViewModel
import com.example.ui.screens.LinesCatalogScreen
import com.example.ui.screens.MapScreen
import com.example.ui.screens.NearestStationsScreen
import com.example.ui.screens.RoutePlannerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemePreferences

enum class AppNavTab(
  val title: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String
) {
  NEAREST("Perto de Mim", Icons.Filled.NearMe, Icons.Outlined.NearMe, "tab_nearest"),
  MAP("Mapa da Rede", Icons.Filled.Map, Icons.Outlined.Map, "tab_map"),
  LINES("Linhas", Icons.Filled.Train, Icons.Outlined.Train, "tab_lines"),
  ROUTES("Rotas", Icons.Filled.AltRoute, Icons.Outlined.AltRoute, "tab_routes")
}

class MainActivity : ComponentActivity() {

  private val viewModel: TransitViewModel by viewModels()

  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      var themeMode by remember { mutableStateOf(ThemePreferences.load(this)) }
      val darkTheme = themeMode.isDark(isSystemInDarkTheme())

      // Ícones da barra de status/navegação acompanham o tema escolhido, não só o do sistema
      DisposableEffect(darkTheme) {
        val style = if (darkTheme) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        onDispose { }
      }

      MyApplicationTheme(darkTheme = darkTheme) {
        var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

        // Location permission launcher
        val permissionLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
          val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
              permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
          if (granted) {
            viewModel.refreshRealLocation()
          }
        }

        LaunchedEffect(Unit) {
          permissionLauncher.launch(
            arrayOf(
              Manifest.permission.ACCESS_FINE_LOCATION,
              Manifest.permission.ACCESS_COARSE_LOCATION
            )
          )
        }

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          topBar = {
            CenterAlignedTopAppBar(
              title = {
                Text(
                  text = "Metrô e Trem SP",
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 18.sp
                )
              },
              actions = {
                IconButton(
                  onClick = {
                    themeMode = themeMode.next()
                    ThemePreferences.save(this@MainActivity, themeMode)
                  },
                  modifier = Modifier.testTag("btn_theme")
                ) {
                  Icon(
                    imageVector = when (themeMode) {
                      ThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto
                      ThemeMode.LIGHT -> Icons.Filled.LightMode
                      ThemeMode.DARK -> Icons.Filled.DarkMode
                    },
                    contentDescription = "${themeMode.label} (toque para trocar)"
                  )
                }
              },
              colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
              )
            )
          },
          bottomBar = {
            NavigationBar(
              modifier = Modifier.testTag("bottom_nav_bar")
            ) {
              AppNavTab.entries.forEachIndexed { index, tab ->
                val isSelected = selectedTabIndex == index
                NavigationBarItem(
                  selected = isSelected,
                  onClick = { selectedTabIndex = index },
                  icon = {
                    Icon(
                      imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                      contentDescription = tab.title
                    )
                  },
                  label = {
                    Text(text = tab.title, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                  },
                  modifier = Modifier.testTag(tab.testTag)
                )
              }
            }
          }
        ) { innerPadding ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          ) {
            when (AppNavTab.entries[selectedTabIndex]) {
              AppNavTab.NEAREST -> {
                NearestStationsScreen(
                  viewModel = viewModel,
                  onNavigateToMap = {
                    selectedTabIndex = AppNavTab.MAP.ordinal
                  },
                  onNavigateToRoute = {
                    selectedTabIndex = AppNavTab.ROUTES.ordinal
                  },
                  onRequestLocationPermission = {
                    permissionLauncher.launch(
                      arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                      )
                    )
                  }
                )
              }
              AppNavTab.MAP -> {
                MapScreen(
                  viewModel = viewModel,
                  onNavigateToRoute = {
                    selectedTabIndex = AppNavTab.ROUTES.ordinal
                  }
                )
              }
              AppNavTab.LINES -> {
                LinesCatalogScreen(
                  viewModel = viewModel,
                  onNavigateToRoute = {
                    selectedTabIndex = AppNavTab.ROUTES.ordinal
                  }
                )
              }
              AppNavTab.ROUTES -> {
                RoutePlannerScreen(
                  viewModel = viewModel
                )
              }
            }
          }
        }
      }
    }
  }
}
