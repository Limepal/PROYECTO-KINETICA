from fastapi import FastAPI, WebSocket
from fastapi.middleware.cors import CORSMiddleware

import cv2
import numpy as np
import base64

from backend.predictor import SignPredictor

app = FastAPI()

# permitir frontend
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# crear predictor UNA sola vez
predictor = SignPredictor()


@app.get("/")
def root():
    return {"message": "API funcionando"}


@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await websocket.accept()

    while True:
        data = await websocket.receive_text()

        # Validar que el frame tenga el formato correcto
        if "," not in data:
            continue

        try:
            image_data = base64.b64decode(data.split(",")[1])
            np_arr = np.frombuffer(image_data, np.uint8)
            frame = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)

            # Validar que el frame no esté vacío
            if frame is None:
                continue

            result = predictor.process_frame(frame)

            if result:
                await websocket.send_text(result)

        except Exception as e:
            print(f"Error procesando frame: {e}")
            continue