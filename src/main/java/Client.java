import java.io.*;
import java.net.Socket;

/**
 * Created by yoojun.jeong on 2017. 3. 21..
 */
public class Client {

    public static void main(String[] args) throws IOException {


        try {
            // insert
            while(true) {
//                Socket socket = new Socket("localhost", 9080);
                Socket socket = new Socket("13.124.91.203", 9080);
//                Socket socket = new Socket("13.124.107.107", 9080);

                BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
                String line = keyboard.readLine();

                if("exit".equals(line)) {
                    socket.close();
                    break;
                }

                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();

                PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(out), true);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));

                // send to server
                printWriter.println(line);

                String echo = bufferedReader.readLine();
                System.out.println("Message: " + echo);

                bufferedReader.close();
                printWriter.close();

                socket.close();
            }

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }
}
