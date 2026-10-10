package com.gabrielifrim.dromos.core.map

import android.view.Gravity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gabrielifrim.dromos.core.domain.model.GeoPoint
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.MultiLineString
import org.maplibre.geojson.Point

/**
 * Carte vectorielle OpenStreetMap avec le tracé en direct.
 *
 * La caméra suit la dernière position tant que l'utilisateur ne déplace pas la
 * carte ; un bouton la recentre. Si le style ne se charge pas (pas de réseau au
 * premier affichage), on bascule sur [TraceCanvas] : la sortie reste visible.
 *
 * @param online false : aucune requête réseau, tracé seul.
 * @param live true : la caméra suit la dernière position (sortie en cours) ;
 *   false : elle cadre le tracé entier (sortie terminée).
 */
@Composable
fun TrackMap(
    segments: List<List<GeoPoint>>,
    traceColor: Color,
    online: Boolean,
    modifier: Modifier = Modifier,
    live: Boolean = true,
    cursor: GeoPoint? = null,
) {
    var failed by remember { mutableStateOf(false) }
    if (segments.all { it.isEmpty() }) {
        // Sans position, la carte montrerait le planisphère : on attend le premier point.
        WaitingForFix(modifier)
        return
    }
    if (!online || failed) {
        TraceCanvas(
            segments = segments,
            traceColor = traceColor,
            modifier = modifier,
            label = stringResource(if (online) R.string.map_unavailable else R.string.map_offline),
            cursor = cursor,
        )
        return
    }

    val context = LocalContext.current
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val casingColor = if (dark) Color(0xFF111315) else Color.White
    // Curseur à l’encre : neutre, il ne se confond ni avec le tracé ni avec la tête.
    val cursorColor = MaterialTheme.colorScheme.onSurface
    // Couleur affichée pendant le chargement du style : celle du thème, pour
    // éviter un rectangle beige en thème sombre.
    val loadColor = MaterialTheme.colorScheme.surfaceContainer.toArgb()
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context, MapLibreMapOptions.createFromAttributes(context).foregroundLoadColor(loadColor))
            .apply { onCreate(null) }
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    var follow by remember { mutableStateOf(true) }

    MapLifecycle(mapView)

    LaunchedEffect(mapView) {
        mapView.addOnDidFailLoadingMapListener { failed = true }
        mapView.getMapAsync { m ->
            m.uiSettings.apply {
                isLogoEnabled = false
                // Attribution affichée par nos soins (texte compact, toujours visible).
                isAttributionEnabled = false
                isRotateGesturesEnabled = false
                isTiltGesturesEnabled = false
                isCompassEnabled = false
                compassGravity = Gravity.TOP or Gravity.END
            }
            m.addOnCameraMoveStartedListener { reason ->
                if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) follow = false
            }
            map = m
        }
    }

    LaunchedEffect(map, dark) {
        val m = map ?: return@LaunchedEffect
        style = null
        m.setStyle(Style.Builder().fromUri(if (dark) MapStyles.DARK else MapStyles.LIGHT)) { loaded ->
            loaded.addSource(GeoJsonSource(TRACE_SOURCE))
            loaded.addSource(GeoJsonSource(POSITION_SOURCE))
            loaded.addSource(GeoJsonSource(CURSOR_SOURCE))
            loaded.addLayer(
                LineLayer(TRACE_CASING, TRACE_SOURCE).withProperties(
                    PropertyFactory.lineColor(casingColor.toArgb()),
                    PropertyFactory.lineWidth(9f),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                ),
            )
            loaded.addLayer(
                LineLayer(TRACE_LINE, TRACE_SOURCE).withProperties(
                    PropertyFactory.lineColor(traceColor.toArgb()),
                    PropertyFactory.lineWidth(5f),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                ),
            )
            // Tête lumineuse : halo flou fixe. Pas de pulsation ici, elle obligerait la
            // carte à se redessiner à chaque image pendant toute la sortie.
            loaded.addLayer(
                CircleLayer(POSITION_HALO, POSITION_SOURCE).withProperties(
                    PropertyFactory.circleRadius(18f),
                    PropertyFactory.circleColor(traceColor.toArgb()),
                    PropertyFactory.circleOpacity(0.35f),
                    PropertyFactory.circleBlur(0.8f),
                ),
            )
            loaded.addLayer(
                CircleLayer(POSITION_LAYER, POSITION_SOURCE).withProperties(
                    PropertyFactory.circleRadius(7f),
                    PropertyFactory.circleColor(traceColor.toArgb()),
                    PropertyFactory.circleStrokeColor(casingColor.toArgb()),
                    PropertyFactory.circleStrokeWidth(3f),
                ),
            )
            // Curseur des graphiques : au-dessus de tout, cerclé pour rester lisible sur le tracé.
            loaded.addLayer(
                CircleLayer(CURSOR_LAYER, CURSOR_SOURCE).withProperties(
                    PropertyFactory.circleRadius(6f),
                    PropertyFactory.circleColor(cursorColor.toArgb()),
                    PropertyFactory.circleStrokeColor(casingColor.toArgb()),
                    PropertyFactory.circleStrokeWidth(3f),
                ),
            )
            style = loaded
        }
    }

    // Déplacer le curseur ne touche pas à la caméra : la carte reste cadrée sur le tracé.
    LaunchedEffect(style, cursor) {
        val s = style ?: return@LaunchedEffect
        s.getSourceAs<GeoJsonSource>(CURSOR_SOURCE)?.setGeoJson(
            FeatureCollection.fromFeatures(listOfNotNull(cursor?.let { Feature.fromGeometry(it.toPoint()) })),
        )
    }

    // Couleur du tracé : suit la palette sans recharger le style.
    LaunchedEffect(style, traceColor) {
        val s = style ?: return@LaunchedEffect
        s.getLayerAs<LineLayer>(TRACE_LINE)?.setProperties(PropertyFactory.lineColor(traceColor.toArgb()))
        s.getLayerAs<CircleLayer>(POSITION_LAYER)?.setProperties(PropertyFactory.circleColor(traceColor.toArgb()))
        s.getLayerAs<CircleLayer>(POSITION_HALO)?.setProperties(PropertyFactory.circleColor(traceColor.toArgb()))
    }

    LaunchedEffect(style, segments) {
        val s = style ?: return@LaunchedEffect
        val lines = segments.filter { it.size >= 2 }.map { seg -> LineString.fromLngLats(seg.map { it.toPoint() }) }
        s.getSourceAs<GeoJsonSource>(TRACE_SOURCE)?.setGeoJson(
            FeatureCollection.fromFeatures(
                if (lines.isEmpty()) emptyList() else listOf(Feature.fromGeometry(MultiLineString.fromLineStrings(lines))),
            ),
        )
        val last = segments.lastOrNull()?.lastOrNull()
        s.getSourceAs<GeoJsonSource>(POSITION_SOURCE)?.setGeoJson(
            FeatureCollection.fromFeatures(listOfNotNull(last?.let { Feature.fromGeometry(it.toPoint()) })),
        )
    }

    val last = segments.lastOrNull()?.lastOrNull()
    LaunchedEffect(map, last, follow, style) {
        val m = map ?: return@LaunchedEffect
        val target = last ?: return@LaunchedEffect
        if (!follow) return@LaunchedEffect
        if (!live) {
            // Sortie terminée : tout le tracé dans le cadre.
            val all = segments.flatten().map { it.toLatLng() }.distinct()
            if (all.size >= 2) {
                m.moveCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.Builder().includes(all).build(), FIT_PADDING_PX))
            } else {
                m.moveCamera(CameraUpdateFactory.newLatLngZoom(target.toLatLng(), FOLLOW_ZOOM))
            }
            return@LaunchedEffect
        }
        val zoom = m.cameraPosition.zoom.takeIf { it >= MIN_FOLLOW_ZOOM } ?: FOLLOW_ZOOM
        m.easeCamera(
            CameraUpdateFactory.newCameraPosition(CameraPosition.Builder().target(target.toLatLng()).zoom(zoom).build()),
            CAMERA_MS,
        )
    }

    Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        // Attribution exigée par la licence ODbL d'OpenStreetMap : toujours visible,
        // sur une pastille pour rester lisible quel que soit le fond.
        Text(
            stringResource(R.string.map_attribution),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF1A1A1A),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .background(Color.White.copy(alpha = 0.8f), MaterialTheme.shapes.small)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
        if (!follow && last != null) {
            SmallFloatingActionButton(
                onClick = { follow = true },
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.map_recenter))
            }
        }
    }
}

@Composable
private fun WaitingForFix(modifier: Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainer), contentAlignment = Alignment.Center) {
        Text(
            stringResource(R.string.map_waiting),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Le MapView suit le cycle de vie de l'écran, sinon il fuit sa surface OpenGL. */
@Composable
private fun MapLifecycle(mapView: MapView) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }
}

private fun GeoPoint.toPoint(): Point = Point.fromLngLat(longitude, latitude)
private fun GeoPoint.toLatLng(): LatLng = LatLng(latitude, longitude)

private const val TRACE_SOURCE = "dromos-trace"
private const val POSITION_SOURCE = "dromos-position"
private const val TRACE_CASING = "dromos-trace-casing"
private const val TRACE_LINE = "dromos-trace-line"
private const val POSITION_LAYER = "dromos-position"
private const val POSITION_HALO = "dromos-position-halo"
private const val CURSOR_SOURCE = "dromos-cursor"
private const val CURSOR_LAYER = "dromos-cursor"
private const val FOLLOW_ZOOM = 16.0
private const val MIN_FOLLOW_ZOOM = 12.0
private const val CAMERA_MS = 600
private const val FIT_PADDING_PX = 80
