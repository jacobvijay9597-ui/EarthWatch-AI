package com.example.data.local

import com.example.data.model.DemoScenarioPreset
import com.example.data.model.GeoPointLatLng
import com.example.data.model.HistoricalLandslideRecord
import com.example.data.model.MonitoredCorridor
import com.example.data.model.MonitoredSpot
import com.example.data.model.NilgirisLocation
import com.example.data.model.RiskLevel

object NilgirisReferenceCatalog {

    val locations: List<NilgirisLocation> = listOf(
        NilgirisLocation(
            id = "coimbatore",
            name = "Coimbatore",
            talukOrArea = "Western Tamil Nadu Plains Gateway",
            elevationMeters = 411,
            latitude = 11.0168,
            longitude = 76.9558,
            mapX = 0.91f,
            mapY = 0.93f
        ),
        NilgirisLocation(
            id = "ooty",
            name = "Ooty (Udhagamandalam)",
            talukOrArea = "Nilgiris Central Plateau",
            elevationMeters = 2240,
            latitude = 11.4102,
            longitude = 76.6950,
            mapX = 0.45f,
            mapY = 0.48f
        ),
        NilgirisLocation(
            id = "coonoor",
            name = "Coonoor",
            talukOrArea = "Southern Ghat Escarpment",
            elevationMeters = 1850,
            latitude = 11.3530,
            longitude = 76.7959,
            mapX = 0.64f,
            mapY = 0.65f
        ),
        NilgirisLocation(
            id = "mettupalayam",
            name = "Mettupalayam",
            talukOrArea = "Foothills Gateway",
            elevationMeters = 314,
            latitude = 11.2994,
            longitude = 76.9355,
            mapX = 0.85f,
            mapY = 0.79f
        ),
        NilgirisLocation(
            id = "kotagiri",
            name = "Kotagiri",
            talukOrArea = "Eastern Nilgiri Ridge",
            elevationMeters = 1793,
            latitude = 11.4216,
            longitude = 76.8617,
            mapX = 0.74f,
            mapY = 0.44f
        ),
        NilgirisLocation(
            id = "masinagudi",
            name = "Masinagudi",
            talukOrArea = "Northern Mudumalai Foothills",
            elevationMeters = 960,
            latitude = 11.5734,
            longitude = 76.6442,
            mapX = 0.36f,
            mapY = 0.16f
        ),
        NilgirisLocation(
            id = "gudalur",
            name = "Gudalur",
            talukOrArea = "Western Wynaad Valley",
            elevationMeters = 1072,
            latitude = 11.5030,
            longitude = 76.4918,
            mapX = 0.14f,
            mapY = 0.28f
        )
    )

    val corridors: List<MonitoredCorridor> = listOf(
        MonitoredCorridor(
            id = "coimbatore_ooty",
            name = "Coimbatore – Mettupalayam – Coonoor – Ooty",
            highwayCode = "NH-181 Main Ascent",
            fromLocationId = "coimbatore",
            toLocationId = "ooty",
            distanceKm = 88,
            averageDriveMinutes = 170,
            maxSlopeDegrees = 32,
            representativeElevationMeters = 1350,
            hairpinCount = 17,
            soilAndGeology = "Plains alluvium transitioning to steep charnockite & deep lateritic ghat clay",
            baselineTerrainScore = 25,
            historicalLandslideCount = 11,
            mapPolyline = listOf(
                0.91f to 0.93f,
                0.88f to 0.86f,
                0.85f to 0.79f,
                0.78f to 0.74f,
                0.71f to 0.69f,
                0.64f to 0.65f,
                0.55f to 0.57f,
                0.45f to 0.48f
            ),
            geoPolyline = listOf(
                GeoPointLatLng(11.0168, 76.9558), // Coimbatore
                GeoPointLatLng(11.1536, 76.9437), // Periyanaickenpalayam
                GeoPointLatLng(11.2422, 76.9602), // Karamadai
                GeoPointLatLng(11.2994, 76.9355), // Mettupalayam
                GeoPointLatLng(11.3370, 76.8725), // Kallar
                GeoPointLatLng(11.3455, 76.8350), // Burliar / Marappalam
                GeoPointLatLng(11.3530, 76.7959), // Coonoor
                GeoPointLatLng(11.3672, 76.7595), // Wellington
                GeoPointLatLng(11.3775, 76.7270), // Aruvankadu / Ketti
                GeoPointLatLng(11.4102, 76.6950)  // Ooty
            ),
            alternateCorridorIds = listOf("mettupalayam_kotagiri_ooty", "mettupalayam_coonoor", "coonoor_ooty"),
            monitoredSpots = listOf(
                MonitoredSpot(
                    id = "spot_cbe_kallar",
                    name = "Kallar Hairpins & Foothill Cut",
                    corridorId = "coimbatore_ooty",
                    elevationMeters = 640,
                    slopeDegrees = 28,
                    latitude = 11.3370,
                    longitude = 76.8725,
                    mapX = 0.78f,
                    mapY = 0.74f,
                    hazardNote = "NH-181 ghat ascent begins; steep cut-slope vulnerable to cloudburst boulder falls"
                ),
                MonitoredSpot(
                    id = "spot_cbe_burliar",
                    name = "Marappalam – Burliar Gorge",
                    corridorId = "coimbatore_ooty",
                    elevationMeters = 1180,
                    slopeDegrees = 32,
                    latitude = 11.3455,
                    longitude = 76.8350,
                    mapX = 0.71f,
                    mapY = 0.69f,
                    hazardNote = "Critical Coimbatore–Ooty monsoon debris-flow and road-slip zone"
                ),
                MonitoredSpot(
                    id = "spot_cbe_ketti",
                    name = "Aruvankadu – Ketti Valley Cut",
                    corridorId = "coimbatore_ooty",
                    elevationMeters = 1980,
                    slopeDegrees = 21,
                    latitude = 11.3775,
                    longitude = 76.7270,
                    mapX = 0.55f,
                    mapY = 0.57f,
                    hazardNote = "Upper ridge retaining-wall saturation point before Ooty"
                )
            )
        ),
        MonitoredCorridor(
            id = "mettupalayam_coonoor",
            name = "Mettupalayam – Coonoor Ghat",
            highwayCode = "NH-181",
            fromLocationId = "mettupalayam",
            toLocationId = "coonoor",
            distanceKm = 33,
            averageDriveMinutes = 70,
            maxSlopeDegrees = 32,
            representativeElevationMeters = 1180,
            hairpinCount = 14,
            soilAndGeology = "Weathered charnockite & deep red lateritic clay over fractured gneiss",
            baselineTerrainScore = 26,
            historicalLandslideCount = 9,
            mapPolyline = listOf(
                0.85f to 0.79f,
                0.78f to 0.74f,
                0.71f to 0.69f,
                0.64f to 0.65f
            ),
            geoPolyline = listOf(
                GeoPointLatLng(11.2994, 76.9355),
                GeoPointLatLng(11.3370, 76.8725),
                GeoPointLatLng(11.3455, 76.8350),
                GeoPointLatLng(11.3530, 76.7959)
            ),
            alternateCorridorIds = listOf("mettupalayam_kotagiri_ooty"),
            monitoredSpots = listOf(
                MonitoredSpot(
                    id = "spot_kallar",
                    name = "Kallar Hairpin Bends 1–4",
                    corridorId = "mettupalayam_coonoor",
                    elevationMeters = 640,
                    slopeDegrees = 28,
                    latitude = 11.3370,
                    longitude = 76.8725,
                    mapX = 0.78f,
                    mapY = 0.74f,
                    hazardNote = "Steep toe-cut slope vulnerable to cloudburst boulder falls"
                ),
                MonitoredSpot(
                    id = "spot_marappalam",
                    name = "Marappalam – Burliar Gorge",
                    corridorId = "mettupalayam_coonoor",
                    elevationMeters = 1180,
                    slopeDegrees = 32,
                    latitude = 11.3455,
                    longitude = 76.8350,
                    mapX = 0.71f,
                    mapY = 0.69f,
                    hazardNote = "High historical debris-flow zone during northeast monsoon peaks"
                )
            )
        ),
        MonitoredCorridor(
            id = "coonoor_ooty",
            name = "Coonoor – Ooty Ridge Corridor",
            highwayCode = "NH-181",
            fromLocationId = "coonoor",
            toLocationId = "ooty",
            distanceKm = 19,
            averageDriveMinutes = 40,
            maxSlopeDegrees = 21,
            representativeElevationMeters = 1980,
            hairpinCount = 3,
            soilAndGeology = "Lateritic tea-estate topsoil with retaining wall embankments",
            baselineTerrainScore = 15,
            historicalLandslideCount = 4,
            mapPolyline = listOf(
                0.64f to 0.65f,
                0.58f to 0.60f,
                0.52f to 0.54f,
                0.45f to 0.48f
            ),
            geoPolyline = listOf(
                GeoPointLatLng(11.3530, 76.7959),
                GeoPointLatLng(11.3672, 76.7595),
                GeoPointLatLng(11.3775, 76.7270),
                GeoPointLatLng(11.4102, 76.6950)
            ),
            alternateCorridorIds = listOf("mettupalayam_kotagiri_ooty"),
            monitoredSpots = listOf(
                MonitoredSpot(
                    id = "spot_aruvankadu",
                    name = "Aruvankadu – Ketti Valley Cut",
                    corridorId = "coonoor_ooty",
                    elevationMeters = 1980,
                    slopeDegrees = 21,
                    latitude = 11.3775,
                    longitude = 76.7270,
                    mapX = 0.55f,
                    mapY = 0.57f,
                    hazardNote = "Moderate embankment saturation risk during sustained multi-day rain"
                )
            )
        ),
        MonitoredCorridor(
            id = "mettupalayam_kotagiri_ooty",
            name = "Mettupalayam – Kotagiri – Ooty",
            highwayCode = "SH-15",
            fromLocationId = "mettupalayam",
            toLocationId = "ooty",
            distanceKm = 62,
            averageDriveMinutes = 110,
            maxSlopeDegrees = 24,
            representativeElevationMeters = 1650,
            hairpinCount = 7,
            soilAndGeology = "Granitic ridge & terraced shola catchments with wider shoulders",
            baselineTerrainScore = 17,
            historicalLandslideCount = 3,
            mapPolyline = listOf(
                0.85f to 0.79f,
                0.81f to 0.64f,
                0.78f to 0.54f,
                0.74f to 0.44f,
                0.60f to 0.46f,
                0.45f to 0.48f
            ),
            geoPolyline = listOf(
                GeoPointLatLng(11.2994, 76.9355),
                GeoPointLatLng(11.3640, 76.9055),
                GeoPointLatLng(11.4216, 76.8617),
                GeoPointLatLng(11.4045, 76.7420),
                GeoPointLatLng(11.4102, 76.6950)
            ),
            alternateCorridorIds = listOf("coimbatore_ooty", "mettupalayam_coonoor", "coonoor_ooty"),
            monitoredSpots = listOf(
                MonitoredSpot(
                    id = "spot_kunjapanai",
                    name = "Kunjapanai Ghat Curve",
                    corridorId = "mettupalayam_kotagiri_ooty",
                    elevationMeters = 1260,
                    slopeDegrees = 24,
                    latitude = 11.3640,
                    longitude = 76.9055,
                    mapX = 0.80f,
                    mapY = 0.60f,
                    hazardNote = "Primary alternate corridor when NH-181 Burliar is restricted"
                ),
                MonitoredSpot(
                    id = "spot_doddabetta_east",
                    name = "Doddabetta – Snowdon Saddle",
                    corridorId = "mettupalayam_kotagiri_ooty",
                    elevationMeters = 2290,
                    slopeDegrees = 19,
                    latitude = 11.4045,
                    longitude = 76.7420,
                    mapX = 0.58f,
                    mapY = 0.46f,
                    hazardNote = "Fog & surface runoff monitoring section near ridge"
                )
            )
        ),
        MonitoredCorridor(
            id = "kalhatti_ghat",
            name = "Ooty – Kalhatti – Masinagudi",
            highwayCode = "Sigur Ghat",
            fromLocationId = "ooty",
            toLocationId = "masinagudi",
            distanceKm = 32,
            averageDriveMinutes = 65,
            maxSlopeDegrees = 35,
            representativeElevationMeters = 1610,
            hairpinCount = 36,
            soilAndGeology = "Very steep escarpment, fractured bedrock & thin loose talus",
            baselineTerrainScore = 29,
            historicalLandslideCount = 7,
            mapPolyline = listOf(
                0.45f to 0.48f,
                0.43f to 0.37f,
                0.39f to 0.27f,
                0.36f to 0.16f
            ),
            geoPolyline = listOf(
                GeoPointLatLng(11.4102, 76.6950),
                GeoPointLatLng(11.4820, 76.6780),
                GeoPointLatLng(11.5310, 76.6600),
                GeoPointLatLng(11.5734, 76.6442)
            ),
            alternateCorridorIds = listOf("gudalur_ooty"),
            monitoredSpots = listOf(
                MonitoredSpot(
                    id = "spot_kalhatti_falls",
                    name = "Kalhatti 36-Hairpin Escarpment",
                    corridorId = "kalhatti_ghat",
                    elevationMeters = 1610,
                    slopeDegrees = 35,
                    latitude = 11.4820,
                    longitude = 76.6780,
                    mapX = 0.41f,
                    mapY = 0.31f,
                    hazardNote = "Extreme gradient (36 tight hairpins); uphill/downhill restrictions often enforced"
                )
            )
        ),
        MonitoredCorridor(
            id = "gudalur_ooty",
            name = "Gudalur – Naduvattam – Ooty",
            highwayCode = "NH-181 West",
            fromLocationId = "gudalur",
            toLocationId = "ooty",
            distanceKm = 51,
            averageDriveMinutes = 95,
            maxSlopeDegrees = 25,
            representativeElevationMeters = 1780,
            hairpinCount = 8,
            soilAndGeology = "Deep humic forest clay exposed to intense southwest monsoon rains",
            baselineTerrainScore = 22,
            historicalLandslideCount = 6,
            mapPolyline = listOf(
                0.14f to 0.28f,
                0.23f to 0.34f,
                0.34f to 0.41f,
                0.45f to 0.48f
            ),
            geoPolyline = listOf(
                GeoPointLatLng(11.5030, 76.4918),
                GeoPointLatLng(11.4810, 76.5515),
                GeoPointLatLng(11.4480, 76.6090),
                GeoPointLatLng(11.4102, 76.6950)
            ),
            alternateCorridorIds = listOf("kalhatti_ghat"),
            monitoredSpots = listOf(
                MonitoredSpot(
                    id = "spot_naduvattam",
                    name = "Naduvattam – Frog Hill Cut",
                    corridorId = "gudalur_ooty",
                    elevationMeters = 1860,
                    slopeDegrees = 25,
                    latitude = 11.4810,
                    longitude = 76.5515,
                    mapX = 0.28f,
                    mapY = 0.37f,
                    hazardNote = "Southwest monsoon saturation & tree-fall slope slip zone"
                )
            )
        )
    )

    val simulatedHistoricalRecords: List<HistoricalLandslideRecord> = listOf(
        HistoricalLandslideRecord(
            id = "hist_0",
            corridorId = "coimbatore_ooty",
            year = 2009,
            locationLabel = "Coimbatore–Ooty NH-181 Marappalam Ghat [SIMULATED DEMO]",
            triggerSummary = "Cloudburst rains (>200mm in 48h) blocked the Coimbatore–Mettupalayam–Coonoor ascent near Burliar.",
            severity = RiskLevel.HIGH
        ),
        HistoricalLandslideRecord(
            id = "hist_1",
            corridorId = "mettupalayam_coonoor",
            year = 2009,
            locationLabel = "Marappalam – Burliar Stretch (NH-181) [SIMULATED DEMO]",
            triggerSummary = "Extreme >200mm 48h northeast monsoon cloudburst triggered deep gneiss debris slides.",
            severity = RiskLevel.HIGH
        ),
        HistoricalLandslideRecord(
            id = "hist_2",
            corridorId = "mettupalayam_coonoor",
            year = 2021,
            locationLabel = "Kallar Hairpin Bend 5 [SIMULATED DEMO]",
            triggerSummary = "Sustained 72h rainfall saturated cut-slope strata leading to boulder rockfall.",
            severity = RiskLevel.HIGH
        ),
        HistoricalLandslideRecord(
            id = "hist_3",
            corridorId = "kalhatti_ghat",
            year = 2019,
            locationLabel = "Kalhatti Hairpin 18 Culvert [SIMULATED DEMO]",
            triggerSummary = "Flash runoff overwhelmed hill drainage channels on 34° escarpment.",
            severity = RiskLevel.MEDIUM
        ),
        HistoricalLandslideRecord(
            id = "hist_4",
            corridorId = "gudalur_ooty",
            year = 2020,
            locationLabel = "Naduvattam – Pykara Approach [SIMULATED DEMO]",
            triggerSummary = "Southwest monsoon soil saturation caused embankment slip and fallen trees.",
            severity = RiskLevel.MEDIUM
        ),
        HistoricalLandslideRecord(
            id = "hist_5",
            corridorId = "coonoor_ooty",
            year = 2023,
            locationLabel = "Ketti Valley Retaining Wall [SIMULATED DEMO]",
            triggerSummary = "Localized toe erosion and retaining wall bulge after continuous drizzle.",
            severity = RiskLevel.LOW
        ),
        HistoricalLandslideRecord(
            id = "hist_6",
            corridorId = "mettupalayam_kotagiri_ooty",
            year = 2022,
            locationLabel = "Kunjapanai Valley Curve [SIMULATED DEMO]",
            triggerSummary = "Minor mudslip cleared within 4 hours; drainage later reinforced.",
            severity = RiskLevel.LOW
        )
    )

    val demoScenarios: List<DemoScenarioPreset> = listOf(
        DemoScenarioPreset(
            id = "demo_cloudburst",
            title = "2009 Monsoon Cloudburst [DEMO]",
            description = "High-intensity ghat storm (28.5 mm/h over 48h, saturated soil 0.45 m³/m³). Non-live demonstration replay.",
            rainfallIntensityMmHr = 28.5,
            rainfallDurationHours = 48.0,
            soilMoistureM3M3 = 0.45
        ),
        DemoScenarioPreset(
            id = "demo_moderate",
            title = "Sustained Monsoon Drizzle [DEMO]",
            description = "Moderate rain (8.4 mm/h over 22h, soil moisture 0.32 m³/m³). Non-live demonstration replay.",
            rainfallIntensityMmHr = 8.4,
            rainfallDurationHours = 22.0,
            soilMoistureM3M3 = 0.32
        ),
        DemoScenarioPreset(
            id = "demo_missing_soil",
            title = "Heavy Rain (Missing Soil Sensor) [DEMO]",
            description = "Tests missing soil moisture handling during 16.0 mm/h storm (30h). Non-live demonstration replay.",
            rainfallIntensityMmHr = 16.0,
            rainfallDurationHours = 30.0,
            soilMoistureM3M3 = null
        )
    )

    fun locationById(id: String): NilgirisLocation =
        locations.firstOrNull { it.id == id } ?: locations.first()

    fun corridorById(id: String): MonitoredCorridor =
        corridors.firstOrNull { it.id == id } ?: corridors.first()

    fun findCorridorsBetween(fromId: String, toId: String): Pair<MonitoredCorridor, List<MonitoredCorridor>> {
        if (fromId == toId) {
            val primary = corridorById("coimbatore_ooty")
            val alts = listOf(corridorById("mettupalayam_kotagiri_ooty"), corridorById("mettupalayam_coonoor"))
            return primary to alts
        }
        val isCbeOoty = (fromId == "coimbatore" && toId == "ooty") || (fromId == "ooty" && toId == "coimbatore")
        if (isCbeOoty) {
            val primary = corridorById("coimbatore_ooty")
            val alts = listOf(
                corridorById("mettupalayam_kotagiri_ooty"),
                corridorById("mettupalayam_coonoor"),
                corridorById("coonoor_ooty")
            )
            return primary to alts
        }
        val isMtpOoty = (fromId == "mettupalayam" && toId == "ooty") || (fromId == "ooty" && toId == "mettupalayam")
        if (isMtpOoty) {
            val primary = corridorById("mettupalayam_coonoor")
            val alts = listOf(corridorById("mettupalayam_kotagiri_ooty"), corridorById("coonoor_ooty"))
            return primary to alts
        }

        val direct = corridors.firstOrNull {
            (it.fromLocationId == fromId && it.toLocationId == toId) ||
                (it.fromLocationId == toId && it.toLocationId == fromId)
        }
        if (direct != null) {
            val alts = direct.alternateCorridorIds.mapNotNull { altId ->
                corridors.firstOrNull { it.id == altId }
            }
            return direct to alts
        }

        val matchingEndpoint = corridors.firstOrNull {
            it.fromLocationId == fromId || it.toLocationId == fromId ||
                it.fromLocationId == toId || it.toLocationId == toId
        } ?: corridors.first()
        val alts = corridors.filter { it.id != matchingEndpoint.id }.take(2)
        return matchingEndpoint to alts
    }
}
