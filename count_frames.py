import cv2
cap = cv2.VideoCapture(r"C:\Users\smora\OneDrive\Escritorio\DBP\PROYECTO-KINETICA\videos\aprender.mp4")
print("FPS:", cap.get(cv2.CAP_PROP_FPS))
print("Frames totales:", cap.get(cv2.CAP_PROP_FRAME_COUNT))
print("Duración (seg):", cap.get(cv2.CAP_PROP_FRAME_COUNT) / cap.get(cv2.CAP_PROP_FPS))
cap.release()