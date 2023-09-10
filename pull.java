import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Main {
    private static final String API_BASE_URL = "http://YOURIP:5700";
    private static final String API_TOKEN = "YOUR_Authorization";
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            String recordData = getData("/api/scripts?t=");
            logger.info("获取到的body code: " + recordData.get("code"));

            for (Object val : recordData.get("data")) {
                try {
                    if (((Map<String, Object>) val).containsKey("children")) {
                        for (Object childVal : ((Map<String, Object>) val).get("children")) {
                            Map<String, Object> childData = getData("/api/scripts/" + ((Map<String, Object>) childVal).get("title") + "?path=" + ((Map<String, Object>) childVal).get("parent") + "&t=");
                            downloadFile((String) childData.get("data"), (String) ((Map<String, Object>) childVal).get("parent"), (String) ((Map<String, Object>) childVal).get("title"));
                        }
                    } else {
                        Map<String, Object> childData = getData("/api/scripts/" + ((Map<String, Object>) val).get("title") + "?path=&t=");
                        downloadFile((String) childData.get("data"), "", (String) ((Map<String, Object>) val).get("title"));
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "处理子项目时出错: " + e.getMessage(), e);
                }
            }
            logger.info("执行完毕");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An error occurred: " + e.getMessage(), e);
        }
    }

    private static Map<String, Object> getData(String urlStr) throws IOException {
        try {
            String timestamp = String.valueOf(System.currentTimeMillis());
            String fullUrl = API_BASE_URL + urlStr + timestamp;
            logger.info(fullUrl);
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(fullUrl)
                    .addHeader("Authorization", "Bearer " + API_TOKEN)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected response code: " + response);
                }
                String responseBody = response.body().string();
                return new ObjectMapper().readValue(responseBody, new TypeReference<Map<String, Object>>() {});
            }
        } catch (IOException e) {
            throw new IOException("Request error: " + e.getMessage(), e);
        }
    }

    private static void downloadFile(String body, String path, String name) throws IOException {
        try {
            String filePath = Paths.get(".", name).toString();
            if (!path.isEmpty()) {
                Path directoryPath = Paths.get(path);
                if (!Files.exists(directoryPath)) {
                    Files.createDirectories(directoryPath);
                    Files.setPosixFilePermissions(directoryPath, PosixFilePermissions.fromString("rwxrwxrwx"));
                }
                filePath = Paths.get(path, name).toString();
            }
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(body.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new IOException("File download error: " + e.getMessage(), e);
        }
    }
}