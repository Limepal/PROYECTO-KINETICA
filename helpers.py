import json
import os
import cv2
import mediapipe as mp
import numpy as np
import pandas as pd
from typing import NamedTuple
from constants import *

# Inicializar MediaPipe Hands una sola vez
mp_hands = mp.solutions.hands
mp_drawing = mp.solutions.drawing_utils
mp_drawing_styles = mp.solutions.drawing_styles


# GENERAL
def mediapipe_detection(image, model):
    image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    image.flags.writeable = False
    results = model.process(image)
    image.flags.writeable = True
    return results

def create_folder(path):
    if not os.path.exists(path):
        os.makedirs(path)

def there_hand(results) -> bool:
    return bool(results.multi_hand_landmarks)

def get_word_ids(path):
    with open(path, 'r') as json_file:
        data = json.load(json_file)
        return data.get('word_ids')


# CAPTURE SAMPLES
def draw_keypoints(image, results):
    '''Dibuja los keypoints de las manos en la imagen.'''
    if results.multi_hand_landmarks:
        for hand_landmarks in results.multi_hand_landmarks:
            mp_drawing.draw_landmarks(
                image,
                hand_landmarks,
                mp_hands.HAND_CONNECTIONS,
                mp_drawing_styles.get_default_hand_landmarks_style(),
                mp_drawing_styles.get_default_hand_connections_style(),
            )

def save_frames(frames, output_folder):
    for num_frame, frame in enumerate(frames):
        frame_path = os.path.join(output_folder, f"{num_frame + 1}.jpg")
        cv2.imwrite(frame_path, frame)


# EXTRACT KEYPOINTS — solo manos (84 valores: 21 landmarks x 2 coords x 2 manos)
def extract_keypoints(results):
    '''
    Extrae keypoints de mano izquierda y derecha.
    Si una mano no está presente, rellena con ceros.
    Total: 21*3 + 21*3 = 126 valores (x, y, z por landmark)
    '''
    lh = np.zeros(21 * 3)
    rh = np.zeros(21 * 3)

    if results.multi_hand_landmarks and results.multi_handedness:
        for hand_landmarks, handedness in zip(results.multi_hand_landmarks, results.multi_handedness):
            label = handedness.classification[0].label  # 'Left' o 'Right'
            kp = np.array([[lm.x, lm.y, lm.z] for lm in hand_landmarks.landmark]).flatten()
            if label == 'Left':
                lh = kp
            else:
                rh = kp

    return np.concatenate([lh, rh])


def get_keypoints(model, sample_path):
    '''Retorna la secuencia de keypoints de una muestra.'''
    kp_seq = np.array([])
    for img_name in sorted(os.listdir(sample_path)):
        img_path = os.path.join(sample_path, img_name)
        frame = cv2.imread(img_path)
        if frame is None:
            continue
        results = mediapipe_detection(frame, model)
        kp_frame = extract_keypoints(results)
        kp_seq = np.concatenate([kp_seq, [kp_frame]] if kp_seq.size > 0 else [[kp_frame]])
    return kp_seq

def insert_keypoints_sequence(df, n_sample: int, kp_seq):
    for frame, keypoints in enumerate(kp_seq):
        data = {'sample': n_sample, 'frame': frame + 1, 'keypoints': [keypoints]}
        df_keypoints = pd.DataFrame(data)
        df = pd.concat([df, df_keypoints])
    return df


# TRAINING MODEL
def get_sequences_and_labels(words_id):
    sequences, labels = [], []
    for word_index, word_id in enumerate(words_id):
        hdf_path = os.path.join(KEYPOINTS_PATH, f"{word_id}.h5")
        data = pd.read_hdf(hdf_path, key='data')
        for _, df_sample in data.groupby('sample'):
            seq_keypoints = [fila['keypoints'] for _, fila in df_sample.iterrows()]
            sequences.append(seq_keypoints)
            labels.append(word_index)
    return sequences, labels