import os
import pandas as pd
import mediapipe as mp
from helpers import create_folder, get_keypoints, insert_keypoints_sequence
from constants import *


def get_sample_count_in_hdf(hdf_path):
    '''Retorna cuántas muestras hay guardadas en el .h5, o 0 si no existe.'''
    if not os.path.exists(hdf_path):
        return 0
    try:
        data = pd.read_hdf(hdf_path, key='data')
        return data['sample'].nunique()
    except Exception:
        return 0


def get_sample_count_in_folder(frames_path):
    '''Retorna cuántas carpetas sample_xxx hay en la carpeta de la seña.'''
    if not os.path.exists(frames_path):
        return 0
    return len([
        s for s in os.listdir(frames_path)
        if os.path.isdir(os.path.join(frames_path, s))
    ])


def create_keypoints(word_id, words_path, hdf_path):
    '''
    Crea o actualiza los keypoints de una seña.
    Solo procesa si hay muestras nuevas respecto al .h5 existente.
    '''
    frames_path = os.path.join(words_path, word_id)
    samples_in_folder = get_sample_count_in_folder(frames_path)
    samples_in_hdf = get_sample_count_in_hdf(hdf_path)

    if samples_in_folder == 0:
        print(f'"{word_id}" — sin muestras, saltando.')
        return

    if samples_in_hdf == samples_in_folder:
        print(f'"{word_id}" — sin cambios ({samples_in_folder} muestras), saltando.')
        return

    if samples_in_hdf > samples_in_folder:
        print(f'"{word_id}" — el .h5 tiene más muestras que la carpeta, regenerando...')

    if samples_in_hdf < samples_in_folder:
        print(f'"{word_id}" — {samples_in_folder - samples_in_hdf} muestras nuevas, actualizando...')

    # Procesar
    data = pd.DataFrame([])

    with mp.solutions.hands.Hands(
        static_image_mode=True,
        max_num_hands=2,
        min_detection_confidence=0.3,
    ) as hands_model:
        sample_list = [s for s in os.listdir(frames_path)
                       if os.path.isdir(os.path.join(frames_path, s))]
        sample_count = len(sample_list)

        for n_sample, sample_name in enumerate(sample_list, start=1):
            sample_path = os.path.join(frames_path, sample_name)
            keypoints_sequence = get_keypoints(hands_model, sample_path)
            data = insert_keypoints_sequence(data, n_sample, keypoints_sequence)
            print(f"  {n_sample}/{sample_count}", end="\r")

    data.to_hdf(hdf_path, key="data", mode="w")
    print(f'"{word_id}" — keypoints creados! ({sample_count} muestras)')


if __name__ == "__main__":
    create_folder(KEYPOINTS_PATH)

    # GENERAR TODAS LAS PALABRAS
    word_ids = [word for word in os.listdir(FRAME_ACTIONS_PATH)
                if os.path.isdir(os.path.join(FRAME_ACTIONS_PATH, word))]

    # GENERAR PARA UNA PALABRA — descomenta y edita:
    # word_ids = ["hola-der"]

    print(f"=== Verificando {len(word_ids)} señas ===\n")
    for word_id in word_ids:
        hdf_path = os.path.join(KEYPOINTS_PATH, f"{word_id}.h5")
        create_keypoints(word_id, FRAME_ACTIONS_PATH, hdf_path)

    print("\n✓ Listo.")