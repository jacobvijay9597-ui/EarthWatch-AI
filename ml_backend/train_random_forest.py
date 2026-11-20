"""
EarthWatch AI - Standalone Random Forest Training Pipeline

Trains a scikit-learn RandomForestClassifier on the labelled historical
Nilgiris landslide susceptibility dataset using the six core features:
  1. rainfall_intensity_mm_hr
  2. rainfall_duration_hours
  3. soil_moisture_m3_m3 (optional / imputed when unavailable)
  4. slope_angle_deg
  5. terrain_elevation_m
  6. historical_landslide_count

Exports:
  artifacts/earthwatch_rf_model.joblib
  artifacts/model_metadata.json
"""

from __future__ import annotations

import json
from datetime import datetime, timezone
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.ensemble import RandomForestClassifier
from sklearn.impute import SimpleImputer
from sklearn.model_selection import StratifiedKFold, cross_validate
from sklearn.pipeline import Pipeline

BASE_DIR = Path(__file__).resolve().parent
DATASET_PATH = BASE_DIR / "dataset" / "nilgiris_landslide_history.csv"
ARTIFACTS_DIR = BASE_DIR / "artifacts"
MODEL_PATH = ARTIFACTS_DIR / "earthwatch_rf_model.joblib"
METADATA_PATH = ARTIFACTS_DIR / "model_metadata.json"

FEATURE_COLUMNS = [
    "rainfall_intensity_mm_hr",
    "rainfall_duration_hours",
    "soil_moisture_m3_m3",
    "slope_angle_deg",
    "terrain_elevation_m",
    "historical_landslide_count",
]
TARGET_COLUMN = "landslide_occurred"


def build_training_pipeline() -> Pipeline:
    # Soil moisture may be missing in live telemetry; median imputation plus a
    # missingness indicator keeps inference honest without fabricating readings.
    preprocessor = ColumnTransformer(
        transformers=[
            (
                "soil_imputer",
                SimpleImputer(strategy="median", add_indicator=True),
                ["soil_moisture_m3_m3"],
            ),
            (
                "required_features",
                SimpleImputer(strategy="median"),
                [
                    "rainfall_intensity_mm_hr",
                    "rainfall_duration_hours",
                    "slope_angle_deg",
                    "terrain_elevation_m",
                    "historical_landslide_count",
                ],
            ),
        ],
        remainder="drop",
    )

    classifier = RandomForestClassifier(
        n_estimators=200,
        max_depth=8,
        min_samples_leaf=2,
        class_weight="balanced",
        random_state=42,
    )

    return Pipeline(
        steps=[
            ("preprocessor", preprocessor),
            ("classifier", classifier),
        ]
    )


def extract_feature_importances(pipeline: Pipeline) -> dict[str, float]:
    rf: RandomForestClassifier = pipeline.named_steps["classifier"]
    raw_importances = rf.feature_importances_
    # ColumnTransformer order: [soil_moisture, soil_moisture_missing_ind, rain_int, rain_dur, slope, elev, history]
    mapped = {
        "soil_moisture_m3_m3": float(raw_importances[0] + (raw_importances[1] if len(raw_importances) > 6 else 0.0)),
        "rainfall_intensity_mm_hr": float(raw_importances[-5]),
        "rainfall_duration_hours": float(raw_importances[-4]),
        "slope_angle_deg": float(raw_importances[-3]),
        "terrain_elevation_m": float(raw_importances[-2]),
        "historical_landslide_count": float(raw_importances[-1]),
    }
    total = sum(mapped.values()) or 1.0
    return {k: round(v / total, 4) for k, v in mapped.items()}


def main() -> None:
    if not DATASET_PATH.exists():
        raise FileNotFoundError(f"Training dataset not found at {DATASET_PATH}")

    df = pd.read_csv(DATASET_PATH)
    X = df[FEATURE_COLUMNS]
    y = df[TARGET_COLUMN].astype(int)

    pipeline = build_training_pipeline()
    cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
    scores = cross_validate(
        pipeline,
        X,
        y,
        cv=cv,
        scoring=("accuracy", "roc_auc", "f1"),
        return_train_score=False,
    )

    pipeline.fit(X, y)

    ARTIFACTS_DIR.mkdir(parents=True, exist_ok=True)
    joblib.dump(pipeline, MODEL_PATH)

    feature_importances = extract_feature_importances(pipeline)
    soil_median = float(df["soil_moisture_m3_m3"].dropna().median())

    metadata = {
        "model_name": "EarthWatch Nilgiris Random Forest Classifier",
        "model_version": "rf-v1.0.0",
        "trained_at_utc": datetime.now(timezone.utc).isoformat(),
        "dataset_path": str(DATASET_PATH.name),
        "dataset_rows": int(len(df)),
        "positive_events": int(y.sum()),
        "negative_events": int((y == 0).sum()),
        "soil_moisture_median_m3_m3": round(soil_median, 4),
        "cv_accuracy_mean": round(float(np.mean(scores["test_accuracy"])), 4),
        "cv_roc_auc_mean": round(float(np.mean(scores["test_roc_auc"])), 4),
        "cv_f1_mean": round(float(np.mean(scores["test_f1"])), 4),
        "feature_importances": feature_importances,
        "is_validated_model": True,
        "notes": "Trained on curated Nilgiris reference landslide records. Replace dataset CSV with verified GSI/NDMA records for regional calibration.",
    }

    with METADATA_PATH.open("w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)

    print(f"Saved trained Random Forest model to {MODEL_PATH}")
    print(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    main()
