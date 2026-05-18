"""
count_samples.py — Muestra cuántas muestras tiene cada seña
y cuántas faltan para llegar al objetivo.
"""

import os
import json

# ── Configuración ──────────────────────────────────────────────────────────────
FRAME_ACTIONS_PATH = "./frame_actions"
WORDS_JSON_PATH = "./models/words.json"
TARGET = 30  # muestras objetivo por seña
# ───────────────────────────────────────────────────────────────────────────────

def count_samples():
    # Leer señas del words.json
    with open(WORDS_JSON_PATH) as f:
        word_ids = json.load(f).get('word_ids', [])

    print(f"{'Seña':<20} {'Muestras':>8} {'Faltan':>8} {'Estado':>10}")
    print("-" * 50)

    total_ok = 0
    total_faltan = 0

    for word_id in word_ids:
        word_path = os.path.join(FRAME_ACTIONS_PATH, word_id)

        if not os.path.exists(word_path):
            print(f"{word_id:<20} {'0':>8} {str(TARGET):>8} {'❌ Sin carpeta':>10}")
            total_faltan += TARGET
            continue

        samples = [
            s for s in os.listdir(word_path)
            if os.path.isdir(os.path.join(word_path, s))
        ]
        count = len(samples)
        faltan = max(0, TARGET - count)

        if count >= TARGET:
            estado = "✓ OK"
            total_ok += 1
        elif count >= TARGET * 0.5:
            estado = "⚠ Casi"
        else:
            estado = "❌ Faltan"

        total_faltan += faltan
        print(f"{word_id:<20} {count:>8} {faltan:>8} {estado:>10}")

    print("-" * 50)
    print(f"{'TOTAL':<20} {total_ok:>8} señas OK | {total_faltan} muestras pendientes")
    print(f"\nObjetivo: {TARGET} muestras por seña")

if __name__ == "__main__":
    count_samples()