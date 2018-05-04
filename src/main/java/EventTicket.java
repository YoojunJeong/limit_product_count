import org.apache.commons.lang.math.NumberUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



/**
 * Created by yoojun.jeong on 2017. 3. 20..
 */
public class EventTicket {

    private static int MAX_THREAD_COUNT = 100;
    private static ConcurrentHashMap<String, Integer>           countMap    = new ConcurrentHashMap();
    private static ConcurrentHashMap<String, Integer>           maxCountMap = new ConcurrentHashMap();
    private static ConcurrentHashMap<String, String>            purchaseMap = new ConcurrentHashMap();
    private static ConcurrentHashMap<String, ConcurrentHashMap> ticketMap   = new ConcurrentHashMap();
    private static ConcurrentHashMap<String, ConcurrentHashMap> cancelMap   = new ConcurrentHashMap();

    public static void main(String[] args) throws IOException {

        ServerSocket serverSocket = new ServerSocket(9080);
        Socket socket = null;
        init();
        System.out.println("Waiting Connection...");

        ExecutorService executorService = Executors.newFixedThreadPool(MAX_THREAD_COUNT);

        try {
            while (true) {

                try {
                    socket = serverSocket.accept();
                    ServerThread serverThread = new ServerThread(socket);
                    executorService.execute(serverThread);
//                    serverThread.start();

                } catch (IOException e) {}
            }
        } finally {
            socket.close();
            serverSocket.close();
        }

    }

    public static synchronized boolean issueTicket(String email, String prvKey) {

        int cnt = countMap.get(prvKey);
        int maxCnt = maxCountMap.get(prvKey);

        // already have a key.
//        if(ticketMap.get(prvKey).containsKey(email)) {
//            return true;
//        }

        // current count less than max count then you can buy a bag.
        if(cnt < maxCnt) {

            if(!ticketMap.get(prvKey).containsKey(email)) {
                insertMap(email, prvKey, 1);

                System.out.println("TicketMap: "+ticketMap.get(prvKey).entrySet());

                return true;
            }
        }

        // if there is a canceled order.
        if(cancelMap.get(prvKey).isEmpty()) {
            return false;
        } else {
            String canceledKey;
            int canceledTicket;
            // getting a canceledKey
            try {
                canceledKey = cancelMap.get(prvKey).keys().nextElement().toString();
                canceledTicket = NumberUtils.toInt(cancelMap.get(prvKey).get(canceledKey).toString());
            } catch (Exception e) {
                return false;
            }

            // updateMap
            return updateMap(email, prvKey, canceledKey, canceledTicket);
        }
    }

    private static synchronized void insertMap(String email, String prvKey, int amount) {

        ticketMap.get(prvKey).put(email, countMap.get(prvKey) + amount);
        countMap.put(prvKey, countMap.get(prvKey) + amount);
    }

    private static boolean updateMap(String email, String prvKey, String canceledEmail, int ticketNum) {

        if(ticketMap.get(prvKey).containsKey(canceledEmail)) {

            ticketMap.get(prvKey).remove(canceledEmail);
            ticketMap.get(prvKey).put(email, ticketNum);
            cancelMap.get(prvKey).remove(canceledEmail);

            System.out.println("ticketMap: "+ticketMap.get(prvKey).entrySet());
            System.out.println("cancelMap: "+cancelMap.get(prvKey).entrySet());

            return true;
        }

        return false;
    }

    public static void cancel(String email, String prvKey) {

        try {
            cancelMap.get(prvKey).put(email, ticketMap.get(prvKey).get(email));

            System.out.println("CancelMap: "+cancelMap.get(prvKey).entrySet());

        } catch (Exception e) {
            System.out.println("cancelMap Exception: " + e.getMessage());
        }
    }

    public static String init() {

        try {
            // Read file.
//            File file = new File("classes/init.json");
            File file = new File("/Users/yoojun.jeong/Documents/git/productcount/src/main/resources/init.json");
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            String line = null;
            String jsonStr = "";
            while ((line = bufferedReader.readLine()) != null) {
                jsonStr += line;
            }
            bufferedReader.close();

            // Convert String to Json.
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject)jsonParser.parse(jsonStr);
            JSONArray jsonArray = (JSONArray)jsonObject.get("products");

            // Make maps.
            JSONObject json;
            for(int i=0; i<jsonArray.size(); i++) {
                json = (JSONObject)jsonArray.get(i);
                countMap.put(json.get("productId")+"_"+json.get("optionId"), NumberUtils.toInt(json.get("initCount").toString()));
                maxCountMap.put(json.get("productId")+"_"+json.get("optionId"), NumberUtils.toInt(json.get("maxCount").toString()));
                ticketMap.put(json.get("productId")+"_"+json.get("optionId"), new ConcurrentHashMap());
                cancelMap.put(json.get("productId")+"_"+json.get("optionId"), new ConcurrentHashMap());
            }

        } catch (Exception e) {
                System.out.print(e.getMessage());
                return "Fail to initiation.";
        }

        return "Initiation completed.";
    }

    public static int checkSoldout(String email, String prvKey) {

        Collection<ConcurrentHashMap> keys = ticketMap.values();
        int sellingCnt = ticketMap.get(prvKey).size() - cancelMap.get(prvKey).size();
        int remaindCnt = maxCountMap.get(prvKey).intValue() - sellingCnt;

        System.out.println("MaxCount: "+maxCountMap.get(prvKey).intValue());
        System.out.println("TicketMap Status: "+ticketMap.get(prvKey).entrySet());

        return remaindCnt;
    }

    public static void purchase(String email, String prvKey, int amount) {

        purchaseMap.put(email, prvKey);

        if(amount > 1) insertMap(email, prvKey, amount-1);
    }

}

