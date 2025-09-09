#include "esp_camera.h"
#include <WiFi.h>
#include <HTTPClient.h>
#include <WebServer.h>
#include "DHT.h"

// ==== Replace with your WiFi credentials ====
const char *ssid = "username";
const char *password = "pdw";
const char* serverUrl = "http://ipV4/upload.php"; // Your PHP endpoint

// Pins
#define FLASH_LED_PIN 4
#define DHTPIN 13            // DHT22 data pin
#define DHTTYPE DHT22
#define SOIL_PIN 14          // Analog pin for soil sensor

bool continuousCapture = false; //new
unsigned long lastCaptureTime = 0; //new
const unsigned long captureInterval = 60000; // 1 minute in ms new


DHT dht(DHTPIN, DHTTYPE);
WebServer server(80);

// ==== AI Thinker Camera Pins ====
#define PWDN_GPIO_NUM     32
#define RESET_GPIO_NUM    -1
#define XCLK_GPIO_NUM      0
#define SIOD_GPIO_NUM     26
#define SIOC_GPIO_NUM     27
#define Y9_GPIO_NUM       35
#define Y8_GPIO_NUM       34
#define Y7_GPIO_NUM       39
#define Y6_GPIO_NUM       36
#define Y5_GPIO_NUM       21
#define Y4_GPIO_NUM       19
#define Y3_GPIO_NUM       18
#define Y2_GPIO_NUM        5
#define VSYNC_GPIO_NUM    25
#define HREF_GPIO_NUM     23
#define PCLK_GPIO_NUM     22

void setupCamera() {
  camera_config_t config;
  config.ledc_channel = LEDC_CHANNEL_0;
  config.ledc_timer   = LEDC_TIMER_0;
  config.pin_d0       = Y2_GPIO_NUM;
  config.pin_d1       = Y3_GPIO_NUM;
  config.pin_d2       = Y4_GPIO_NUM;
  config.pin_d3       = Y5_GPIO_NUM;
  config.pin_d4       = Y6_GPIO_NUM;
  config.pin_d5       = Y7_GPIO_NUM;
  config.pin_d6       = Y8_GPIO_NUM;
  config.pin_d7       = Y9_GPIO_NUM;
  config.pin_xclk     = XCLK_GPIO_NUM;
  config.pin_pclk     = PCLK_GPIO_NUM;
  config.pin_vsync    = VSYNC_GPIO_NUM;
  config.pin_href     = HREF_GPIO_NUM;
  config.pin_sscb_sda = SIOD_GPIO_NUM;
  config.pin_sscb_scl = SIOC_GPIO_NUM;
  config.pin_pwdn     = PWDN_GPIO_NUM;
  config.pin_reset    = RESET_GPIO_NUM;
  config.xclk_freq_hz = 20000000;
  config.pixel_format = PIXFORMAT_JPEG;
  config.frame_size = FRAMESIZE_VGA;
  config.jpeg_quality = 12;
  config.fb_count = 1;

  esp_err_t err = esp_camera_init(&config);
  if (err != ESP_OK) {
    Serial.printf("Camera init failed: 0x%x\n", err);
    ESP.restart();
  }
}

void captureAndSend() {
  digitalWrite(FLASH_LED_PIN, HIGH);
  delay(200);
  camera_fb_t* fb = esp_camera_fb_get();
  if (!fb) {
    server.send(500, "text/plain", "Capture failed");
    digitalWrite(FLASH_LED_PIN, LOW);
    return;
  }

  HTTPClient http;
  http.begin(serverUrl);
  http.addHeader("Content-Type", "image/jpeg");
  int httpCode = http.POST(fb->buf, fb->len);
  esp_camera_fb_return(fb);
  digitalWrite(FLASH_LED_PIN, LOW);

  server.sendHeader("Access-Control-Allow-Origin", "*");
  if (httpCode > 0) {
    server.send(200, "text/plain", "Image uploaded");
  } else {
    server.send(500, "text/plain", "Upload failed");
  }
  http.end();
}

void handleDHT() {
  float h = dht.readHumidity();
  float t = dht.readTemperature();
  server.sendHeader("Access-Control-Allow-Origin", "*");
  if (isnan(h) || isnan(t)) {
    server.send(500, "application/json", "{\"error\":\"Sensor error\"}");
    return;
  }
  String json = "{\"temperature\":" + String(t) + ",\"humidity\":" + String(h) + "}";
  server.send(200, "application/json", json);
}

void handleSoil() {
  int value = analogRead(SOIL_PIN);
  int percent = map(value, 4095, 0, 0, 100); // Adjust for your soil sensor
  String json = "{\"raw\":" + String(value) + ",\"percent\":" + String(percent) + "}";
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.send(200, "application/json", json);
}

void startStream() {
  // Not needed, stream served directly from http://ESP32_IP/stream
  server.send(200, "text/plain", "Streaming handled on /stream");
}

void startContinuousCapture() { //new
  continuousCapture = true;
  lastCaptureTime = millis();
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.send(200, "text/plain", "Continuous capture started");
}

void stopContinuousCapture() {
  continuousCapture = false; //new
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.send(200, "text/plain", "Continuous capture stopped");
}


void setup() {
  Serial.begin(115200);
  pinMode(FLASH_LED_PIN, OUTPUT);
  digitalWrite(FLASH_LED_PIN, LOW);

  dht.begin();
  WiFi.begin(ssid, password);
  Serial.print("Connecting to WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500); Serial.print(".");
  }

  Serial.println("\nWiFi connected!");
  Serial.println("IP address: " + WiFi.localIP().toString());

  setupCamera();

  server.on("/capture", HTTP_GET, captureAndSend);
  server.on("/dht", HTTP_GET, handleDHT);
  server.on("/soil", HTTP_GET, handleSoil);
  server.on("/startCapture", HTTP_GET, startContinuousCapture);
  server.on("/stopCapture", HTTP_GET, stopContinuousCapture);

  server.begin();
}

void loop() {
  if (continuousCapture && (millis() - lastCaptureTime >= captureInterval)) { //new
    captureAndSend(); // Reuse your existing capture function
    lastCaptureTime = millis();
  }
  
  server.handleClient();
}
