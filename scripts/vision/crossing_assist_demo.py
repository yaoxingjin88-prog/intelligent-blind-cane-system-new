from __future__ import annotations

import argparse
import time
from collections import deque
from dataclasses import dataclass
from typing import List, Optional, Tuple

import cv2
import numpy as np
import requests


@dataclass
class TrafficLightResult:
    status: str
    confidence: float
    center: Optional[Tuple[int, int]] = None
    radius: int = 0


@dataclass
class ZebraCrossingResult:
    detected: bool
    direction: str
    confidence: float
    center_x: Optional[int] = None
    stripe_count: int = 0


class CrossingAssistVisionDemo:
    def __init__(self, api_base: str, device_id: str, camera_index: int, report_interval: float, detect_interval: float, frame_width: int, preview: bool):
        self.api_base = api_base.rstrip("/")
        self.device_id = device_id
        self.camera_index = camera_index
        self.report_interval = report_interval
        self.detect_interval = max(0.05, detect_interval)
        self.frame_width = frame_width
        self.preview = preview
        self.last_report_at = 0.0
        self.last_payload_signature = None
        self.last_report_ok = False
        self.traffic_history = deque(maxlen=7)
        self.zebra_history = deque(maxlen=7)
        self.pending_signature = None
        self.pending_signature_streak = 0
        self.min_traffic_confidence = 0.2
        self.min_zebra_confidence = 0.3
        self.min_report_streak = 2
        self.force_report_interval = max(self.report_interval, 3.0)
        self.last_frame = None
        self.last_traffic_light = TrafficLightResult(status="UNKNOWN", confidence=0.0)
        self.last_zebra_crossing = ZebraCrossingResult(detected=False, direction="UNKNOWN", confidence=0.0)
        self.last_payload = self.build_payload(self.last_traffic_light, self.last_zebra_crossing)
        self.last_detection_at = 0.0

    def run(self) -> None:
        capture = cv2.VideoCapture(self.camera_index)
        if not capture.isOpened():
            raise RuntimeError(f"无法打开摄像头: {self.camera_index}")
        try:
            capture.set(cv2.CAP_PROP_BUFFERSIZE, 1)
        except Exception:
            pass

        try:
            while True:
                now = time.time()
                success, frame = capture.read()
                if not success or frame is None:
                    continue

                frame = self.resize_frame(frame)
                self.last_frame = frame

                if now - self.last_detection_at >= self.detect_interval:
                    traffic_light = self.stabilize_traffic_light(self.detect_traffic_light(frame))
                    zebra_crossing = self.stabilize_zebra_crossing(self.detect_zebra_crossing(frame))
                    payload = self.build_payload(traffic_light, zebra_crossing)
                    self.maybe_report(payload)
                    self.last_traffic_light = traffic_light
                    self.last_zebra_crossing = zebra_crossing
                    self.last_payload = payload
                    self.last_detection_at = now

                if self.preview:
                    next_forced_report_in = max(0.0, self.force_report_interval - max(0.0, time.time() - self.last_report_at))
                    preview = self.draw_overlay(
                        self.last_frame.copy(),
                        self.last_traffic_light,
                        self.last_zebra_crossing,
                        self.last_payload,
                        next_forced_report_in,
                    )
                    cv2.imshow("Crossing Assist Demo", preview)
                    key = cv2.waitKey(1) & 0xFF
                    if key in (27, ord("q")):
                        break
                else:
                    time.sleep(0.01)
        finally:
            capture.release()
            if self.preview:
                cv2.destroyAllWindows()

    def resize_frame(self, frame: np.ndarray) -> np.ndarray:
        height, width = frame.shape[:2]
        if width <= self.frame_width:
            return frame
        ratio = self.frame_width / float(width)
        return cv2.resize(frame, (self.frame_width, int(height * ratio)))

    def select_dominant_label(self, labels: List[str]) -> Tuple[str, int]:
        counts = {}
        for label in labels:
            counts[label] = counts.get(label, 0) + 1
        if not counts:
            return "UNKNOWN", 0
        dominant_label = max(counts, key=counts.get)
        return dominant_label, counts[dominant_label]

    def stabilize_traffic_light(self, result: TrafficLightResult) -> TrafficLightResult:
        self.traffic_history.append(result)
        valid = [item for item in self.traffic_history if item.status != "UNKNOWN" and item.confidence >= self.min_traffic_confidence]
        if len(valid) < 3:
            return TrafficLightResult(status="UNKNOWN", confidence=0.0)

        dominant_status, dominant_count = self.select_dominant_label([item.status for item in valid])
        if dominant_count < max(3, int(len(self.traffic_history) * 0.5)):
            return TrafficLightResult(status="UNKNOWN", confidence=0.0)

        matched = [item for item in valid if item.status == dominant_status]
        centers = [item.center for item in matched if item.center]
        radius_values = [item.radius for item in matched if item.radius > 0]
        center = None
        if centers:
            center = (
                int(sum(point[0] for point in centers) / len(centers)),
                int(sum(point[1] for point in centers) / len(centers)),
            )
        radius = int(sum(radius_values) / len(radius_values)) if radius_values else 0
        confidence = min(1.0, sum(item.confidence for item in matched) / len(matched) + 0.08 * min(dominant_count, 4))
        return TrafficLightResult(status=dominant_status, confidence=confidence, center=center, radius=radius)

    def stabilize_zebra_crossing(self, result: ZebraCrossingResult) -> ZebraCrossingResult:
        self.zebra_history.append(result)
        positives = [
            item for item in self.zebra_history
            if item.detected and item.direction != "UNKNOWN" and item.confidence >= self.min_zebra_confidence
        ]
        if len(positives) < 3:
            return ZebraCrossingResult(detected=False, direction="UNKNOWN", confidence=0.0)

        dominant_direction, dominant_count = self.select_dominant_label([item.direction for item in positives])
        if dominant_count < max(3, int(len(positives) * 0.6)):
            return ZebraCrossingResult(detected=False, direction="UNKNOWN", confidence=0.0)

        matched = [item for item in positives if item.direction == dominant_direction]
        centers = [item.center_x for item in matched if item.center_x is not None]
        center_x = int(sum(centers) / len(centers)) if centers else None
        stripe_count = int(round(sum(item.stripe_count for item in matched) / len(matched)))
        confidence = min(1.0, sum(item.confidence for item in matched) / len(matched) + 0.06 * min(dominant_count, 4))
        return ZebraCrossingResult(
            detected=True,
            direction=dominant_direction,
            confidence=confidence,
            center_x=center_x,
            stripe_count=stripe_count,
        )

    def detect_traffic_light(self, frame: np.ndarray) -> TrafficLightResult:
        height, width = frame.shape[:2]
        roi_x1 = int(width * 0.15)
        roi_x2 = int(width * 0.85)
        roi_y2 = int(height * 0.5)
        roi = frame[:roi_y2, roi_x1:roi_x2]
        roi = cv2.GaussianBlur(roi, (5, 5), 0)
        hsv = cv2.cvtColor(roi, cv2.COLOR_BGR2HSV)

        color_ranges = {
            "RED": [
                ((0, 110, 110), (12, 255, 255)),
                ((165, 110, 110), (180, 255, 255)),
            ],
            "YELLOW": [
                ((16, 90, 130), (40, 255, 255)),
            ],
            "GREEN": [
                ((40, 70, 90), (95, 255, 255)),
            ],
        }

        best_result = TrafficLightResult(status="UNKNOWN", confidence=0.0)

        for status, ranges in color_ranges.items():
            mask = np.zeros(hsv.shape[:2], dtype=np.uint8)
            for lower, upper in ranges:
                mask = cv2.bitwise_or(mask, cv2.inRange(hsv, np.array(lower, dtype=np.uint8), np.array(upper, dtype=np.uint8)))
            kernel = np.ones((5, 5), np.uint8)
            mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, kernel)
            mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, kernel)

            contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            for contour in contours:
                area = cv2.contourArea(contour)
                if area < 55:
                    continue
                x, y, w, h = cv2.boundingRect(contour)
                aspect_ratio = w / float(max(h, 1))
                if aspect_ratio < 0.6 or aspect_ratio > 1.7:
                    continue
                perimeter = cv2.arcLength(contour, True)
                if perimeter <= 0:
                    continue
                circularity = (4.0 * np.pi * area) / (perimeter * perimeter)
                if circularity < 0.42:
                    continue
                (cx, cy), radius = cv2.minEnclosingCircle(contour)
                if radius < 5 or radius > 40:
                    continue
                if cy > roi.shape[0] * 0.9:
                    continue
                active_pixels = mask[y:y + h, x:x + w] > 0
                if not np.any(active_pixels):
                    continue
                saturation_values = hsv[y:y + h, x:x + w, 1][active_pixels]
                brightness_values = hsv[y:y + h, x:x + w, 2][active_pixels]
                mean_saturation = float(np.mean(saturation_values)) / 255.0
                mean_brightness = float(np.mean(brightness_values)) / 255.0
                center_bias = max(0.25, 1.0 - abs((cx / float(roi.shape[1])) - 0.5) * 1.2)
                upper_bias = max(0.35, 1.0 - (cy / float(max(roi.shape[0], 1))) * 0.7)
                area_score = min(1.0, area / 450.0)
                score = (
                    0.34 * area_score
                    + 0.28 * min(1.0, circularity)
                    + 0.18 * mean_saturation
                    + 0.10 * mean_brightness
                    + 0.10 * center_bias * upper_bias
                )
                if score > best_result.confidence:
                    best_result = TrafficLightResult(
                        status=status,
                        confidence=float(score),
                        center=(int(cx) + roi_x1, int(cy)),
                        radius=int(radius),
                    )

        return best_result

    def detect_zebra_crossing(self, frame: np.ndarray) -> ZebraCrossingResult:
        height, width = frame.shape[:2]
        roi_y1 = int(height * 0.5)
        roi = frame[roi_y1:, :]
        hsv = cv2.cvtColor(roi, cv2.COLOR_BGR2HSV)
        gray = cv2.cvtColor(roi, cv2.COLOR_BGR2GRAY)
        gray = cv2.GaussianBlur(gray, (5, 5), 0)
        white_mask = cv2.inRange(hsv, np.array((0, 0, 170), dtype=np.uint8), np.array((180, 70, 255), dtype=np.uint8))
        adaptive_mask = cv2.adaptiveThreshold(gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 19, -4)
        white_mask = cv2.bitwise_and(white_mask, adaptive_mask)
        open_kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (5, 5))
        close_kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (31, 7))
        white_mask = cv2.morphologyEx(white_mask, cv2.MORPH_OPEN, open_kernel)
        white_mask = cv2.morphologyEx(white_mask, cv2.MORPH_CLOSE, close_kernel)

        contours, _ = cv2.findContours(white_mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        stripes = []
        for contour in contours:
            area = cv2.contourArea(contour)
            if area < 350:
                continue
            x, y, w, h = cv2.boundingRect(contour)
            if w < max(60, int(width * 0.08)) or h < 8:
                continue
            rect = cv2.minAreaRect(contour)
            (_, _), (rect_w, rect_h), angle = rect
            long_side = max(rect_w, rect_h)
            short_side = max(1.0, min(rect_w, rect_h))
            if long_side < width * 0.12 or short_side < 8:
                continue
            aspect_ratio = long_side / short_side
            if aspect_ratio < 2.5:
                continue
            normalized_angle = angle if rect_w >= rect_h else angle + 90.0
            if abs(normalized_angle) > 20 and abs(abs(normalized_angle) - 180.0) > 20:
                continue
            fill_ratio = area / float(max(1, w * h))
            if fill_ratio < 0.22:
                continue
            center_y = y + h / 2.0
            bottom_bias = 0.4 + 0.6 * min(1.0, center_y / float(max(1, roi.shape[0])))
            width_score = min(1.0, long_side / max(1.0, width * 0.35))
            area_score = min(1.0, area / 2500.0)
            score = 0.30 * width_score + 0.25 * min(1.0, aspect_ratio / 6.0) + 0.20 * fill_ratio + 0.15 * bottom_bias + 0.10 * area_score
            stripes.append((x, y, w, h, area, score, x + w / 2.0))

        if len(stripes) < 3:
            return ZebraCrossingResult(detected=False, direction="UNKNOWN", confidence=0.0)

        stripes.sort(key=lambda item: item[4] * item[5], reverse=True)
        top_stripes = stripes[:6]
        vertical_positions = [item[1] + item[3] / 2.0 for item in top_stripes]
        vertical_spread = max(vertical_positions) - min(vertical_positions)
        if vertical_spread < 20 and len(top_stripes) < 4:
            return ZebraCrossingResult(detected=False, direction="UNKNOWN", confidence=0.0)

        total_weight = sum(item[4] * item[5] for item in top_stripes)
        weighted_center = sum(item[6] * item[4] * item[5] for item in top_stripes) / max(1.0, total_weight)
        horizontal_ratio = weighted_center / float(width)

        if horizontal_ratio < 0.44:
            direction = "LEFT"
        elif horizontal_ratio > 0.56:
            direction = "RIGHT"
        else:
            direction = "CENTER"

        confidence = min(1.0, 0.55 * min(1.0, len(top_stripes) / 5.0) + 0.45 * min(1.0, vertical_spread / 100.0))
        return ZebraCrossingResult(
            detected=True,
            direction=direction,
            confidence=float(confidence),
            center_x=int(weighted_center),
            stripe_count=len(top_stripes),
        )

    def build_payload(self, traffic_light: TrafficLightResult, zebra_crossing: ZebraCrossingResult) -> dict:
        confidence = max(traffic_light.confidence, zebra_crossing.confidence)
        payload = {
            "trafficLightStatus": traffic_light.status,
            "zebraCrossingDetected": zebra_crossing.detected,
            "zebraCrossingDirection": zebra_crossing.direction,
            "vehicleApproaching": False,
            "confidence": round(confidence, 3),
            "source": "vision-demo",
        }
        return payload

    def maybe_report(self, payload: dict) -> None:
        now = time.time()
        signature = (
            payload.get("trafficLightStatus"),
            payload.get("zebraCrossingDetected"),
            payload.get("zebraCrossingDirection"),
        )
        if signature == self.pending_signature:
            self.pending_signature_streak += 1
        else:
            self.pending_signature = signature
            self.pending_signature_streak = 1

        signature_changed = signature != self.last_payload_signature
        periodic_due = now - self.last_report_at >= self.force_report_interval
        should_report = False
        if signature_changed and self.pending_signature_streak >= self.min_report_streak:
            should_report = True
        if periodic_due:
            should_report = True
        if payload.get("confidence", 0.0) < 0.18 and signature == ("UNKNOWN", False, "UNKNOWN") and not periodic_due:
            should_report = False
        if not should_report:
            return

        url = f"{self.api_base}/mini/devices/{self.device_id}/crossing-assist"
        try:
            response = requests.post(url, json=payload, timeout=2.5)
            response.raise_for_status()
            data = response.json()
            self.last_report_ok = bool(data.get("code") == 200)
            self.last_payload_signature = signature
            self.last_report_at = now
            print(f"[REPORT] {payload} -> {data.get('msg', 'OK')}")
        except Exception as exc:
            self.last_report_ok = False
            print(f"[REPORT ERROR] {exc}")

    def draw_overlay(
        self,
        frame: np.ndarray,
        traffic_light: TrafficLightResult,
        zebra_crossing: ZebraCrossingResult,
        payload: dict,
        next_forced_report_in: float,
    ) -> np.ndarray:
        if traffic_light.center:
            color = {
                "RED": (0, 0, 255),
                "YELLOW": (0, 255, 255),
                "GREEN": (0, 255, 0),
            }.get(traffic_light.status, (255, 255, 255))
            cv2.circle(frame, traffic_light.center, max(traffic_light.radius, 10), color, 2)

        if zebra_crossing.center_x is not None:
            cv2.line(frame, (zebra_crossing.center_x, int(frame.shape[0] * 0.55)), (zebra_crossing.center_x, frame.shape[0] - 20), (255, 0, 0), 2)

        lines = [
            f"Traffic Light: {traffic_light.status} ({traffic_light.confidence:.2f})",
            f"Zebra: {'YES' if zebra_crossing.detected else 'NO'} / {zebra_crossing.direction} ({zebra_crossing.confidence:.2f})",
            f"Recommendation Basis: {payload['trafficLightStatus']} + {payload['zebraCrossingDirection']}",
            f"Backend Report: {'OK' if self.last_report_ok else 'PENDING'}",
            f"Detect Interval: {self.detect_interval:.2f}s | Report Interval: {self.report_interval:.1f}s",
            f"Next Forced Report In: {next_forced_report_in:.1f}s",
            "Press Q or ESC to exit",
        ]

        y = 32
        for text in lines:
            cv2.putText(frame, text, (16, y), cv2.FONT_HERSHEY_SIMPLEX, 0.68, (20, 20, 20), 4, cv2.LINE_AA)
            cv2.putText(frame, text, (16, y), cv2.FONT_HERSHEY_SIMPLEX, 0.68, (255, 255, 255), 1, cv2.LINE_AA)
            y += 30

        center_x = frame.shape[1] // 2
        cv2.line(frame, (center_x, 0), (center_x, frame.shape[0]), (180, 180, 180), 1)
        cv2.rectangle(frame, (0, int(frame.shape[0] * 0.48)), (frame.shape[1], frame.shape[0]), (120, 120, 120), 1)
        return frame


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--api-base", default="http://127.0.0.1:8081/api")
    parser.add_argument("--device-id", default="ESP32_001")
    parser.add_argument("--camera", type=int, default=0)
    parser.add_argument("--interval", type=float, default=1.5)
    parser.add_argument("--detect-interval", type=float, default=0.2)
    parser.add_argument("--width", type=int, default=960)
    parser.add_argument("--no-preview", action="store_true")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    demo = CrossingAssistVisionDemo(
        api_base=args.api_base,
        device_id=args.device_id,
        camera_index=args.camera,
        report_interval=args.interval,
        detect_interval=args.detect_interval,
        frame_width=args.width,
        preview=not args.no_preview,
    )
    demo.run()


if __name__ == "__main__":
    main()
