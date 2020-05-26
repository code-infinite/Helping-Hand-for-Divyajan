# for YOLO Models

# import the necessary packages
import numpy as np
import cv2
import os
import io
from PIL import Image
import base64


class Detect:

    returnFrame=""
    net=None
    classes = []
    colors=None
    output_layers=None

    def __init__(self):
        path=os.path.dirname(__file__)
        # load our serialized model from disk
        self.net = cv2.dnn.readNet(f"{path}/yolov3.weights", f"{path}/yolov3.cfg")
        with open(f"{path}/coco.names", "r") as f:
            self.classes = [line.strip() for line in f.readlines()]
        layer_names = self.net.getLayerNames()
        self.output_layers = [layer_names[i[0] - 1] for i in self.net.getUnconnectedOutLayers()]
        self.colors = np.random.uniform(0, 255, size=(len(self.classes), 3))


    # initialize the list of class labels MobileNet SSD was trained to
    # detect, then generate a set of bounding box colors for each class

    """docstring for Detect"""
    def result(self, inputFrame):

        # grab the frame from the bytes and resize it
        # to have a maximum width of 400 pixels
        pic = Image.open(io.BytesIO(bytes(inputFrame)))
        open_cv_image = np.array(pic)

        # Convert RGB to BGR
        frame = open_cv_image[:, :, ::-1].copy()
        frame=cv2.rotate(frame,rotateCode = 0)
        img = cv2.resize(frame, None, fx=0.4, fy=0.4)
        height, width, channels = img.shape
        # Detecting objects
        blob = cv2.dnn.blobFromImage(img, 0.00392, (416, 416), (0, 0, 0), True, crop=False)
        self.net.setInput(blob)
        outs = self.net.forward(self.output_layers)
        
        # Showing informations on the screen
        class_ids = []
        confidences = []
        boxes = []
        for out in outs:
            for detection in out:
                scores = detection[5:]
                class_id = np.argmax(scores)
                confidence = scores[class_id]
                if confidence > 0.5:
                
                    # Object detected
                    center_x = int(detection[0] * width)
                    center_y = int(detection[1] * height)
                    w = int(detection[2] * width)
                    h = int(detection[3] * height)

                    # Rectangle coordinates
                    x = int(center_x - w / 2)
                    y = int(center_y - h / 2)

                    boxes.append([x, y, w, h])
                    confidences.append(float(confidence))
                    class_ids.append(class_id)

        indexes = cv2.dnn.NMSBoxes(boxes, confidences, 0.5, 0.4)
        font = cv2.FONT_HERSHEY_PLAIN
        for i in range(len(boxes)):
            if i in indexes:
                x, y, w, h = boxes[i]
                label = str(self.classes[class_ids[i]])
                color = self.colors[i]
                cv2.rectangle(img, (x, y), (x + w, y + h), color, 2)
                cv2.putText(img, label, (x, y + 30), font, 3, color, 3)



        self.returnFrame=str(base64.b64encode(cv2.imencode('.jpg', img)[1]))