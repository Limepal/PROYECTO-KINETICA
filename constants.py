import os
import cv2

# SETTINGS
MIN_LENGTH_FRAMES = 5
LENGTH_KEYPOINTS = 126   # 21 landmarks x 3 coordenadas (x, y, z) x 2 manos = 126
MODEL_FRAMES = 25

# PATHS
ROOT_PATH = os.getcwd()
FRAME_ACTIONS_PATH = os.path.join(ROOT_PATH, "frame_actions")
DATA_PATH = os.path.join(ROOT_PATH, "data")
DATA_JSON_PATH = os.path.join(DATA_PATH, "data.json")
MODEL_FOLDER_PATH = os.path.join(ROOT_PATH, "models")
MODEL_PATH = os.path.join(MODEL_FOLDER_PATH, f"actions_{MODEL_FRAMES}.keras")
KEYPOINTS_PATH = os.path.join(DATA_PATH, "keypoints")
WORDS_JSON_PATH = os.path.join(MODEL_FOLDER_PATH, "words.json")

# SHOW IMAGE PARAMETERS
FONT = cv2.FONT_HERSHEY_PLAIN
FONT_SIZE = 1.5
FONT_POS = (5, 30)

words_text = {
    "a-izq": "a",
    "b-izq": "b",
    "c-izq": "c",
    "d-izq": "d",
    "e-izq": "e",
    "f-izq": "f",
    "g-izq": "g",
    "h-izq": "h",
    "i-izq": "i",
    "j-izq": "j",
    "k-izq": "k",
    "l-izq": "l",
    "m-izq": "m",
    "n-izq": "n",
    "o-izq": "o",
    "p-izq": "p",
    "q-izq": "q",
    "r-izq": "r",
    "s-izq": "s",
    "t-izq": "t",
    "u-izq": "u",
    "v-izq": "v",
    "w-izq": "w",
    "x-izq": "x",
    "y-izq": "y",
    "z-izq": "z",

    "a-der": "a",
    "b-der": "b",
    "c-der": "c",
    "d-der": "d",
    "e-der": "e",
    "f-der": "f",
    "g-der": "g",
    "h-der": "h",
    "i-der": "i",
    "j-der": "j",
    "k-der": "k",
    "l-der": "l",
    "m-der": "m",
    "n-der": "n",
    "o-der": "o",
    "p-der": "p",
    "q-der": "q",
    "r-der": "r",
    "s-der": "s",
    "t-der": "t",
    "u-der": "u",
    "v-der": "v",
    "w-der": "w",
    "x-der": "x",
    "y-der": "y",
    "z-der": "z",
    "comer" : "COMER",
    "que": "QUE",
    "adios": "ADIÓS",
    "bien": "BIEN",
    "buenas-noches": "BUENAS NOCHES",
    "buenas-tardes": "BUENAS TARDES",
    "buenos-dias": "BUENOS DIAS",
    "como-estas": "COMO ESTAS",
    "disculpa": "DISCULPA",
    "gracias": "GRACIAS",
    "aprender": "APRENDER",
    "hola": "HOLA",  # cubre hola-der y hola-izq (se hace split('-')[0])
    "mal": "MAL",
    "mas_o_menos": "MAS O MENOS",
    "me_ayudas": "ME AYUDAS",
    "por_favor": "POR FAVOR"
}
