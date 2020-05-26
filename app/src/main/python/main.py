# for caffe Models

import base64
import cv2
import io
# import the necessary packages
import numpy as np
import os
from PIL import Image


class Detect:
    returnFrame = ""
    net = None
    CLASSES = None
    COLORS = None
    CLASSES_height = None
    warn = 0
    warnObj = ""
    outdoor = {"bus", "car", "chair", "cow", "horse", "motorbike", "person", "sheep", "sofa",
               "train"}

    def __init__(self):
        self.CLASSES = ["background", "aeroplane", "bicycle", "bird", "boat",
                        "bottle", "bus", "car", "cat", "chair", "cow", "dining table",
                        "dog", "horse", "motorbike", "person", "potted plant", "sheep",
                        "sofa", "train", "tv or monitor"]
        self.CLASSES_height = ["100", "6300", "550", "70", "410",
                               "265", "3810", "1350", "300", "700", "1800", "746",
                               "800", "1760", "1120", "1300", "270", "910",
                               "750", "4025", "420"]
        self.COLORS = np.random.uniform(0, 255, size=(len(self.CLASSES), 3))
        # load our serialized model from disk
        path = os.path.dirname(__file__)
        self.net = cv2.dnn.readNetFromCaffe(f"{path}/MobileNetSSD_deploy.prototxt.txt",
                                            f"{path}/MobileNetSSD_deploy.caffemodel")

    # initialize the list of class labels MobileNet SSD was trained to
    # detect, then generate a set of bounding box colors for each class

    def result(self, input_frame, focal_length, sensor_height, camRotation):
        global height
        self.warn = 0
        self.warnObj = ""
        # grab the frame from the bytes and resize it
        # to have a maximum width of 400 pixels
        frame = Image.open(io.BytesIO(bytes(input_frame)))
        frame = np.array(frame)
        # Convert RGB to BGR
        frame = frame[:, :, ::-1].copy()
        # grab the frame dimensions and convert it to a blob
        if camRotation == 90:
            frame = cv2.rotate(frame, rotateCode=0)
        if camRotation == 180:
            frame = cv2.rotate(frame, rotateCode=1)
        if camRotation == 270:
            frame = cv2.rotate(frame, rotateCode=2)
        (h, w) = frame.shape[:2]
        blob = cv2.dnn.blobFromImage(cv2.resize(frame, (300, 300)),
                                     0.007843, (300, 300), 127.5)

        # pass the blob through the network and obtain the detections and
        # predictions
        self.net.setInput(blob)
        detections = self.net.forward()

        # loop over the detections
        for i in np.arange(0, detections.shape[2]):
            # extract the confidence (i.e., probability) associated with
            # the prediction
            confidence = detections[0, 0, i, 2]

            # filter out weak detections by ensuring the `confidence` is
            # greater than the minimum confidence
            if confidence > 0.2:
                # extract the index of the class label from the
                # `detections`, then compute the (x, y)-coordinates of
                # the bounding box for the object
                idx = int(detections[0, 0, i, 1])
                box = detections[0, 0, i, 3:7] * np.array([w, h, w, h])
                (startX, startY, endX, endY) = box.astype(int)
                # draw the prediction on the frame
                label = "{}: {:.2f}%".format(self.CLASSES[idx], confidence * 100)
                cv2.rectangle(frame, (startX, startY), (endX, endY), self.COLORS[idx], 2)
                y = startY - 15 if startY - 15 > 15 else startY + 15
                known_height = float(self.CLASSES_height[idx])
                if endY > 9:
                    height = ((endY - startY) // 10) * 10
                distance = round(
                    ((round(focal_length, 2) * known_height * 300) / (height * sensor_height)) / 10,
                    2)
                if distance <= 130:
                    self.warnObj = self.CLASSES[idx]
                    if self.warnObj in self.outdoor:
                        self.warn = 1
                        if startX <= 100 and endX <= 200:
                            self.warnObj += " on your left side"
                        elif startX >= 200 and endX >= 300:
                            self.warnObj += " on your right side"
                        else:
                            self.warnObj += " In front of you"

                cv2.putText(frame, f"{label} {distance}", (startX, y),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.5, self.COLORS[idx], 2)

        if camRotation == 0:
            frame = cv2.rotate(frame, rotateCode=0)
        if camRotation == 180:
            frame = cv2.rotate(frame, rotateCode=2)
        if camRotation == 270:
            frame = cv2.rotate(frame, rotateCode=1)
        self.returnFrame = str(base64.b64encode(cv2.imencode('.jpg', frame)[1]))
