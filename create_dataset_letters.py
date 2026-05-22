"""
create_dataset_letters.py — Genera data.pickle para entrenar el Random Forest de letras.

Lee los archivos .h5 de keypoints (los mismos que usa el LSTM) y extrae
UN solo vector de features por muestra (promedio de los frames), que es
lo que necesita el Random Forest para señas estáticas de UNA SOLA MANO.

Las señas de letras deben estar nombradas con el formato:
    a-izq, a-der, b-izq, b-der, ... z-izq, z-der

El label que se guarda es la letra base (ej: 'a', 'b', ...) sin el sufijo -izq/-der.

Uso:
    python create_dataset_letters.py
    python create_dataset_letters.py --words_json models/words.json  (solo las del json)
    python create_dataset_letters.py --all  (todas las .h5 en keypoints/)
"""

import os
import pickle
import argparse
import numpy as np
import pandas as pd
from constants import KEYPOINTS_PATH, WORDS_JSON_PATH
from helpers import get_word_ids

# ── Configuración ──────────────────────────────────────────────────────────────
# Estrategia de agregación por muestra:
#   'mean'   → promedio de todos los frames (buena para señas estáticas)
#   'median' → mediana (más robusta a frames con mano perdida)
#   'first'  → solo el primer frame
AGGREGATION = 'mean'
# ───────────────────────────────────────────────────────────────────────────────


def is_letter_word(word_id: str) -> bool:
    """Retorna True si el word_id corresponde a una letra (ej: 'a-izq', 'b-der')."""
    base = word_id.split('-')[0]
    return len(base) == 1 and base.isalpha()


def get_letter_label(word_id: str) -> str:
    """Extrae la letra base de un word_id (ej: 'a-izq' → 'a')."""
    return word_id.split('-')[0].lower()


def aggregate_sequence(keypoints_list: list, strategy: str = 'mean') -> np.ndarray:
    """
    Dado una lista de arrays de keypoints (uno por frame),
    retorna un único vector representativo de la muestra.
    """
    arr = np.array(keypoints_list)   # shape: (n_frames, 126)
    if strategy == 'mean':
        return arr.mean(axis=0)
    elif strategy == 'median':
        return np.median(arr, axis=0)
    elif strategy == 'first':
        return arr[0]
    else:
        raise ValueError(f"Estrategia desconocida: {strategy}")


def build_dataset(word_ids: list, aggregation: str = AGGREGATION):
    """
    Construye listas de features y labels para el RF.
    Solo procesa word_ids que correspondan a letras.
    """
    data, labels = [], []
    skipped = []

    letter_words = [w for w in word_ids if is_letter_word(w)]

    if not letter_words:
        print("⚠ No se encontraron señas de letras en la lista de word_ids.")
        print("  Asegúrate de que tengan formato 'a-izq', 'b-der', etc.")
        return [], []

    print(f"Procesando {len(letter_words)} señas de letras...\n")

    for word_id in sorted(letter_words):
        hdf_path = os.path.join(KEYPOINTS_PATH, f"{word_id}.h5")

        if not os.path.exists(hdf_path):
            print(f"  ⚠ '{word_id}' — .h5 no encontrado, saltando.")
            skipped.append(word_id)
            continue

        try:
            df = pd.read_hdf(hdf_path, key='data')
        except Exception as e:
            print(f"  ✗ '{word_id}' — error al leer .h5: {e}")
            skipped.append(word_id)
            continue

        label = get_letter_label(word_id)
        sample_count = 0

        for _, df_sample in df.groupby('sample'):
            keypoints_list = [row['keypoints'] for _, row in df_sample.iterrows()]
            if len(keypoints_list) == 0:
                continue

            feature_vec = aggregate_sequence(keypoints_list, aggregation)
            data.append(feature_vec)
            labels.append(label)
            sample_count += 1

        print(f"  ✓ '{word_id}' → label='{label}' | {sample_count} muestras")

    print(f"\nTotal: {len(data)} muestras | {len(set(labels))} clases únicas")
    if skipped:
        print(f"Saltados ({len(skipped)}): {skipped}")

    return data, labels


def main():
    parser = argparse.ArgumentParser(description="Genera data.pickle para Random Forest de letras")
    parser.add_argument('--all', action='store_true',
                        help='Usa todos los .h5 en keypoints/ en vez del words.json')
    parser.add_argument('--words_json', default=WORDS_JSON_PATH,
                        help=f'Ruta al words.json (default: {WORDS_JSON_PATH})')
    parser.add_argument('--output', default='data.pickle',
                        help='Ruta de salida del pickle (default: data.pickle)')
    parser.add_argument('--aggregation', default=AGGREGATION,
                        choices=['mean', 'median', 'first'],
                        help=f'Estrategia de agregación de frames (default: {AGGREGATION})')
    args = parser.parse_args()

    # Obtener lista de word_ids
    if args.all:
        word_ids = [
            os.path.splitext(f)[0]
            for f in os.listdir(KEYPOINTS_PATH)
            if f.endswith('.h5')
        ]
        print(f"Modo --all: {len(word_ids)} archivos .h5 encontrados")
    else:
        if not os.path.exists(args.words_json):
            print(f"✗ No se encontró {args.words_json}")
            print("  Usa --all para procesar todos los .h5, o --words_json para indicar otro path.")
            return
        word_ids = get_word_ids(args.words_json)
        print(f"Usando words.json: {len(word_ids)} señas totales")

    data, labels = build_dataset(word_ids, args.aggregation)

    if not data:
        print("✗ No se generaron datos. Revisa tus archivos .h5 y nombres de señas.")
        return

    # Construir mapa label → índice numérico para el RF
    unique_labels = sorted(set(labels))
    label_to_int = {l: i for i, l in enumerate(unique_labels)}
    labels_int = [label_to_int[l] for l in labels]

    dataset = {
        'data': data,
        'labels': labels_int,
        'labels_str': labels,
        'label_to_int': label_to_int,
        'int_to_label': {v: k for k, v in label_to_int.items()},
        'aggregation': args.aggregation,
    }

    with open(args.output, 'wb') as f:
        pickle.dump(dataset, f)

    print(f"\n✓ Dataset guardado en '{args.output}'")
    print(f"  Clases ({len(unique_labels)}): {unique_labels}")
    print(f"\nAhora corre:")
    print(f"  python train_classifier.py")


if __name__ == '__main__':
    main()