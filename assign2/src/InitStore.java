import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;

class InitStore implements Runnable {
    private static String id;
    private static Map<String, Integer> clusterMembers = new HashMap<String, Integer>();
    private static Integer counter;
    private static String logPath;
    private static String directoryPath;
    private static int port;
    private static int messageRec = 0;

    InitStore(int port ,String iD, Map<String, Integer> clMem, Integer c ){
        id = iD;
        clusterMembers = clMem;
        counter = c;
        this.logPath = "../nodes/" + id + "/log.txt";
        this.directoryPath = "../nodes/" + id;
        this.port = port;
    }

    @Override
    public void run() {
        try {
            getInitTCPMessages();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getInitTCPMessages() throws Exception {


        Selector selector = Selector.open();
        ServerSocketChannel serverSocketChannel =
                ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress("127.0.0.9", port));
        serverSocketChannel.register(selector, SelectionKey.
                OP_ACCEPT);
        SelectionKey key = null;


        while (messageRec < 1) {
            if (selector.select() <= 0)
                continue;
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();
            while (iterator.hasNext()) {
                key = (SelectionKey) iterator.next();
                iterator.remove();
                if (key.isAcceptable()) {
                    SocketChannel sc = serverSocketChannel.accept();
                    sc.configureBlocking(false);
                    sc.register(selector, SelectionKey.
                            OP_READ);
                    System.out.println("Connection Accepted: "
                            + sc.getLocalAddress() + "n");
                }
                if (key.isReadable()) {
                    SocketChannel sc = (SocketChannel) key.channel();
                    ByteBuffer bb = ByteBuffer.allocate(1024);
                    sc.read(bb);
                    String result = new String(bb.array()).trim();
                    System.out.println("Message received: "
                            + result);
                    String[] m = result.split(";", 4);
                    //System.out.println("M3: " + m[3]);
                    logMessages(m[3]);
                    messageRec++;
                    if (result.length() <= 0) {
                        sc.close();

                    }
                }
            }
        }
    }




    //server;dd3d38a15f9c460520a36a80d33449bd8dfe392ff5778cfa3a5667b9aa14643f;0;
    //5a3d999db083d8eb3cb23198d27b6155c782a1696641c4ae9c25d6058559c109;8012;127.0.0.2;0;
    //5a3d999db083d8eb3cb23198d27b6155c782a1696641c4ae9c25d6058559c109;8012;127.0.0.2;0;
    //

    private static void logMessages(String message) throws Exception {

        String path = logPath;
        String[] logMessages = arrangeFileContent(message);

        ArrayList<String> newLog = compareLogs(logMessages);

        writeToFile_Log(path, newLog);
    }

    private static String[] arrangeFileContent(String content){
        return content.split("---", -1);
    }

    //server;dd3d38a15f9c460520a36a80d33449bd8dfe392ff5778cfa3a5667b9aa14643f;0;
    //5a3d999db083d8eb3cb23198d27b6155c782a1696641c4ae9c25d6058559c109;8012;127.0.0.2;0;---
    //5a3d999db083d8eb3cb23198d27b6155c782a1696641c4ae9c25d6058559c109;8012;127.0.0.2;0;---
    //5a3d999db083d8eb3cb23198d27b6155c782a1696641c4ae9c25d6058559c109;8012;127.0.0.2;0;---
    private static ArrayList<String> compareLogs(String[] conteudo){
        ArrayList<String> newLogs = new ArrayList<>();


        for(int i = 0; i < conteudo.length; i++){
            //System.out.println("CONT: " + conteudo[i]);
            if(conteudo[i].equals("")) break;
            String[] line = conteudo[i].split(";", -1);

            String key = line[0] + ";" + line[1] + ";" + line[2];

            if(clusterMembers.containsKey(key)){
                String lineLog = "";
                if(clusterMembers.get(key) < Integer.parseInt(line[3])){
                    lineLog = key + ";" + Integer.parseInt(line[3]);

                    newLogs.add(lineLog);
                    clusterMembers.put(key, Integer.parseInt(line[3]));
                }else if(clusterMembers.get(key) == Integer.parseInt(line[3])) {
                    lineLog = key + ";" + clusterMembers.get(key);
                    newLogs.add(lineLog);
                }
                //System.out.println("LINELOG: " + lineLog);
            }else{

                String lineLog = key + ";" + line[3] ;
                //System.out.println("KEY: " + key);

                newLogs.add(lineLog);
                clusterMembers.put(key, Integer.parseInt(line[3]));
            }
        }

        return newLogs;

    }

    private static void writeToFile_Log(String path, ArrayList<String> content) throws IOException {
        File fout = new File(path);
        FileOutputStream fos = new FileOutputStream(fout);

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
        String c = counter.toString();
        bw.write(c);
        bw.newLine();

        for (int i = 0; i < content.size() ; i++) {
            bw.write(content.get(i));
            bw.newLine();
        }

        bw.close();
    }
}