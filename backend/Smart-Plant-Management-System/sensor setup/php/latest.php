<?php
ini_set('display_errors', 1);
error_reporting(E_ALL);
header('Content-Type: application/json');

$host = 'localhost';
$db   = 'databasename';
$user = 'root';
$pass = 'password';

$conn = new mysqli($host, $user, $pass, $db);
if ($conn->connect_error) {
    echo json_encode(['error' => 'Database connection failed']);
    exit;
}

$result = $conn->query("SELECT filename FROM images ORDER BY id DESC LIMIT 1");
$row = $result ? $result->fetch_assoc() : null;
$conn->close();

echo json_encode([
    'filename' => $row && $row['filename'] ? '../images/' . $row['filename'] : null
]);
?>
