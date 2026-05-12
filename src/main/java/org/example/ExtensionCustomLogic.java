package org.example;

import com.nandbox.bots.api.Nandbox;
import com.nandbox.bots.api.NandboxClient;
import com.nandbox.bots.api.data.*;
import com.nandbox.bots.api.inmessages.*;
import com.nandbox.bots.api.outmessages.*;
import com.nandbox.bots.api.util.*;
import com.nandbox.bots.api.test.*;

import net.minidev.json.*;
import net.minidev.json.parser.JSONParser;

import org.example.CallbackAdapter;
import java.io.FileInputStream;
import java.io.IOException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;

public class ExtensionCustomLogic extends CallbackAdapter {
    private Nandbox.Api api;

    private static final String OPENWEATHER_API_BASE = "https://api.openweathermap.org/data/2.5/weather";
    private static final String OPENWEATHER_API_KEY = "123456789";

    public static void main(String[] args) throws Exception {
        String TOKEN = "";
	Properties properties = new Properties();
        try {
            FileInputStream input = new FileInputStream("token.properties");
            try {
                properties.load(input);
                TOKEN = properties.getProperty("Token");
                System.out.println("Token123456: " + TOKEN);
            } finally {
                input.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        NandboxClient client = NandboxClient.get();
        client.connect(TOKEN, new ExtensionCustomLogic());
    }

    @Override
    public void onConnect(Nandbox.Api api) {
        this.api = api;
    }

    @Override
    public void onReceive(IncomingMessage incomingMsg) {
        if (incomingMsg == null) {
            return;
        }
        if (incomingMsg.getChat() == null || incomingMsg.getFrom() == null) {
            return;
        }

        String chatId = incomingMsg.getChat().getId();
        String text = incomingMsg.getText();
        String reference = Utils.getUniqueId();
        String userId = incomingMsg.getFrom().getId();
        String appId = incomingMsg.getAppId();
        Integer chatSettings = incomingMsg.getChatSettings();

        if (text == null) {
            return;
        }

        String trimmed = text.trim();
        if (trimmed.length() == 0) {
            return;
        }

        if (equalsAnyIgnoreCase(trimmed, "/start", "start", "help", "/help")) {
            sendText(chatId, "Your weathersss guide\n\nUse: /getweather [city]\nExample: /getweather London", reference, userId, chatSettings, appId);
            return;
        }

        if (startsWithCommand(trimmed, "/getweather") || startsWithCommand(trimmed, "getweather")) {
            String city = extractCommandArgument(trimmed);
            if (city == null || city.trim().length() == 0) {
                sendText(chatId, "Please provide a city.\nExample: /getweather London", reference, userId, chatSettings, appId);
                return;
            }

            String waitingRef = Utils.getUniqueId();
            sendText(chatId, "Fetching current weather for: " + city + " ...", waitingRef, userId, chatSettings, appId);

            try {
                WeatherResult result = fetchWeather(city);
                if (result == null || !result.ok) {
                    String msg = (result != null && result.errorMessage != null) ? result.errorMessage : "Unable to fetch weather right now.";
                    sendText(chatId, msg, Utils.getUniqueId(), userId, chatSettings, appId);
                    return;
                }

                String reply = formatWeather(result);
                sendText(chatId, reply, Utils.getUniqueId(), userId, chatSettings, appId);
            } catch (Exception e) {
                sendText(chatId, "Error while fetching weather. Please try again later.", Utils.getUniqueId(), userId, chatSettings, appId);
            }
            return;
        }

        sendText(chatId, "I can fetch current weather. Use: /getweather [city]", reference, userId, chatSettings, appId);
    }

    @Override
    public void onReceive(JSONObject obj) {
        if (obj == null) {
            return;
        }
        Object t = obj.get("type");
        if (t != null) {
            String type = String.valueOf(t);
            if ("message".equalsIgnoreCase(type) || "incomingMessage".equalsIgnoreCase(type) || "chat_message".equalsIgnoreCase(type)) {
                return;
            }
        }
        if (obj.containsKey("message") || obj.containsKey("chat") || obj.containsKey("text")) {
            return;
        }
    }

    @Override
    public void onClose() {}

    @Override
    public void onError() {}

    @Override
    public void onChatMenuCallBack(ChatMenuCallback chatMenuCallback) {}

    @Override
    public void onInlineMessageCallback(InlineMessageCallback inlineMsgCallback) {}

    @Override
    public void onMessagAckCallback(MessageAck msgAck) {}

    @Override
    public void onUserJoinedBot(User user) {}

    @Override
    public void onChatMember(ChatMember chatMember) {}

    @Override
    public void onChatAdministrators(ChatAdministrators chatAdministrators) {}

    @Override
    public void userStartedBot(User user) {}

    @Override
    public void onMyProfile(User user) {}

    @Override
    public void onProductDetail(ProductItemResponse productItem) {}

    @Override
    public void onCollectionProduct(GetProductCollectionResponse collectionProduct) {}

    @Override
    public void listCollectionItemResponse(ListCollectionItemResponse collections) {}

    @Override
    public void onUserDetails(User user, String appId) {}

    @Override
    public void userStoppedBot(User user) {}

    @Override
    public void userLeftBot(User user) {}

    @Override
    public void permanentUrl(PermanentUrl permenantUrl) {}

    @Override
    public void onChatDetails(Chat chat, String appId) {}

    @Override
    public void onInlineSearh(InlineSearch inlineSearch) {}

    @Override
    public void onBlackListPattern(Pattern pattern) {}

    @Override
    public void onWhiteListPattern(Pattern pattern) {}

    @Override
    public void onBlackList(BlackList blackList) {}

    @Override
    public void onDeleteBlackList(List_ak blackList) {}

    @Override
    public void onWhiteList(WhiteList whiteList) {}

    @Override
    public void onDeleteWhiteList(List_ak whiteList) {}

    @Override
    public void onScheduleMessage(IncomingMessage incomingScheduleMsg) {}

    @Override
    public void onWorkflowDetails(WorkflowDetails workflowDetails) {}

    @Override
    public void onCreateChat(Chat chat) {}

    @Override
    public void onMenuCallBack(MenuCallback menuCallback) {}

    private void sendText(String chatId, String msg, String reference, String userId, Integer chatSettings, String appId) {
        if (this.api == null) {
            return;
        }
        this.api.sendText(
            chatId,
            msg,
            reference,
            null,
            userId,
            new Integer(0),
            Boolean.FALSE,
            chatSettings,
            null,
            null,
            null,
            appId
        );
    }

    private static boolean equalsAnyIgnoreCase(String s, String a, String b, String c, String d) {
        if (s == null) return false;
        return s.equalsIgnoreCase(a) || s.equalsIgnoreCase(b) || s.equalsIgnoreCase(c) || s.equalsIgnoreCase(d);
    }

    private static boolean startsWithCommand(String text, String cmd) {
        if (text == null || cmd == null) return false;
        if (text.equalsIgnoreCase(cmd)) return true;
        if (text.length() > cmd.length() && text.regionMatches(true, 0, cmd, 0, cmd.length())) {
            char ch = text.charAt(cmd.length());
            return Character.isWhitespace(ch);
        }
        return false;
    }

    private static String extractCommandArgument(String text) {
        if (text == null) return null;
        String t = text.trim();
        int space = t.indexOf(' ');
        if (space < 0) {
            return "";
        }
        return t.substring(space + 1).trim();
    }

    private static class WeatherResult {
        boolean ok;
        String cityName;
        String country;
        String description;
        double tempC;
        double feelsLikeC;
        int humidity;
        double windSpeed;
        String errorMessage;
    }

    private static WeatherResult fetchWeather(String city) throws Exception {
        WeatherResult res = new WeatherResult();
        if (city == null || city.trim().length() == 0) {
            res.ok = false;
            res.errorMessage = "City is required.";
            return res;
        }

        String q = URLEncoder.encode(city.trim(), "UTF-8");
        String urlStr = OPENWEATHER_API_BASE + "?q=" + q + "&appid=" + URLEncoder.encode(OPENWEATHER_API_KEY, "UTF-8") + "&units=metric";

        HttpURLConnection conn = null;
        InputStream is = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(20000);

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                is = conn.getInputStream();
            } else {
                is = conn.getErrorStream();
            }

            String body = readAll(is);
            JSONParser parser = new JSONParser(JSONParser.MODE_PERMISSIVE);
            Object parsed = parser.parse(body);
            if (!(parsed instanceof JSONObject)) {
                res.ok = false;
                res.errorMessage = "Unexpected response from weather service.";
                return res;
            }
            JSONObject json = (JSONObject) parsed;

            Object codObj = json.get("cod");
            int cod = 0;
            if (codObj instanceof Number) {
                cod = ((Number) codObj).intValue();
            } else if (codObj != null) {
                try { cod = Integer.parseInt(String.valueOf(codObj)); } catch (Exception ex) { cod = 0; }
            }

            if (code < 200 || code >= 300 || (cod != 0 && cod != 200)) {
                String msg = stringVal(json.get("message"));
                if (msg == null || msg.length() == 0) {
                    msg = "City not found. Please check the spelling.";
                } else {
                    msg = "Weather service error: " + msg;
                }
                res.ok = false;
                res.errorMessage = msg;
                return res;
            }

            res.cityName = stringVal(json.get("name"));

            JSONObject sys = objVal(json.get("sys"));
            res.country = sys != null ? stringVal(sys.get("country")) : null;

            JSONArray weatherArr = arrVal(json.get("weather"));
            if (weatherArr != null && weatherArr.size() > 0) {
                Object first = weatherArr.get(0);
                if (first instanceof JSONObject) {
                    res.description = stringVal(((JSONObject) first).get("description"));
                }
            }

            JSONObject main = objVal(json.get("main"));
            if (main != null) {
                res.tempC = doubleVal(main.get("temp"));
                res.feelsLikeC = doubleVal(main.get("feels_like"));
                res.humidity = intVal(main.get("humidity"));
            }

            JSONObject wind = objVal(json.get("wind"));
            if (wind != null) {
                res.windSpeed = doubleVal(wind.get("speed"));
            }

            res.ok = true;
            return res;
        } catch (Exception e) {
            res.ok = false;
            res.errorMessage = "Unable to reach weather service.";
            return res;
        } finally {
            if (is != null) {
                try { is.close(); } catch (Exception ex) {}
            }
            if (conn != null) {
                try { conn.disconnect(); } catch (Exception ex) {}
            }
        }
    }

    private static String formatWeather(WeatherResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Current weather");
        if (r.cityName != null && r.cityName.length() > 0) {
            sb.append(" for ").append(r.cityName);
            if (r.country != null && r.country.length() > 0) {
                sb.append(", ").append(r.country);
            }
        }
        sb.append(":\n");
        if (r.description != null && r.description.length() > 0) {
            sb.append("- Condition: ").append(capitalize(r.description)).append("\n");
        }
        sb.append("- Temperature: ").append(trimDouble(r.tempC)).append("\u00b0C");
        sb.append(" (feels like ").append(trimDouble(r.feelsLikeC)).append("\u00b0C)\n");
        sb.append("- Humidity: ").append(r.humidity).append("%\n");
        sb.append("- Wind: ").append(trimDouble(r.windSpeed)).append(" m/s");
        return sb.toString();
    }

    private static String capitalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.length() == 0) return t;
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    private static String trimDouble(double d) {
        String s = String.valueOf(d);
        if (s.indexOf('.') >= 0) {
            while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
            if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private static String readAll(InputStream is) throws Exception {
        if (is == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    private static String stringVal(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static JSONObject objVal(Object o) {
        if (o instanceof JSONObject) return (JSONObject) o;
        return null;
    }

    private static JSONArray arrVal(Object o) {
        if (o instanceof JSONArray) return (JSONArray) o;
        return null;
    }

    private static double doubleVal(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        if (o == null) return 0.0;
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return 0.0; }
    }

    private static int intVal(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        if (o == null) return 0;
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return 0; }
    }
}
