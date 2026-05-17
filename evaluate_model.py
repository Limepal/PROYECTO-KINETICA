import cv2
import numpy as np
from mediapipe.python.solutions.holistic import Holistic
from keras.models import load_model
from helpers import get_word_ids, mediapipe_detection, there_hand, extract_keypoints, draw_keypoints
from constants import *

# ── Configuración ──────────────────────────────────────────────────────────────
THRESHOLD = 0.6          # confianza mínima para aceptar una predicción
MARGIN_FRAME = 1         # frames ignorados al inicio de cada seña
MIN_FRAMES = 5           # mínimo de frames para considerar una seña válida
STILLNESS_FRAMES = 4     # frames quietos consecutivos para disparar predicción
STILLNESS_THRESHOLD = 0.02  # diferencia de keypoints para considerar "quieto"
COOLDOWN_FRAMES = 10     # frames de espera tras predecir (evita doble predicción)
# ───────────────────────────────────────────────────────────────────────────────


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


def is_still(kp_current, kp_prev, threshold=STILLNESS_THRESHOLD):
    '''Retorna True si la diferencia entre dos frames de keypoints es pequeña.'''
    if kp_prev is None:
        return False
    diff = np.mean(np.abs(np.array(kp_current) - np.array(kp_prev)))
    return diff < threshold


def predict_sign(model, kp_seq, word_ids):
    '''Predice la seña y retorna (palabra, confianza) o (None, 0) si no supera umbral.'''
    kp_normalized = normalize_keypoints(kp_seq, int(MODEL_FRAMES))
    res = model.predict(np.expand_dims(kp_normalized, axis=0), verbose=0)[0]
    confidence = res[np.argmax(res)]
    word_id = word_ids[np.argmax(res)]
    print(f"  → {word_id} ({confidence * 100:.1f}%)")
    if confidence > THRESHOLD:
        word = words_text.get(word_id.split('-')[0])
        return word, confidence
    return None, confidence


def evaluate_model(src=None):
    kp_seq = []
    sentence = []
    word_ids = get_word_ids(WORDS_JSON_PATH)
    model = load_model(MODEL_PATH)

    count_frame = 0        # frames totales desde que apareció la mano
    still_count = 0        # frames quietos consecutivos
    cooldown = 0           # frames restantes de cooldown post-predicción
    prev_kp = None         # keypoints del frame anterior (para detectar quietud)
    recording = False      # True mientras acumulamos frames de una seña

    with Holistic() as holistic_model:
        video = cv2.VideoCapture(src or 0)

        while video.isOpened():
            ret, frame = video.read()
            if not ret:
                break

            results = mediapipe_detection(frame, holistic_model)
            hand_present = there_hand(results)

            # ── Cooldown post-predicción ───────────────────────────────────────
            if cooldown > 0:
                cooldown -= 1
                if not src:
                    _draw_ui(frame, sentence, recording=False, still_count=0, kp_seq=kp_seq)
                    draw_keypoints(frame, results)
                    cv2.imshow('Traductor LSP', frame)
                    if cv2.waitKey(10) & 0xFF == ord('q'):
                        break
                continue

            # ── Mano presente ─────────────────────────────────────────────────
            if hand_present:
                recording = True
                count_frame += 1
                kp_frame = extract_keypoints(results)

                if count_frame > MARGIN_FRAME:
                    kp_seq.append(kp_frame)

                    # Detectar quietud comparando con frame anterior
                    if is_still(kp_frame, prev_kp):
                        still_count += 1
                    else:
                        still_count = 0

                    # Si está quieto suficientes frames y tenemos suficiente data → predecir
                    if still_count >= STILLNESS_FRAMES and len(kp_seq) >= MIN_FRAMES:
                        # Quitar los frames quietos del final (son transición, no seña)
                        kp_to_predict = kp_seq[:-still_count] if still_count < len(kp_seq) else kp_seq
                        if len(kp_to_predict) >= MIN_FRAMES:
                            sent, conf = predict_sign(model, kp_to_predict, word_ids)
                            if sent:
                                sentence.insert(0, sent)
                            # Resetear para la siguiente seña
                            kp_seq = []
                            count_frame = 0
                            still_count = 0
                            prev_kp = None
                            cooldown = COOLDOWN_FRAMES
                            recording = False
                            continue

                prev_kp = kp_frame

            # ── Mano ausente ──────────────────────────────────────────────────
            else:
                # Si salió la mano y teníamos suficientes frames → predecir igual
                if len(kp_seq) >= MIN_FRAMES:
                    sent, conf = predict_sign(model, kp_seq, word_ids)
                    if sent:
                        sentence.insert(0, sent)
                    cooldown = COOLDOWN_FRAMES

                # Resetear todo
                kp_seq = []
                count_frame = 0
                still_count = 0
                prev_kp = None
                recording = False

            # ── UI ────────────────────────────────────────────────────────────
            if not src:
                _draw_ui(frame, sentence, recording, still_count, kp_seq)
                draw_keypoints(frame, results)
                cv2.imshow('Traductor LSP', frame)
                if cv2.waitKey(10) & 0xFF == ord('q'):
                    break

        video.release()
        cv2.destroyAllWindows()
        return sentence


def _draw_ui(frame, sentence, recording, still_count, kp_seq):
    '''Dibuja la interfaz sobre el frame.'''
    H, W = frame.shape[:2]

    # Barra superior — traducción
    cv2.rectangle(frame, (0, 0), (W, 40), (245, 117, 16), -1)
    text = ' | '.join(sentence[:5]) if sentence else 'Esperando seña...'
    cv2.putText(frame, text, (10, 28), FONT, FONT_SIZE, (255, 255, 255))

    # Barra de progreso de frames capturados
    if recording and len(kp_seq) > 0:
        progress = min(len(kp_seq) / MODEL_FRAMES, 1.0)
        bar_w = int((W - 20) * progress)
        cv2.rectangle(frame, (10, 45), (W - 10, 55), (80, 80, 80), -1)
        cv2.rectangle(frame, (10, 45), (10 + bar_w, 55), (0, 200, 100), -1)

        # Punto rojo = grabando
        cv2.circle(frame, (W - 20, 20), 7, (0, 0, 255), -1)

    # Indicador de quietud
    if still_count > 0:
        cv2.putText(frame, f'Pausa: {still_count}/{STILLNESS_FRAMES}',
                    (10, H - 15), FONT, 1.2, (0, 200, 255))


if __name__ == "__main__":
    evaluate_model()