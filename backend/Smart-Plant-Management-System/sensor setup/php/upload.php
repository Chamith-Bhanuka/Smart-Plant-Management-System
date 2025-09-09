<?php
// upload.php

// --- Database credentials ---
$host = 'localhost';
$db   = 'databasename';
$user = 'root';
$pass = 'password'; // leave blank if no MySQL password
$uploadDir = __DIR__ . '/images';  

// Ensure images directory exists
if (!is_dir($uploadDir)) {
    mkdir($uploadDir, 0755, true);
}

// Generate unique filename
$filename = uniqid('img_') . '.jpg';
$imagePath = $uploadDir . '/' . $filename;

// Read raw JPEG data
$data = file_get_contents('php://input');

if (empty($data)) {
    http_response_code(400);
    echo json_encode(['error' => 'No data received']);
    exit;
}

// Save file
file_put_contents($imagePath, $data);

// Insert into MySQL
$conn = new mysqli($host, $user, $pass, $db);
if ($conn->connect_error) {
    http_response_code(500);
    echo json_encode(['error' => 'DB connect failed']);
    exit;
}
$stmt = $conn->prepare("INSERT INTO images (filename) VALUES (?)");
$stmt->bind_param('s', $filename);
$stmt->execute();
$stmt->close();
$conn->close();

// Respond with path
echo json_encode(['filename' => 'images/' . $filename]);
?>
