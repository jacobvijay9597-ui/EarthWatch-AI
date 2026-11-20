"""
EarthWatch AI - Standalone Python ML Inference API (FastAPI)

Connects the separately trained scikit-learn Random Forest model to the
EarthWatch AI mobile frontend over HTTP JSON. Keeps ML model artifacts and
feature engineering cleanly decoupled from the Android application.
"""

from __future__ import annotations

import json
import os
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional

import joblib
import pandas as pd
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from sqlalchemy import Column, DateTime, Float, Integer, String, create_engine
from sqlalchemy.orm import declarative_base, sessionmaker

BASE_DIR = Path(__file__).resolve().parent
ARTIFACTS_DIR = BASE_DIR / "artifacts"
MODEL_PATH = ARTIFACTS_DIR / "earthwatch_rf_model.joblib"
METADATA_PATH = ARTIFACTS_DIR / "model_metadata.json"
DEFAULT_SQLITE_URL = f"sqlite:///{BASE_DIR / 'earthwatch_ml.db'}"
DATABASE_URL = os.getenv("DATABASE_URL", DEFAULT_SQLITE_URL)

Base = declarative_base()


class PredictionAuditLog(Base):
    __tablename__ = "prediction_audit_logs"

    id = Column(Integer, primary_key=True, autoincrement=True)
    corridor_id = Column(String(64), nullable=False, index=True)
    corridor_name = Column(String(128), nullable=False)
    probability = Column(Float, nullable=True)
    risk_category = Column(String(32), nullable=False)
    rainfall_intensity_mm_hr = Column(Float, nullable=True)
    rainfall_duration_hours = Column(Float, nullable=True)
    soil_moisture_m3_m3 = Column(Float, nullable=True)
    slope_angle_deg = Column(Float, nullable=True)
    freshness_status = Column(String(32), nullable=False)
    created_at = Column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc), nullable=False)


_engine_kwargs = {"connect_args": {"check_same_thread": False}} if DATABASE_URL.startswith("sqlite") else {"pool_pre_ping": True}
sql_engine = create_engine(DATABASE_URL, **_engine_kwargs)
SessionLocal = sessionmaker(bind=sql_engine, autoflush=False, autocommit=False)
Base.metadata.create_all(bind=sql_engine)

FEATURE_META = {
    "rainfall_intensity_mm_hr": ("Rainfall Intensity", "mm/h", 8.0),
    "rainfall_duration_hours": ("Rainfall Duration", "h", 18.0),
    "soil_moisture_m3_m3": ("Soil Moisture (0–7cm)", "m³/m³", 0.30),
    "slope_angle_deg": ("Slope Angle", "°", 22.0),
    "terrain_elevation_m": ("Terrain Elevation", "m", 1200.0),
    "historical_landslide_count": ("Historical Landslide Records", "events", 4.0),
}

app = FastAPI(
    title="EarthWatch AI Landslide Random Forest API",
    version="1.0.0",
    description="Separately trained Python Random Forest inference backend for Nilgiris landslide probability estimation.",
)

_loaded_model = None
_loaded_metadata: dict = {}


def load_model_bundle() -> None:
    global _loaded_model, _loaded_metadata
    if MODEL_PATH.exists():
        _loaded_model = joblib.load(MODEL_PATH)
    if METADATA_PATH.exists():
        with METADATA_PATH.open("r", encoding="utf-8") as f:
            _loaded_metadata = json.load(f)


load_model_bundle()


class LandslidePredictionRequest(BaseModel):
    corridor_id: str
    corridor_name: str
    rainfall_intensity_mm_hr: Optional[float] = Field(default=None, ge=0.0)
    rainfall_duration_hours: Optional[float] = Field(default=None, ge=0.0)
    rainfall_24h_mm: Optional[float] = Field(default=None, ge=0.0)
    rainfall_72h_mm: Optional[float] = Field(default=None, ge=0.0)
    soil_moisture_m3_m3: Optional[float] = Field(default=None, ge=0.0, le=1.0)
    slope_angle_deg: Optional[float] = Field(default=None, ge=0.0, le=90.0)
    terrain_elevation_meters: Optional[int] = Field(default=None, ge=0)
    historical_landslide_count: Optional[int] = Field(default=None, ge=0)
    telemetry_timestamp_iso: Optional[str] = None
    telemetry_age_minutes: Optional[int] = Field(default=None, ge=0)


class FeatureContributionDto(BaseModel):
    feature_name: str
    importance_weight: float
    observed_value_label: str
    impact_direction: str


class LandslidePredictionResponse(BaseModel):
    corridor_id: str
    corridor_name: str
    estimated_landslide_probability: Optional[float]
    risk_category: str
    contributing_factors: list[FeatureContributionDto]
    model_name: str
    model_version: str
    is_validated_model: bool
    execution_mode: str
    data_freshness_status: str
    freshness_warning: str
    missing_data_warnings: list[str]
    explanation_note: str


@app.get("/health")
def health() -> dict:
    db_backend = "postgresql" if "postgres" in DATABASE_URL.lower() else "sqlite"
    return {
        "status": "ok" if _loaded_model is not None else "model_missing",
        "model_loaded": _loaded_model is not None,
        "is_validated_model": bool(_loaded_metadata.get("is_validated_model", False)),
        "model_name": _loaded_metadata.get("model_name", "Not trained"),
        "model_version": _loaded_metadata.get("model_version", "unavailable"),
        "dataset_rows": _loaded_metadata.get("dataset_rows", 0),
        "cv_accuracy_mean": _loaded_metadata.get("cv_accuracy_mean", 0.0),
        "sql_backend": db_backend,
    }


@app.get("/api/v1/predictions/history")
def prediction_history(limit: int = 20) -> list[dict]:
    safe_limit = max(1, min(limit, 100))
    with SessionLocal() as session:
        rows = (
            session.query(PredictionAuditLog)
            .order_by(PredictionAuditLog.id.desc())
            .limit(safe_limit)
            .all()
        )
        return [
            {
                "id": r.id,
                "corridor_id": r.corridor_id,
                "corridor_name": r.corridor_name,
                "probability": r.probability,
                "risk_category": r.risk_category,
                "freshness_status": r.freshness_status,
                "created_at": r.created_at.isoformat() if r.created_at else None,
            }
            for r in rows
        ]


def classify_risk(probability: float) -> str:
    if probability >= 0.65:
        return "HIGH"
    if probability >= 0.35:
        return "MEDIUM"
    return "LOW"


def evaluate_freshness(age_minutes: Optional[int], timestamp_iso: Optional[str]) -> tuple[str, str]:
    if age_minutes is None:
        return "MISSING", "Telemetry timestamp unavailable; data freshness cannot be confirmed."
    if age_minutes > 120:
        return "STALE", f"Weather telemetry is {age_minutes} minutes old ({timestamp_iso or 'unknown'}). Refresh before travel."
    if age_minutes > 45:
        return "AGING", f"Weather telemetry synced {age_minutes} minutes ago; moderate lag for rapid ghat cloudbursts."
    return "FRESH", f"Live weather telemetry synced {age_minutes} minutes ago ({timestamp_iso or 'recent'})."


@app.post("/api/v1/predict/landslide-risk", response_model=LandslidePredictionResponse)
def predict_landslide_risk(req: LandslidePredictionRequest) -> LandslidePredictionResponse:
    if _loaded_model is None:
        raise HTTPException(
            status_code=503,
            detail="Trained Random Forest artifact missing. Run train_random_forest.py first.",
        )

    freshness_status, freshness_warning = evaluate_freshness(
        req.telemetry_age_minutes, req.telemetry_timestamp_iso
    )

    missing_warnings: list[str] = []
    if req.rainfall_intensity_mm_hr is None or req.rainfall_duration_hours is None:
        missing_warnings.append("Missing live rainfall intensity or rainfall duration; cannot run reliable prediction.")
    if req.soil_moisture_m3_m3 is None:
        missing_warnings.append("Soil moisture reading unavailable; Random Forest median imputation applied (confidence reduced).")
    if req.slope_angle_deg is None or req.terrain_elevation_meters is None:
        missing_warnings.append("Missing terrain slope or elevation metadata.")
    if req.historical_landslide_count is None:
        missing_warnings.append("Historical landslide count unavailable.")

    if (
        req.rainfall_intensity_mm_hr is None
        or req.rainfall_duration_hours is None
        or req.slope_angle_deg is None
        or req.terrain_elevation_meters is None
        or req.historical_landslide_count is None
    ):
        return LandslidePredictionResponse(
            corridor_id=req.corridor_id,
            corridor_name=req.corridor_name,
            estimated_landslide_probability=None,
            risk_category="UNCONFIRMED",
            contributing_factors=[],
            model_name=_loaded_metadata.get("model_name", "EarthWatch Random Forest"),
            model_version=_loaded_metadata.get("model_version", "rf-v1.0.0"),
            is_validated_model=False,
            execution_mode="DEMONSTRATION_INSUFFICIENT_DATA",
            data_freshness_status=freshness_status,
            freshness_warning=freshness_warning,
            missing_data_warnings=missing_warnings,
            explanation_note="Required meteorological or terrain inputs are missing. Prediction withheld rather than fabricated.",
        )

    row = pd.DataFrame(
        [
            {
                "rainfall_intensity_mm_hr": req.rainfall_intensity_mm_hr,
                "rainfall_duration_hours": req.rainfall_duration_hours,
                "soil_moisture_m3_m3": req.soil_moisture_m3_m3,
                "slope_angle_deg": req.slope_angle_deg,
                "terrain_elevation_m": float(req.terrain_elevation_meters),
                "historical_landslide_count": float(req.historical_landslide_count),
            }
        ]
    )

    prob_slide = float(_loaded_model.predict_proba(row)[0][1])
    risk_category = classify_risk(prob_slide)

    importances: dict[str, float] = _loaded_metadata.get("feature_importances", {})
    values = {
        "rainfall_intensity_mm_hr": (req.rainfall_intensity_mm_hr, f"{req.rainfall_intensity_mm_hr:.1f} mm/h"),
        "rainfall_duration_hours": (req.rainfall_duration_hours, f"{req.rainfall_duration_hours:.0f} hrs (72h window)"),
        "soil_moisture_m3_m3": (
            req.soil_moisture_m3_m3,
            f"{req.soil_moisture_m3_m3:.3f} m³/m³" if req.soil_moisture_m3_m3 is not None else "Unavailable (Imputed)",
        ),
        "slope_angle_deg": (req.slope_angle_deg, f"{req.slope_angle_deg:.0f}° max gradient"),
        "terrain_elevation_m": (float(req.terrain_elevation_meters), f"{req.terrain_elevation_meters} m"),
        "historical_landslide_count": (float(req.historical_landslide_count), f"{req.historical_landslide_count} recorded events"),
    }

    contributions: list[FeatureContributionDto] = []
    for key, (label, _unit, threshold) in FEATURE_META.items():
        weight = float(importances.get(key, 1.0 / len(FEATURE_META)))
        numeric_val, display_val = values[key]
        if numeric_val is None:
            direction = "Missing (Imputed)"
        elif numeric_val >= threshold:
            direction = "Elevating Risk"
        else:
            direction = "Within Baseline"
        contributions.append(
            FeatureContributionDto(
                feature_name=label,
                importance_weight=round(weight, 4),
                observed_value_label=display_val,
                impact_direction=direction,
            )
        )

    contributions.sort(key=lambda c: c.importance_weight, reverse=True)

    rounded_prob = round(prob_slide, 4)
    try:
        with SessionLocal() as session:
            session.add(
                PredictionAuditLog(
                    corridor_id=req.corridor_id,
                    corridor_name=req.corridor_name,
                    probability=rounded_prob,
                    risk_category=risk_category,
                    rainfall_intensity_mm_hr=req.rainfall_intensity_mm_hr,
                    rainfall_duration_hours=req.rainfall_duration_hours,
                    soil_moisture_m3_m3=req.soil_moisture_m3_m3,
                    slope_angle_deg=req.slope_angle_deg,
                    freshness_status=freshness_status,
                )
            )
            session.commit()
    except Exception:
        pass

    return LandslidePredictionResponse(
        corridor_id=req.corridor_id,
        corridor_name=req.corridor_name,
        estimated_landslide_probability=rounded_prob,
        risk_category=risk_category,
        contributing_factors=contributions,
        model_name=_loaded_metadata.get("model_name", "EarthWatch Random Forest"),
        model_version=_loaded_metadata.get("model_version", "rf-v1.0.0"),
        is_validated_model=bool(_loaded_metadata.get("is_validated_model", True)),
        execution_mode="PYTHON_RANDOM_FOREST_API",
        data_freshness_status=freshness_status,
        freshness_warning=freshness_warning,
        missing_data_warnings=missing_warnings,
        explanation_note=(
            "Prediction generated by the external Python scikit-learn Random Forest classifier "
            "using current telemetry and terrain features."
        ),
    )
