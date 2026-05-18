"""
play_frames.py — Reproduce imágenes de una carpeta como si fuera un video.

Uso:
    python play_frames.py                          → abre un selector de carpeta
    python play_frames.py ruta/a/tu/carpeta        → reproduce esa carpeta
    python play_frames.py ruta/carpeta --fps 15    → reproduce a 15 FPS

Controles:
    SPACE  → pausar / reanudar
    →      → siguiente frame (cuando pausado)
    ←      → frame anterior (cuando pausado)
    R      → reiniciar
    Q/ESC  → salir
"""

import os
import sys
import cv2
import argparse


EXTENSIONS = ('.jpg', '.jpeg', '.png', '.bmp', '.webp')


def load_frames(folder_path):
    files = sorted([
        f for f in os.listdir(folder_path)
        if os.path.isfile(os.path.join(folder_path, f))
        and f.lower().endswith(EXTENSIONS)
    ])
    if not files:
        print(f"No se encontraron imágenes en '{folder_path}'.")
        return []

    frames = []
    for f in files:
        img = cv2.imread(os.path.join(folder_path, f))
        if img is not None:
            frames.append((f, img))
        else:
            print(f"  ⚠ No se pudo leer: {f}")

    return frames


def play_frames(folder_path, fps=12):
    print(f"Cargando imágenes de '{folder_path}'...")
    frames = load_frames(folder_path)
    if not frames:
        return

    total = len(frames)
    print(f"  ✓ {total} imágenes cargadas a {fps} FPS")
    print("\nControles: SPACE=pausar  ←→=frame a frame  R=reiniciar  Q=salir\n")

    delay = max(1, int(1000 / fps))
    current = 0
    paused = False

    while True:
        name, frame = frames[current]

        # Overlay de info
        display = frame.copy()
        h, w = display.shape[:2]

        # Barra inferior
        cv2.rectangle(display, (0, h - 35), (w, h), (30, 30, 30), -1)

        # Nombre del archivo y progreso
        cv2.putText(display, f"{name}  [{current + 1}/{total}]",
                    (10, h - 12), cv2.FONT_HERSHEY_SIMPLEX, 0.55, (200, 200, 200), 1)

        # Barra de progreso
        bar_w = int((w - 20) * (current + 1) / total)
        cv2.rectangle(display, (10, h - 38), (w - 10, h - 36), (80, 80, 80), -1)
        cv2.rectangle(display, (10, h - 38), (10 + bar_w, h - 36), (0, 200, 100), -1)

        # Indicador PAUSE
        if paused:
            cv2.putText(display, "PAUSADO", (w - 100, 30),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 200, 255), 2)

        # FPS indicator
        cv2.putText(display, f"{fps} FPS", (10, 25),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, (150, 150, 150), 1)

        cv2.imshow("Reproductor de Frames", display)

        key = cv2.waitKey(1 if paused else delay) & 0xFF

        if key in (ord('q'), 27):       # Q o ESC → salir
            break
        elif key == ord(' '):           # SPACE → pausar/reanudar
            paused = not paused
        elif key == ord('r'):           # R → reiniciar
            current = 0
            paused = False
        elif key == 83 or key == ord('d'):   # → → siguiente frame
            current = min(current + 1, total - 1)
        elif key == 81 or key == ord('a'):   # ← → frame anterior
            current = max(current - 1, 0)
        elif not paused:
            current += 1
            if current >= total:
                current = 0             # loop automático

    cv2.destroyAllWindows()


def main():
    parser = argparse.ArgumentParser(description="Reproduce imágenes como video")
    parser.add_argument("folder", nargs="?", default=None, help="Carpeta con imágenes")
    parser.add_argument("--fps", type=int, default=12, help="Frames por segundo (default: 12)")
    args = parser.parse_args()

    folder = args.folder
    if not folder:
        # Si no se pasó carpeta, pedirla por input
        folder = input("Ruta de la carpeta con imágenes: ").strip().strip('"')

    if not os.path.exists(folder):
        print(f"Error: '{folder}' no existe.")
        return

    play_frames(folder, fps=args.fps)


if __name__ == "__main__":
    main()