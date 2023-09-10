<?php

require 'vendor/autoload.php';

use GuzzleHttp\Client;
use GuzzleHttp\Exception\RequestException;
use GuzzleHttp\Psr7\Response;

$API_BASE_URL = "http://YOURIP:5700";
$API_TOKEN = "YOUR_Authorization";

// 设置日志
$log = new Monolog\Logger('logger');
$log->pushHandler(new Monolog\Handler\StreamHandler('php://stdout', Monolog\Logger::INFO));

function main() {
    global $log;
    try {
        $record_data = get_data("/api/scripts?t=");
        $log->info("获取到的body code: " . $record_data["code"]);

        foreach ($record_data["data"] as $val) {
            try {
                if (isset($val["children"])) {
                    foreach ($val["children"] as $child_val) {
                        $child_data = get_data("/api/scripts/" . $child_val["title"] . "?path=" . $child_val["parent"] . "&t=");
                        download_file($child_data["data"], $child_val["parent"], $child_val["title"]);
                    }
                } else {
                    $child_data = get_data("/api/scripts/" . $val["title"] . "?path=&t=");
                    download_file($child_data["data"], "", $val["title"]);
                }
            } catch (Exception $e) {
                $log->error("处理子项目时出错: " . $e->getMessage());
            }
        }
        $log->info("执行完毕");
    } catch (Exception $e) {
        $log->error("An error occurred: " . $e->getMessage());
    }
}

function get_data($url_str) {
    global $API_BASE_URL, $API_TOKEN, $log;
    try {
        $timestamp = strval(intval(microtime(true) * 1000));
        $full_url = $API_BASE_URL . $url_str . $timestamp;
        $log->info($full_url);
        $headers = ["Authorization" => "Bearer " . $API_TOKEN];
        $client = new Client();
        $response = $client->get($full_url, ["headers" => $headers]);
        $statusCode = $response->getStatusCode();
        if ($statusCode >= 200 && $statusCode < 300) {
            return json_decode($response->getBody(), true);
        } else {
            throw new Exception("Request error: " . $response->getReasonPhrase());
        }
    } catch (RequestException $e) {
        throw new Exception("Request error: " . $e->getMessage());
    }
}

function download_file($body, $path, $name) {
    global $log;
    try {
        $file_path = "./" . $name;
        if ($path) {
            if (!file_exists($path)) {
                mkdir($path, 0777, true);
                chmod($path, 0777);
            }
            $file_path = $path . "/" . $name;
        }
        file_put_contents($file_path, $body);
    } catch (Exception $e) {
        throw new Exception("File download error: " . $e->getMessage());
    }
}

main();

?>