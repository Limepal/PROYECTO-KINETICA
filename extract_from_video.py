"""
extract_from_video.py — Extrae frames de videos profesionales y los guarda
como muestras de entrenamiento en frame_actions/<nombre_seña>/sample_xxx/

Uso:
    python extract_from_video.py                         → procesa todos los videos en VIDEO_DIR
    python extract_from_video.py ruta/video.mp4 nombre   → procesa un video específico

Estructura esperada de videos:
    videos/
        hola.mp4
        gracias.mp4
        buenos-dias.mp4
        ...

El nombre del archivo (sin extensión) se usa como nombre de la seña.
"""

import os
import sys
import cv2
import numpy as np
from datetime import datetime

# ── Configuración ──────────────────────────────────────────────────────────────
VIDEO_DIR = "./videos"              # carpeta donde están tus videos
OUTPUT_DIR = "./frame_actions"      # carpeta de salida (frame_actions del proyecto)
TARGET_FRAMES = 15                  # frames a extraer por video
VIDEO_EXTENSIONS = ('.mp4', '.avi', '.mov', '.mkv')
# ───────────────────────────────────────────────────────────────────────────────


def extract_frames(video_path, target_frames=TARGET_FRAMES):
    '''
    Extrae `target_frames` frames distribuidos uniformemente del video.
    Retorna lista de frames (numpy arrays).
    '''
    cap = cv2.VideoCapture(video_path)
    if not cap.isOpened():
        print(f"  ✗ No se pudo abrir: {video_path}")
        return []

    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    fps = cap.get(cv2.CAP_PROP_FPS)
    duration = total_frames / fps if fps > 0 else 0

    print(f"  Video: {total_frames} frames | {fps:.1f} FPS | {duration:.2f} seg")

    if total_frames == 0:
        print("  ✗ Video vacío o corrupto.")
        cap.release()
        return []

    # Distribuir índices uniformemente a lo largo del video
    indices = np.linspace(0, total_frames - 1, target_frames).astype(int)

    frames = []
    for idx in indices:
        cap.set(cv2.CAP_PROP_POS_FRAMES, idx)
        ret, frame = cap.read()
        if ret:
            frames.append(frame)
        else:
            print(f"  ⚠ No se pudo leer frame {idx}")

    cap.release()
    return frames


def save_sample(frames, word_name):
    '''
    Guarda los frames como una muestra en frame_actions/<word_name>/sample_xxx/
    '''
    word_dir = os.path.join(OUTPUT_DIR, word_name)
    os.makedirs(word_dir, exist_ok=True)

    today = datetime.now().strftime('%y%m%d%H%M%S%f')
    sample_dir = os.path.join(word_dir, f"sample_{today}")
    os.makedirs(sample_dir, exist_ok=True)

    for i, frame in enumerate(frames, start=1):
        frame_path = os.path.join(sample_dir, f"frame_{i:02d}.jpg")
        cv2.imwrite(frame_path, frame, [cv2.IMWRITE_JPEG_QUALITY, 95])

    print(f"  ✓ Guardado en: {sample_dir}")
    return sample_dir


def process_video(video_path, word_name):
    print(f"\nProcesando '{word_name}' desde: {os.path.basename(video_path)}")
    frames = extract_frames(video_path)
    if not frames:
        return False
    if len(frames) < TARGET_FRAMES:
        print(f"  ⚠ Solo se extrajeron {len(frames)} frames (esperados {TARGET_FRAMES})")
    save_sample(frames, word_name)
    return True


def process_all_videos(video_dir):
    if not os.path.exists(video_dir):
        print(f"Error: carpeta '{video_dir}' no existe.")
        print(f"Crea la carpeta y pon tus videos ahí, o pasa la ruta como argumento.")
        return

    videos = [
        f for f in os.listdir(video_dir)
        if f.lower().endswith(VIDEO_EXTENSIONS)
    ]

    if not videos:
        print(f"No se encontraron videos en '{video_dir}'.")
        return

    print(f"=== Extrayendo frames de {len(videos)} videos ===")
    ok, fail = 0, 0

    for video_file in sorted(videos):
        word_name = os.path.splitext(video_file)[0]  # nombre sin extensión = nombre de seña
        video_path = os.path.join(video_dir, video_file)
        if process_video(video_path, word_name):
            ok += 1
        else:
            fail += 1

    print(f"\n✓ {ok} videos procesados correctamente.")
    if fail:
        print(f"✗ {fail} videos fallaron.")

    print(f"\nAhora corre:")
    print(f"  python create_keypoints.py")
    print(f"  python training_model.py")


if __name__ == "__main__":
    if len(sys.argv) == 3:
        # Modo: un video específico
        # python extract_from_video.py ruta/video.mp4 nombre_seña
        video_path = sys.argv[1]
        word_name = sys.argv[2]
        process_video(video_path, word_name)

    elif len(sys.argv) == 2:
        # Modo: carpeta de videos
        process_all_videos(sys.argv[1])

    else:
        # Modo: carpeta por defecto
        process_all_videos(VIDEO_DIR)