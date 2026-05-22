"""
train_classifier.py — Entrena un clasificador Random Forest para letras estáticas (1 mano).

Genera un único modelo:
  - model_letters.p → todas las letras (126 features)

Flujo:
    1. python create_dataset_letters.py   → genera data.pickle
    2. python train_classifier.py         → genera model_letters.p
"""

import pickle
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import accuracy_score, classification_report


def train_model(data, labels_str):
    """Entrena y evalúa el modelo RF con todas las muestras."""
    print(f"\n{'='*50}")
    print(f"Entrenando: model_letters")
    print(f"Muestras: {len(data)}  |  Clases únicas: {sorted(set(labels_str))}")

    data_arr = np.asarray(data)

    # Verificar consistencia de features
    feature_sizes = {len(d) for d in data}
    if len(feature_sizes) > 1:
        raise ValueError(f"Inconsistencia en features: tamaños encontrados {feature_sizes}")
    print(f"Features por muestra: {feature_sizes.pop()}")

    unique = sorted(set(labels_str))
    local_map = {l: i for i, l in enumerate(unique)}
    local_labels = np.array([local_map[l] for l in labels_str])

    x_train, x_test, y_train, y_test = train_test_split(
        data_arr, local_labels,
        test_size=0.2, shuffle=True,
        stratify=local_labels, random_state=42,
    )

    model = RandomForestClassifier(
        n_estimators=200,
        max_depth=None,
        min_samples_leaf=2,
        random_state=42,
        n_jobs=-1,
    )
    model.fit(x_train, y_train)

    y_pred = model.predict(x_test)
    acc = accuracy_score(y_test, y_pred)
    print(f"\nAccuracy en test: {acc * 100:.2f}%")

    cv_scores = cross_val_score(model, data_arr, local_labels, cv=5,
                                scoring='accuracy', n_jobs=-1)
    print(f"Cross-val (5 folds): {cv_scores.mean()*100:.2f}% ± {cv_scores.std()*100:.2f}%")

    print("\nReporte por clase:")
    print(classification_report(y_test, y_pred, target_names=unique))

    output_file = "model_letters.p"
    with open(output_file, 'wb') as f:
        pickle.dump({
            'model': model,
            'classes': unique,
            'local_map': local_map,
            'feature_size': data_arr.shape[1],
        }, f)

    print(f"✓ Guardado: '{output_file}'")
    return model


def main():
    print("=== Entrenamiento de clasificador de letras (1 mano) ===\n")

    with open('data.pickle', 'rb') as f:
        dataset = pickle.load(f)

    data       = dataset['data']
    labels_str = dataset['labels_str']

    train_model(data, labels_str)

    print("\n✓ Entrenamiento completado.")
    print("\nEl modelo 'model_letters.p' ya está listo para usar.")


if __name__ == '__main__':
    main()