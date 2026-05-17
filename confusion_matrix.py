import numpy as np
from tensorflow.keras.preprocessing.sequence import pad_sequences
from keras.models import load_model
from helpers import get_word_ids, get_sequences_and_labels
from constants import *
from sklearn.metrics import confusion_matrix, ConfusionMatrixDisplay
import matplotlib.pyplot as plt


def generate_confusion_matrix():
    word_ids = get_word_ids(WORDS_JSON_PATH)
    sequences, labels = get_sequences_and_labels(word_ids)
    sequences = pad_sequences(sequences, maxlen=int(MODEL_FRAMES), padding='pre', truncating='post', dtype='float32')

    model = load_model(MODEL_PATH)

    all_predictions = []
    for seq in sequences:
        res = model.predict(np.expand_dims(seq, axis=0))[0]
        all_predictions.append(np.argmax(res))

    conf_matrix = confusion_matrix(labels, all_predictions)

    plt.figure(figsize=(10, 8))
    disp = ConfusionMatrixDisplay(confusion_matrix=conf_matrix, display_labels=word_ids)
    disp.plot(cmap=plt.cm.Blues)
    plt.xticks(rotation=45, ha='right')
    plt.xlabel('Predicted')
    plt.ylabel('True')
    plt.title('Confusion Matrix')
    plt.tight_layout()
    plt.show()


if __name__ == "__main__":
    generate_confusion_matrix()
