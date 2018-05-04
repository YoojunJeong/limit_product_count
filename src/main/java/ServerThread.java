import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.Socket;

/**
 * Created by yoojun.jeong on 2017. 3. 21..
 */
public class ServerThread extends Thread {

    private Socket socket;
    private BufferedReader bufferedReader;
    private PrintWriter printWriter;

    ServerThread(Socket socket) {
        this.socket = socket;
    }

    public void run() {

        try {
            service();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                closeAll();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void service() throws IOException, ParseException {

        OutputStream outputStream = socket.getOutputStream();
        InputStream inputStream = socket.getInputStream();

        printWriter = new PrintWriter(new OutputStreamWriter(outputStream), true);
        bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject)jsonParser.parse(bufferedReader.readLine());

        String event     = (String) jsonObject.get("event");
        String email     = (String) jsonObject.get("email");
        String productId = (String) jsonObject.get("productId");
        String optionId  = (String) jsonObject.get("optionId");
        String prvKey    = makeKey(productId, optionId);



        if("init".equalsIgnoreCase(event)) {
            System.out.println("Received String = " + jsonObject.toJSONString());
            printWriter.println(EventTicket.init());
        } else {
            if("cancel".equalsIgnoreCase(event)) {
                System.out.println("Received String = " + jsonObject.toJSONString());
                EventTicket.cancel(email, prvKey);
                printWriter.println(email+" is canceled from ["+prvKey+"]");
            } else if("checkSoldout".equalsIgnoreCase(event)) {
                printWriter.println(EventTicket.checkSoldout(email, prvKey));
            } else {
                System.out.println("Received String = " + jsonObject.toJSONString());
                printWriter.println(EventTicket.issueTicket(email, prvKey));
            }
        }

    }

    private static String makeKey(String productId, String optionId) {
        return productId+"_"+optionId;
    }

    public void closeAll() throws IOException {

        if(printWriter != null)     printWriter.close();
        if(bufferedReader != null)  bufferedReader.close();
        if(socket != null)          socket.close();
    }
}
