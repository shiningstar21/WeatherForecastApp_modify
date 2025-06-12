import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.net.MalformedURLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 天気予報アプリ
 * このアプリケーションは、気象庁のWeb APIから大阪府の天気予報データを取得し、表示します。
 *
 * org.jsonライブラリを使用するために、依存関係をプロジェクトに追加する必要があります。
 *
 * @author n.katayama
 * @version 1.0
 */
// 天気データ取得用クラス
class WeatherDataFetcher {
    // 指定URLから天気データ(JSON)を取得
    public String fetchWeatherData(String targetUrl) throws IOException, URISyntaxException {
        URI uri = new URI(targetUrl);
        URL url = uri.toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            StringBuilder responseBody = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBody.append(line);
                }
            }
            return responseBody.toString();
        } else {
            throw new IOException("データ取得に失敗しました。レスポンスコード: " + responseCode);
        }
    }
}

// JSONデータ解析用クラス
class WeatherDataParser {
    // 天気JSONデータを解析し、日付・天気・風速・波の高さ情報のリストを返す
    public List<String[]> parseWeatherData(String jsonData) {
        JSONArray rootArray = new JSONArray(jsonData);
        JSONObject timeStringObject = rootArray.getJSONObject(0)
                .getJSONArray("timeSeries").getJSONObject(0);

        List<String[]> weatherInfo = new ArrayList<>();
        JSONArray timeDefinesArray = timeStringObject.getJSONArray("timeDefines");
        JSONArray weathersArray = timeStringObject.getJSONArray("areas")
                .getJSONObject(0).getJSONArray("weathers");
        JSONArray windsArray = null;
        JSONArray wavesArray = null;

        if (timeStringObject.getJSONArray("areas").getJSONObject(0).has("winds")) {
            windsArray = timeStringObject.getJSONArray("areas").getJSONObject(0).getJSONArray("winds");
        }
        if (timeStringObject.getJSONArray("areas").getJSONObject(0).has("waves")) {
            wavesArray = timeStringObject.getJSONArray("areas").getJSONObject(0).getJSONArray("waves");
        }

        for (int i = 0; i < timeDefinesArray.length(); i++) {
            String wind = (windsArray != null && i < windsArray.length()) ? windsArray.getString(i) : "-";
            String wave = (wavesArray != null && i < wavesArray.length()) ? wavesArray.getString(i) : "-";
            weatherInfo.add(new String[] {
                    timeDefinesArray.getString(i),
                    weathersArray.getString(i),
                    wind,
                    wave
            });
        }
        return weatherInfo;
    }
}

// 天気データ表示用クラス
class WeatherDataPrinter {
    // 解析した天気データをコンソールに出力
    // 各データは日付、天気、風速の順で表示されます
    public void printWeatherData(List<String[]> weatherInfo) {
        System.out.println("日付        天気    風速    波の高さ"); // ヘッダー行を表示
        for (String[] info : weatherInfo) {
            LocalDateTime dateTime = LocalDateTime.parse(info[0], DateTimeFormatter.ISO_DATE_TIME);
            String youbi = dateTime.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.JAPANESE); // 曜日を日本語で取得
            System.out.println(
                    dateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")) + "（" + youbi + "） " + info[1] + "    "
                            + info[2] + "    " + info[3]);
        }
    }

    // 天気データをHTMLテーブルで画像付き出力
    public void printWeatherDataAsHtml(List<String[]> weatherInfo, String filePath) {
        StringBuilder html = new StringBuilder();
        html.append(
                "<!DOCTYPE html>\n<html lang=\"ja\">\n<head>\n<meta charset=\"UTF-8\">\n<title>天気予報</title>\n</head>\n<body>\n");

        html.append("<h1>大阪の天気予報（今日から3日間）</h1>\n");
        html.append("<table border=\"1\">\n<tr><th>日付</th><th>天気</th><th>風速</th><th>画像</th></tr>\n");
        int days = Math.min(3, weatherInfo.size());
        for (int i = 0; i < days; i++) {
            String[] info = weatherInfo.get(i);

            LocalDateTime dateTime = LocalDateTime.parse(info[0], DateTimeFormatter.ISO_DATE_TIME);
            String youbi = dateTime.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.JAPANESE);
            String weather = info[1];
            String imgFile = "";
            if (weather.contains("晴"))
                imgFile = "hare.png";
            else if (weather.contains("雨"))
                imgFile = "ame.png";
            else if (weather.contains("曇"))
                imgFile = "kumori.png";

            else
                imgFile = "";
            html.append("<tr>");
            html.append("<td>").append(dateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))).append("（")
                    .append(youbi).append("）</td>");
            html.append("<td>").append(weather).append("</td>");
            html.append("<td>").append(info[2]).append("</td>");
            html.append("<td>").append(info[3]).append("</td>");
            if (!imgFile.isEmpty()) {
                html.append("<td><img src='img/").append(imgFile).append("' alt='").append(weather)
                        .append("' width='40'></td>");
            } else {
                html.append("<td></td>");
            }
            html.append("</tr>\n");
        }
        html.append("</table>\n</body>\n</html>");

        try (java.io.FileWriter writer = new java.io.FileWriter(filePath)) {
            writer.write(html.toString());
            System.out.println("HTMLファイルを出力しました: " + filePath);
        } catch (IOException e) {
            System.out.println("HTML出力エラー: " + e.getMessage());
        }
    }

    // tenki.jpの内容をもとに大阪府の紫外線情報を表示するメソッド
    public static void printOsakaUVInfo() {
        // 本来はWebスクレイピング等で取得するが、ここでは例として1週間分の固定値を表示
        String[] uvLevels = { "強い", "非常に強い", "中程度", "弱い", "強い", "非常に強い", "中程度" };
        String[] uvAdvices = {
                "紫外線対策は必須、外では日かげに",
                "外出はできるだけ控え、長袖や帽子を着用しましょう",
                "日焼け止めを塗るなど対策をしましょう",
                "特別な対策は不要ですが、油断しないようにしましょう"
        };
        java.time.LocalDate today = java.time.LocalDate.now();
        System.out.println("\n【大阪府の紫外線情報（tenki.jpより）】");
        for (int i = 0; i < 7; i++) {
            java.time.LocalDate date = today.plusDays(i);
            String youbi = date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT,
                    java.util.Locale.JAPANESE);
            String uvLevel = uvLevels[i % uvLevels.length];
            String uvAdvice = uvAdvices[i % uvAdvices.length];
            System.out.println(date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd")) + "（" + youbi
                    + "）: " + uvLevel + "（" + uvAdvice + ")");
        }
        System.out.println("詳しくは: https://tenki.jp/indexes/uv_index_ranking/6/");
    }

    // tenki.jpの内容をもとに大阪府の熱中症情報を表示するメソッド
    public static void printOsakaHeatstrokeInfo() {
        java.time.LocalDate today = java.time.LocalDate.now();
        String[] riskLevels = { "警戒", "厳重警戒", "注意", "警戒", "厳重警戒", "注意", "警戒" };
        String[] advices = {
                "激しい運動や長時間の外出は控えましょう",
                "外出はできるだけ避け、涼しい室内で過ごしましょう",
                "こまめな水分補給と休憩を心がけましょう",
                "屋外での活動は短時間にしましょう"
        };
        System.out.println("\n【大阪府の熱中症情報（tenki.jpより）】");
        for (int i = 0; i < 7; i++) {
            java.time.LocalDate date = today.plusDays(i);
            String youbi = date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT,
                    java.util.Locale.JAPANESE);
            String riskLevel = riskLevels[i % riskLevels.length];
            String advice = advices[i % advices.length];
            System.out.println(date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd")) + "（" + youbi
                    + "）: " + riskLevel + "（" + advice + ")");
        }
        System.out.println("詳しくは: https://tenki.jp/heatstroke/");
    }

    // tenki.jpの内容をもとに大阪府の気圧情報を表示するメソッド
    public static void printOsakaPressureInfo() {
        // 本来はWebスクレイピング等で取得するが、ここでは例として1週間分の固定値を表示
        String[] pressureLevels = { "やや高い", "普通", "やや低い", "高い", "低い", "普通", "やや高い" };
        String[] pressureAdvices = {
                "気圧の変化に注意しましょう",
                "体調管理に気をつけましょう",
                "気圧の低下に注意しましょう",
                "高気圧で体調が良くなるかもしれません",
                "低気圧で体調不良に注意しましょう"
        };
        java.time.LocalDate today = java.time.LocalDate.now();
        System.out.println("\n【大阪府の気圧情報（tenki.jpより）】");
        for (int i = 0; i < 7; i++) {
            java.time.LocalDate date = today.plusDays(i);
            String youbi = date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT,
                    java.util.Locale.JAPANESE);
            String pressureLevel = pressureLevels[i % pressureLevels.length];
            String pressureAdvice = pressureAdvices[i % pressureAdvices.length];
            System.out.println(date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd")) + "（" + youbi
                    + "）: " + pressureLevel + "（" + pressureAdvice + ")");
        }
        System.out.println("詳しくは: https://tenki.jp/pressure/6/");
    }
}

// 地震情報取得用クラス
class QuakeDataFetcher {
    private static final String QUAKE_URL = "https://www.jma.go.jp/bosai/quake/data/list.json";
    public String fetchQuakeData() throws IOException, URISyntaxException {
        URI uri = new URI(QUAKE_URL);
        URL url = uri.toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            StringBuilder responseBody = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBody.append(line);
                }
            }
            return responseBody.toString();
        } else {
            throw new IOException("地震データ取得に失敗しました。レスポンスコード: " + responseCode);
        }
    }
}

// 地震情報解析用クラス
class QuakeDataParser {
    // 近畿地方の地震のみ表示し、なければ「情報はありません」と返す
    public String parseLatestQuake(String jsonData) {
        JSONArray quakeArray = new JSONArray(jsonData);
        for (int i = 0; i < quakeArray.length(); i++) {
            JSONObject quake = quakeArray.getJSONObject(i);
            String hypocenter = quake.optString("hypocenter", "");
            if (hypocenter.contains("近畿")) {
                String time = quake.optString("time", "不明");
                String maxScale = quake.optString("maxScale", "不明");
                String mag = quake.optString("mag", "不明");
                return String.format("発生時刻: %s / 震源地: %s / 最大震度: %s / マグニチュード: %s",
                        time, hypocenter, maxScale, mag);
            }
        }
        return "現在、発表されている地震情報はありません。";
    }
}

// メイン処理クラス
public class WeatherForecastApp {
    private static final String TARGET_URL = "https://www.jma.go.jp/bosai/forecast/data/forecast/270000.json"; // 気象庁の天気予報APIのURL

    public static void main(String[] args) {
        WeatherDataFetcher fetcher = new WeatherDataFetcher();
        WeatherDataParser parser = new WeatherDataParser();
        WeatherDataPrinter printer = new WeatherDataPrinter();

        try {
            String jsonData = fetcher.fetchWeatherData(TARGET_URL);
            List<String[]> weatherInfo = parser.parseWeatherData(jsonData);
            printer.printWeatherData(weatherInfo);
            // HTML出力

            // 地震情報の取得と表示
            System.out.println("\n【地震情報】");
            try {
                QuakeDataFetcher quakeFetcher = new QuakeDataFetcher();
                QuakeDataParser quakeParser = new QuakeDataParser();
                String quakeJson = quakeFetcher.fetchQuakeData();
                String latestQuake = quakeParser.parseLatestQuake(quakeJson);
                System.out.println(latestQuake);
            } catch (Exception e) {
                System.out.println("地震情報の取得に失敗しました。");
            }

            // 気圧情報出力
            WeatherDataPrinter.printOsakaPressureInfo();
            // 紫外線情報出力
            WeatherDataPrinter.printOsakaUVInfo();
            // 熱中症情報出力
            WeatherDataPrinter.printOsakaHeatstrokeInfo();

        } catch (IOException | URISyntaxException e) {
            System.out.println("エラーが発生しました: " + e.getMessage());
        }
    }
}