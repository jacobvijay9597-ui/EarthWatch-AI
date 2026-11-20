# EarthWatch AI — Python Random Forest ML Backend

This directory contains the decoupled Python machine learning training pipeline and FastAPI inference server for Nilgiris landslide susceptibility prediction.

## Features Analyzed
1. `rainfall_intensity_mm_hr` — Live rainfall rate (mm/h)
2. `rainfall_duration_hours` — Total wet hours (>= 0.2 mm/h) across the 72-hour window
3. `soil_moisture_m3_m3` — Surface soil moisture (0–7 cm), optional with median imputation + missingness indicator when unavailable
4. `slope_angle_deg` — Maximum corridor ghat gradient in degrees
5. `terrain_elevation_m` — Representative elevation above sea level (meters)
6. `historical_landslide_count` — Historical landslide event frequency for the corridor

## Quick Start

```bash
cd ml_backend
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

# 1. Train & validate the Random Forest classifier
python train_random_forest.py

# 2. Start the FastAPI prediction backend
uvicorn server:app --host 0.0.0.0 --port 8000
```

## Connecting From Android
- **Android Emulator:** `http://10.0.2.2:8000/`
- **Hosted Cloud Run / FastAPI URL:** `https://your-ml-service.run.app/`

If the Python Random Forest backend is unreachable or required live telemetry is unavailable, the Android app enters a clearly labelled **Demonstration Mode** and never presents fabricated telemetry as a validated forecast.

## SQL Audit Database (PostgreSQL / Cloud SQL or SQLite)
By default, `server.py` logs all Random Forest prediction runs to a local SQLite file (`earthwatch_ml.db`). To connect a managed **Cloud SQL for PostgreSQL** instance on Google Cloud, set `DATABASE_URL` before launching `uvicorn`:

```bash
export DATABASE_URL="postgresql+psycopg://USER:PASSWORD@HOST:5432/earthwatch"
uvicorn server:app --host 0.0.0.0 --port 8000
```

Query logged inference history via `GET /api/v1/predictions/history`.

