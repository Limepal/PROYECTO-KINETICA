import os
import pandas as pd
from mediapipe.python.solutions.holistic import Holistic
from helpers import create_folder, get_keypoints, insert_keypoints_sequence
from constants import *


def create_keypoints(word_id, words_path, hdf_path):
    '''
    Recorre la carpeta de frames de la palabra y guarda sus keypoints en `hdf_path`.
    '''
    data = pd.DataFrame([])
    frames_path = os.path.join(words_path, word_id)

    with Holistic() as holistic:
        print(f'Creando keypoints de "{word_id}"...')
        sample_list = os.listdir(frames_path)
        sample_count = len(sample_list)

        for n_sample, sample_name in enumerate(sample_list, start=1):
            sample_path = os.path.join(frames_path, sample_name)
            keypoints_sequence = get_keypoints(holistic, sample_path)
            data = insert_keypoints_sequence(data, n_sample, keypoints_sequence)
            print(f"{n_sample}/{sample_count}", end="\r")

    data.to_hdf(hdf_path, key="data", mode="w")
    print(f"Keypoints creados! ({sample_count} muestras)")


if __name__ == "__main__":
    create_folder(KEYPOINTS_PATH)

    # GENERAR TODAS LAS PALABRAS
    word_ids = [word for word in os.listdir(FRAME_ACTIONS_PATH)]

    # GENERAR PARA UNA PALABRA O CONJUNTO — descomenta y edita:
    # word_ids = ["hola-der"]

    for word_id in word_ids:
        hdf_path = os.path.join(KEYPOINTS_PATH, f"{word_id}.h5")
        create_keypoints(word_id, FRAME_ACTIONS_PATH, hdf_path)
