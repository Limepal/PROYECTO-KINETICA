"""
evaluate_model.py — Traductor LSP con sistema HÍBRIDO:
  • LSTM (modelo .keras) → señas DINÁMICAS (palabras, frases con movimiento)
  • Random Forest (.p)   → letras ESTÁTICAS  (mano quieta N frames)

Lógica de decisión:
    1. Si la mano está quieta >= STATIC_FRAMES consecutivos → RF predice letra
    2. Si hay movimiento y acumula >= MIN_FRAMES → LSTM predice palabra
    3. Si la mano desaparece con suficientes frames → LSTM predice
"""

import cv2
import pickle
import numpy as np
import tensorflow as tf
import mediapipe as mp
from keras.models import load_model
from helpers import get_word_ids, mediapipe_detection, there_hand, extract_keypoints, draw_keypoints
from constants import *

# ── Configuración LSTM (señas dinámicas) ──────────────────────────────────────
THRESHOLD           = 0.80
MARGIN_FRAME        = 0
MIN_FRAMES          = 15
STILLNESS_FRAMES    = 10
STILLNESS_THRESHOLD = 0.08
COOLDOWN_FRAMES     = 2
LOST_HAND_TOLERANCE = 6    # frames sin mano antes de resetear

# ── Configuración Random Forest (letras estáticas) ────────────────────────────
RF_THRESHOLD        = 0.80
STATIC_FRAMES       = 5
STATIC_THRESHOLD    = 0.04
RF_CONFIRM_FRAMES   = 3

# ── Rutas de modelos RF ───────────────────────────────────────────────────────
MODEL_1HAND_PATH  = "model_1hand.p"
MODEL_2HANDS_PATH = "model_2hands.p"


# ══════════════════════════════════════════════════════════════════════════════
# Interpolación / normalización de keypoints para LSTM
# ══════════════════════════════════════════════════════════════════════════════

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
            interp = (1 - weight) * np.array(keypoints[lower]) + weight * np.array(keypoints[upper])
            result.append(interp.tolist())
    return result


def normalize_keypoints(keypoints, target_length=15):
    current_length = len(keypoints)
    if current_length < target_length:
        return interpolate_keypoints(keypoints, target_length)
    elif current_length > target_length:
        step = current_length / target_length
        indices = np.arange(0, current_length, step).astype(int)[:target_length]
        return [keypoints[i] for i in indices]
    return keypoints


# ══════════════════════════════════════════════════════════════════════════════
# Utilidades de detección
# ══════════════════════════════════════════════════════════════════════════════

def is_still(kp_current, kp_prev, threshold=STILLNESS_THRESHOLD):
    if kp_prev is None:
        return False
    return np.mean(np.abs(np.array(kp_current) - np.array(kp_prev))) < threshold


def count_active_hands(kp: np.ndarray) -> int:
    """Cuenta cuántas manos tienen keypoints no nulos en el vector de 126 dims."""
    lh_active = not np.allclose(kp[:63], 0)
    rh_active = not np.allclose(kp[63:], 0)
    return int(lh_active) + int(rh_active)


# ══════════════════════════════════════════════════════════════════════════════
# Carga de modelos RF
# ══════════════════════════════════════════════════════════════════════════════

def load_rf_models():
    rf_1hand = rf_2hands = None

    if os.path.exists(MODEL_1HAND_PATH):
        with open(MODEL_1HAND_PATH, 'rb') as f:
            rf_1hand = pickle.load(f)
        print(f"✓ RF 1 mano cargado  ({len(rf_1hand['classes'])} clases: {rf_1hand['classes']})")
    else:
        print(f"⚠ No se encontró {MODEL_1HAND_PATH} — predicción de letras desactivada para 1 mano")

    if os.path.exists(MODEL_2HANDS_PATH):
        with open(MODEL_2HANDS_PATH, 'rb') as f:
            rf_2hands = pickle.load(f)
        print(f"✓ RF 2 manos cargado ({len(rf_2hands['classes'])} clases)")
    else:
        print(f"⚠ No se encontró {MODEL_2HANDS_PATH} — predicción de letras desactivada para 2 manos")

    return rf_1hand, rf_2hands


# ══════════════════════════════════════════════════════════════════════════════
# Predicciones
# ══════════════════════════════════════════════════════════════════════════════

def predict_lstm(predict_fn, kp_seq, word_ids):
    kp_normalized = normalize_keypoints(kp_seq, int(MODEL_FRAMES))
    input_tensor = tf.constant(np.expand_dims(kp_normalized, axis=0), dtype=tf.float32)
    res = predict_fn(input_tensor, training=False).numpy()[0]
    confidence = float(res[np.argmax(res)])
    word_id = word_ids[np.argmax(res)]
    print(f"  [LSTM] → {word_id} ({confidence * 100:.1f}%)")
    if confidence > THRESHOLD:
        word = words_text.get(word_id) or words_text.get(word_id.split('-')[0])
        return word, confidence
    return None, confidence


def predict_rf(kp_frame: np.ndarray, rf_1hand, rf_2hands):
    n_hands = count_active_hands(kp_frame)

    # Si no hay manos activas en el vector (ej: durante lost_hand), no predecir
    if n_hands == 0:
        return None, 0.0

    rf_data = rf_2hands if n_hands == 2 else rf_1hand
    if rf_data is None:
        return None, 0.0

    model   = rf_data['model']
    classes = rf_data['classes']

    proba      = model.predict_proba([kp_frame])[0]
    best_idx   = int(np.argmax(proba))
    confidence = float(proba[best_idx])
    letter     = classes[best_idx]

    print(f"  [RF]   → '{letter}' ({confidence * 100:.1f}%) [{n_hands} mano(s)]")

    if confidence >= RF_THRESHOLD:
        return letter.upper(), confidence
    return None, confidence


# ══════════════════════════════════════════════════════════════════════════════
# Main loop
# ══════════════════════════════════════════════════════════════════════════════

def evaluate_model(src=None):
    word_ids = get_word_ids(WORDS_JSON_PATH)

    video = cv2.VideoCapture(src or 0)
    video.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
    video.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)

    lstm_model = load_model(MODEL_PATH)
    predict_fn = tf.function(lstm_model, reduce_retracing=True)

    rf_1hand, rf_2hands = load_rf_models()
    rf_available = (rf_1hand is not None) or (rf_2hands is not None)

    kp_seq           = []
    sentence         = []
    count_frame      = 0
    still_count      = 0
    static_count     = 0
    cooldown         = 0
    prev_kp          = None
    recording        = False
    lost_hand_frames = 0
    rf_last_letter   = None
    rf_confirm_count = 0

    with mp.solutions.hands.Hands(
        static_image_mode=False,
        max_num_hands=2,
        min_detection_confidence=0.5,
        min_tracking_confidence=0.5,
    ) as hands_model:

        while video.isOpened():
            ret, frame = video.read()
            if not ret:
                break

            results      = mediapipe_detection(frame, hands_model)
            hand_present = there_hand(results)

            # ── Cooldown ──────────────────────────────────────────────────────
            if cooldown > 0:
                cooldown -= 1
                if not src:
                    _draw_ui(frame, sentence, recording=False,
                             still_count=0, static_count=0, kp_seq=kp_seq)
                    draw_keypoints(frame, results)
                    cv2.imshow('Traductor LSP', frame)
                    if cv2.waitKey(10) & 0xFF == ord('q'):
                        break
                continue

            # ── Mano presente ─────────────────────────────────────────────────
            if hand_present:
                lost_hand_frames = 0
                recording        = True
                count_frame     += 1
                kp_frame         = extract_keypoints(results)

                if count_frame > MARGIN_FRAME:
                    kp_seq.append(kp_frame)

                    # ── ¿Quieto? (para LSTM stillness) ───────────────────────
                    if is_still(kp_frame, prev_kp, STILLNESS_THRESHOLD):
                        still_count += 1
                    else:
                        still_count = 0

                    # ── ¿Muy quieto? (para RF estático) ──────────────────────
                    if rf_available and is_still(kp_frame, prev_kp, STATIC_THRESHOLD):
                        static_count += 1

                        if static_count >= STATIC_FRAMES:
                            letter, conf = predict_rf(kp_frame, rf_1hand, rf_2hands)

                            if letter == rf_last_letter:
                                rf_confirm_count += 1
                            else:
                                rf_last_letter   = letter
                                rf_confirm_count = 1

                            if letter and rf_confirm_count >= RF_CONFIRM_FRAMES:
                                sentence.insert(0, letter)
                                kp_seq, count_frame, still_count = [], 0, 0
                                static_count, rf_last_letter, rf_confirm_count = 0, None, 0
                                prev_kp, lost_hand_frames = None, 0
                                recording = False
                                cooldown  = COOLDOWN_FRAMES
                                prev_kp   = kp_frame
                                if not src:
                                    _draw_ui(frame, sentence, recording, still_count, static_count, kp_seq)
                                    draw_keypoints(frame, results)
                                    cv2.imshow('Traductor LSP', frame)
                                    if cv2.waitKey(10) & 0xFF == ord('q'):
                                        break
                                continue
                    else:
                        # Hay movimiento → reset RF
                        static_count     = 0
                        rf_last_letter   = None
                        rf_confirm_count = 0

                    # ── ¿Quieto suficiente para LSTM? ─────────────────────────
                    if still_count >= STILLNESS_FRAMES and len(kp_seq) >= MIN_FRAMES:
                        kp_to_predict = kp_seq[:-still_count] if still_count < len(kp_seq) else kp_seq
                        if len(kp_to_predict) >= MIN_FRAMES:
                            sent, conf = predict_lstm(predict_fn, kp_to_predict, word_ids)
                            if sent:
                                sentence.insert(0, sent)
                        kp_seq, count_frame, still_count = [], 0, 0
                        static_count, rf_last_letter, rf_confirm_count = 0, None, 0
                        prev_kp, lost_hand_frames = None, 0
                        cooldown  = COOLDOWN_FRAMES
                        recording = False
                        continue

                prev_kp = kp_frame

            # ── Mano ausente ──────────────────────────────────────────────────
            else:
                if recording:
                    lost_hand_frames += 1

                    if lost_hand_frames <= LOST_HAND_TOLERANCE:
                        # Dentro de la tolerancia:
                        # • kp_seq se rellena con el último frame conocido (LSTM sigue acumulando)
                        # • static_count y rf_confirm_count NO se tocan → se congelan
                        #   Esto evita que "buenos días" dispare el RF cuando los puntos
                        #   desaparecen brevemente entre movimientos
                        if prev_kp is not None:
                            kp_seq.append(prev_kp)
                        if not src:
                            _draw_ui(frame, sentence, recording, still_count, static_count, kp_seq)
                            draw_keypoints(frame, results)
                            cv2.imshow('Traductor LSP', frame)
                            if cv2.waitKey(10) & 0xFF == ord('q'):
                                break
                        continue

                # Tolerancia agotada → predecir LSTM y resetear todo
                if len(kp_seq) >= MIN_FRAMES:
                    sent, conf = predict_lstm(predict_fn, kp_seq, word_ids)
                    if sent:
                        sentence.insert(0, sent)
                    cooldown = COOLDOWN_FRAMES

                kp_seq, count_frame, still_count = [], 0, 0
                static_count, rf_last_letter, rf_confirm_count = 0, None, 0
                lost_hand_frames = 0
                prev_kp   = None
                recording = False

            # ── UI ────────────────────────────────────────────────────────────
            if not src:
                _draw_ui(frame, sentence, recording, still_count, static_count, kp_seq)
                draw_keypoints(frame, results)
                cv2.imshow('Traductor LSP', frame)
                if cv2.waitKey(10) & 0xFF == ord('q'):
                    break

    video.release()
    cv2.destroyAllWindows()
    return sentence


# ══════════════════════════════════════════════════════════════════════════════
# UI
# ══════════════════════════════════════════════════════════════════════════════

def _draw_ui(frame, sentence, recording, still_count, static_count, kp_seq):
    H, W = frame.shape[:2]

    cv2.rectangle(frame, (0, 0), (W, 40), (245, 117, 16), -1)
    text = ' | '.join(sentence[:5]) if sentence else 'Esperando seña...'
    cv2.putText(frame, text, (10, 28), FONT, FONT_SIZE, (255, 255, 255))

    if recording and len(kp_seq) > 0:
        progress = min(len(kp_seq) / MODEL_FRAMES, 1.0)
        bar_w = int((W - 20) * progress)
        cv2.rectangle(frame, (10, 45), (W - 10, 55), (80, 80, 80), -1)
        cv2.rectangle(frame, (10, 45), (10 + bar_w, 55), (0, 200, 100), -1)
        cv2.circle(frame, (W - 20, 20), 7, (0, 0, 255), -1)

    if still_count > 0:
        cv2.putText(frame, f'Pausa: {still_count}/{STILLNESS_FRAMES}',
                    (10, H - 35), FONT, 1.2, (0, 200, 255))

    if static_count >= STATIC_FRAMES:
        pct   = min(static_count / (STATIC_FRAMES * 2), 1.0)
        bar_w = int((W - 20) * pct)
        cv2.rectangle(frame, (10, 60), (W - 10, 68), (60, 60, 60), -1)
        cv2.rectangle(frame, (10, 60), (10 + bar_w, 68), (255, 180, 0), -1)
        cv2.putText(frame, f'Letra estatica ({static_count}f)',
                    (10, H - 15), FONT, 1.2, (255, 180, 0))


if __name__ == '__main__':
    evaluate_model()