"""
rename_images.py — Renombra imágenes de una carpeta a frame_01, frame_02, ...

Uso:
    python rename_images.py                        → renombra en la carpeta actual
    python rename_images.py ruta/a/tu/carpeta      → renombra en esa carpeta
"""

import os
import sys

EXTENSIONS = ('.jpg', '.jpeg', '.png', '.bmp', '.webp')


def rename_images(folder_path='.'):
    if not os.path.exists(folder_path):
        print(f"Error: la carpeta '{folder_path}' no existe.")
        return

    images = [
        f for f in os.listdir(folder_path)
        if os.path.isfile(os.path.join(folder_path, f))
        and f.lower().endswith(EXTENSIONS)
    ]

    if not images:
        print(f"No se encontraron imágenes en '{folder_path}'.")
        return

    images = sorted(images)
    print(f"Se encontraron {len(images)} imágenes en '{folder_path}'")
    print("Renombrando...\n")

    # Primero renombrar a nombres temporales para evitar colisiones
    temp_names = []
    for i, img in enumerate(images):
        ext = os.path.splitext(img)[1].lower()
        temp_name = f"__temp_{i}{ext}"
        os.rename(os.path.join(folder_path, img), os.path.join(folder_path, temp_name))
        temp_names.append(temp_name)

    # Luego renombrar a frame_01, frame_02, ...
    for i, temp_name in enumerate(temp_names, start=1):
        ext = os.path.splitext(temp_name)[1].lower()
        new_name = f"frame_{i:02d}{ext}"
        os.rename(os.path.join(folder_path, temp_name), os.path.join(folder_path, new_name))
        print(f"  {temp_name}  →  {new_name}")

    print(f"\n✓ {len(images)} imágenes renombradas correctamente.")


if __name__ == "__main__":
    folder = sys.argv[1] if len(sys.argv) > 1 else '.'
    rename_images(folder)