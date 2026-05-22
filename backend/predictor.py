"""
predictor.py
Versión PRODUCCIÓN/API del traductor LSP híbrido.

Este archivo:
- NO abre cámara
- NO usa cv2.imshow
- NO tiene while principal

Recibe frames desde:
- WebSocket
- FastAPI
- navegador

Uso:

predictor = SignPredictor()

result = predictor.process_frame(frame)

if result:
    print(result)
"""

import os
import pickle
import numpy as np
import tensorflow as tf
import mediapipe as mp

from keras.models import load_model

from helpers import (
    get_word_ids,
    mediapipe_detection,
    there_hand,
    extract_keypoints,
)

from constants import *


# ─────────────────────────────────────────────────────────────
# CONFIG
# ─────────────────────────────────────────────────────────────

THRESHOLD = 0.80
MARGIN_FRAME = 0
MIN_FRAMES = 15
STILLNESS_FRAMES = 10
STILLNESS_THRESHOLD = 0.08
COOLDOWN_FRAMES = 2
LOST_HAND_TOLERANCE = 6

RF_THRESHOLD = 0.60
STATIC_FRAMES = 5
STATIC_THRESHOLD = 0.04
RF_CONFIRM_FRAMES = 3

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MODEL_1HAND_PATH = os.path.join(BASE_DIR, "..", "model_1hand.p")
MODEL_2HANDS_PATH = os.path.join(BASE_DIR, "..", "model_2hands.p")


# ─────────────────────────────────────────────────────────────
# UTILIDADES
# ─────────────────────────────────────────────────────────────

def interpolate_keypoints(keypoints, target_length=15):
    current_length = len(keypoints)

    if current_length == target_length:
        return keypoints

    indices = np.linspace(0, current_length - 1, target_length)

    result = []

    for i in indices:
        lower = int(np.floor(i))
        upper = int(np.ceil(i))

        weight = i - lower

        if lower == upper:
            result.append(keypoints[lower])

        else:
            interp = (
                (1 - weight) * np.array(keypoints[lower])
                + weight * np.array(keypoints[upper])
            )

            result.append(interp.tolist())

    return result


def normalize_keypoints(keypoints, target_length=15):
    current_length = len(keypoints)

    if current_length < target_length:
        return interpolate_keypoints(keypoints, target_length)

    elif current_length > target_length:
        step = current_length / target_length

        indices = np.arange(
            0,
            current_length,
            step
        ).astype(int)[:target_length]

        return [keypoints[i] for i in indices]

    return keypoints


def is_still(kp_current, kp_prev, threshold):
    if kp_prev is None:
        return False

    diff = np.mean(
        np.abs(
            np.array(kp_current) - np.array(kp_prev)
        )
    )

    return diff < threshold


def count_active_hands(kp: np.ndarray) -> int:
    lh_active = not np.allclose(kp[:63], 0)
    rh_active = not np.allclose(kp[63:], 0)

    return int(lh_active) + int(rh_active)


def load_rf_models():

    print("RUTA 1:", os.path.abspath(MODEL_1HAND_PATH))
    print("RUTA 2:", os.path.abspath(MODEL_2HANDS_PATH))

    rf_1hand = None
    rf_2hands = None

    if os.path.exists(MODEL_1HAND_PATH):
        with open(MODEL_1HAND_PATH, "rb") as f:
            rf_1hand = pickle.load(f)

        print("✓ RF 1 mano cargado")

    else:
        print("⚠ No se encontró model_1hand.p")

    if os.path.exists(MODEL_2HANDS_PATH):
        with open(MODEL_2HANDS_PATH, "rb") as f:
            rf_2hands = pickle.load(f)

        print("✓ RF 2 manos cargado")

    else:
        print("⚠ No se encontró model_2hands.p")

    return rf_1hand, rf_2hands


# ─────────────────────────────────────────────────────────────
# CLASE PRINCIPAL
# ─────────────────────────────────────────────────────────────

class SignPredictor:

    def __init__(self):

        print("Cargando modelos...")

        self.word_ids = get_word_ids(WORDS_JSON_PATH)

        # LSTM
        self.lstm_model = load_model(MODEL_PATH)

        self.predict_fn = tf.function(
            self.lstm_model,
            reduce_retracing=True
        )

        # RF
        self.rf_1hand, self.rf_2hands = load_rf_models()

        self.rf_available = (
            self.rf_1hand is not None
            or self.rf_2hands is not None
        )

        # MediaPipe
        self.hands_model = mp.solutions.hands.Hands(
            static_image_mode=False,
            max_num_hands=2,
            min_detection_confidence=0.5,
            min_tracking_confidence=0.5,
        )

        # ESTADO
        self.kp_seq = []

        self.count_frame = 0
        self.still_count = 0
        self.static_count = 0

        self.cooldown = 0

        self.prev_kp = None

        self.recording = False

        self.lost_hand_frames = 0

        self.rf_last_letter = None
        self.rf_confirm_count = 0

        print("✓ Predictor listo")

    # ─────────────────────────────────────────
    # LSTM
    # ─────────────────────────────────────────

    def predict_lstm(self, kp_seq):

        kp_normalized = normalize_keypoints(
            kp_seq,
            int(MODEL_FRAMES)
        )

        input_tensor = tf.constant(
            np.expand_dims(kp_normalized, axis=0),
            dtype=tf.float32
        )

        res = self.predict_fn(
            input_tensor,
            training=False
        ).numpy()[0]

        confidence = float(res[np.argmax(res)])

        word_id = self.word_ids[np.argmax(res)]

        print(f"[LSTM] {word_id} ({confidence:.2f})")

        if confidence > THRESHOLD:

            word = (
                words_text.get(word_id)
                or words_text.get(word_id.split('-')[0])
            )

            return word

        return None

    # ─────────────────────────────────────────
    # RF
    # ─────────────────────────────────────────

    def predict_rf(self, kp_frame):

        n_hands = count_active_hands(kp_frame)

        rf_data = (
            self.rf_2hands
            if n_hands == 2
            else self.rf_1hand
        )

        if rf_data is None:
            return None

        model = rf_data["model"]
        classes = rf_data["classes"]

        proba = model.predict_proba([kp_frame])[0]

        best_idx = int(np.argmax(proba))

        confidence = float(proba[best_idx])

        letter = classes[best_idx]

        print(f"[RF] {letter} ({confidence:.2f})")

        if confidence >= RF_THRESHOLD:
            return letter.upper()

        return None

    # ─────────────────────────────────────────
    # RESET
    # ─────────────────────────────────────────

    def reset_state(self):

        self.kp_seq = []

        self.count_frame = 0
        self.still_count = 0
        self.static_count = 0

        self.prev_kp = None

        self.recording = False

        self.lost_hand_frames = 0

        self.rf_last_letter = None
        self.rf_confirm_count = 0

    # ─────────────────────────────────────────
    # FRAME PRINCIPAL
    # ─────────────────────────────────────────

    def process_frame(self, frame):

        # cooldown
        if self.cooldown > 0:
            self.cooldown -= 1
            return None

        # mediapipe
        results = mediapipe_detection(
            frame,
            self.hands_model
        )

        hand_present = there_hand(results)

        # ─────────────────────────────────────
        # MANO PRESENTE
        # ─────────────────────────────────────

        if hand_present:

            self.lost_hand_frames = 0

            self.recording = True

            self.count_frame += 1

            kp_frame = extract_keypoints(results)

            if self.count_frame > MARGIN_FRAME:

                self.kp_seq.append(kp_frame)

                # quietud LSTM
                if is_still(
                    kp_frame,
                    self.prev_kp,
                    STILLNESS_THRESHOLD
                ):
                    self.still_count += 1

                else:
                    self.still_count = 0

                # quietud RF
                if (
                    self.rf_available
                    and is_still(
                        kp_frame,
                        self.prev_kp,
                        STATIC_THRESHOLD
                    )
                ):

                    self.static_count += 1

                    # RF
                    if self.static_count >= STATIC_FRAMES:

                        letter = self.predict_rf(kp_frame)

                        if letter == self.rf_last_letter:
                            self.rf_confirm_count += 1

                        else:
                            self.rf_last_letter = letter
                            self.rf_confirm_count = 1

                        if (
                            letter
                            and self.rf_confirm_count >= RF_CONFIRM_FRAMES
                        ):

                            print("PRED:", letter)

                            self.reset_state()

                            self.cooldown = COOLDOWN_FRAMES

                            return letter

                else:

                    self.static_count = 0
                    self.rf_last_letter = None
                    self.rf_confirm_count = 0

                # LSTM
                if (
                    self.still_count >= STILLNESS_FRAMES
                    and len(self.kp_seq) >= MIN_FRAMES
                ):

                    kp_to_predict = (
                        self.kp_seq[:-self.still_count]
                        if self.still_count < len(self.kp_seq)
                        else self.kp_seq
                    )

                    if len(kp_to_predict) >= MIN_FRAMES:

                        sent = self.predict_lstm(kp_to_predict)

                        if sent:

                            print("PRED:", sent)

                            self.reset_state()

                            self.cooldown = COOLDOWN_FRAMES

                            return sent

            self.prev_kp = kp_frame

        # ─────────────────────────────────────
        # MANO AUSENTE
        # ─────────────────────────────────────

        else:

            if self.recording:

                self.lost_hand_frames += 1

                if self.lost_hand_frames <= LOST_HAND_TOLERANCE:

                    if self.prev_kp is not None:
                        self.kp_seq.append(self.prev_kp)

                    return None

            # predecir si hay suficientes frames
            if len(self.kp_seq) >= MIN_FRAMES:

                sent = self.predict_lstm(self.kp_seq)

                if sent:

                    print("PRED:", sent)

                    self.reset_state()

                    self.cooldown = COOLDOWN_FRAMES

                    return sent

            self.reset_state()

        return None