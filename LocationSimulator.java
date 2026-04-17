import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class LocationSimulator {
    private static final String API_URL = "http://localhost:8081/api/sensor-data";
    private static final String[] DEVICE_IDS = {"DEVICE001", "DEVICE002", "DEVICE003", "DEVICE004"};
    private static final double BASE_LAT = 39.9042;  // 北京基准纬度
    private static final double BASE_LNG = 116.4074; // 北京基准经度
    private static final int REPORT_INTERVAL_MS = 30000;
    private static final Random random = new Random();

    public static void main(String[] args) {
        System.out.println("开始模拟多设备低频位置上报...");
        System.out.println("按 Ctrl+C 停止\n");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n已停止模拟");
        }));

        while (true) {
            try {
                for (String currentDeviceId : DEVICE_IDS) {
                    sendLocationData(currentDeviceId);
                    Thread.sleep(800);
                }
                Thread.sleep(REPORT_INTERVAL_MS);
            } catch (Exception e) {
                System.err.println("错误: " + e.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    private static void sendLocationData(String deviceId) throws Exception {
        double[] baseCoordinate = getDeviceBaseCoordinate(deviceId);
        double lat = baseCoordinate[0] + (random.nextDouble() - 0.5) * 0.006;
        double lng = baseCoordinate[1] + (random.nextDouble() - 0.5) * 0.006;
        boolean simulatedFall = random.nextDouble() < 0.03;
        double accelX = simulatedFall ? randomSigned(2.4, 3.1) : randomSigned(0.75, 1.15);
        double accelY = simulatedFall ? randomSigned(2.2, 2.9) : randomSigned(0.75, 1.10);
        double accelZ = simulatedFall ? randomSigned(0.05, 0.35) : randomSigned(0.85, 1.20);

        String json = String.format(
            "{\"deviceId\":\"%s\",\"obstacleDistance\":%.1f,\"isFall\":false," +
            "\"accelX\":%.3f,\"accelY\":%.3f,\"accelZ\":%.3f," +
            "\"latitude\":%.6f,\"longitude\":%.6f,\"temperature\":%.1f,\"humidity\":%.1f," +
            "\"dataTime\":\"%s\"}",
            deviceId,
            30 + random.nextDouble() * 100,
            accelX, accelY, accelZ,
            lat, lng,
            20 + random.nextDouble() * 10,
            40 + random.nextDouble() * 20,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            System.out.printf("[%s] 已上报位置: (%.6f, %.6f)%s%n",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                lat, lng,
                simulatedFall ? "，并模拟一次跌倒冲击" : "");
        } else {
            System.err.println("上报失败: HTTP " + response.statusCode());
        }
    }

    private static double[] getDeviceBaseCoordinate(String deviceId) {
        return switch (deviceId) {
            case "DEVICE002" -> new double[]{39.9060, 116.4108};
            case "DEVICE003" -> new double[]{39.9015, 116.4036};
            case "DEVICE004" -> new double[]{39.9083, 116.4142};
            default -> new double[]{BASE_LAT, BASE_LNG};
        };
    }

    private static double randomSigned(double minAbs, double maxAbs) {
        double value = minAbs + random.nextDouble() * (maxAbs - minAbs);
        return random.nextBoolean() ? value : -value;
    }
}
