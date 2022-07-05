import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

public class TestClient {
    private static BufferedReader input = null;
    private static String hashedId;
    public static void main(String[] args) throws Exception {

        String[] str = args[0].split(":", -1);

        HashFunction hashFunction = new HashFunction(str[0], Integer.parseInt(str[1]), true);
        hashedId = hashFunction.createHashNode();

        InetSocketAddress addr = new InetSocketAddress(
                str[0], Integer.parseInt(str[1]));
        Selector selector = Selector.open();
        SocketChannel sc = SocketChannel.open();
        sc.configureBlocking(false);
        sc.connect(addr);
        sc.register(selector, SelectionKey.OP_CONNECT |
                SelectionKey.OP_READ | SelectionKey.
                OP_WRITE);
        input = new BufferedReader(new
                InputStreamReader(System.in));
        while (true) {

            if (selector.select() > 0) {
                Boolean doneStatus;
                if(args.length > 2) {
                    doneStatus = processReadySet
                            (selector.selectedKeys(), args[1], args[2]);
                }else {
                    doneStatus = processReadySet(selector.selectedKeys(), args[1]);
                }
                if (doneStatus) {
                    break;
                }
            }
        }
        sc.close();
    }


    private static String putOp(String op, String filename) throws FileNotFoundException, NoSuchAlgorithmException {

        HashFunction hashFunction = new HashFunction(filename);

        String hashedKey = hashFunction.createHashNode();

        return "cliente;" + op + ";" + hashedKey + ";" + getFileMessage(filename);

    }


    private static String del_messages(String op, String hashedKey) throws NoSuchAlgorithmException {

        return "cliente;" + op + ";" + hashedKey;


    }

    private static String get_messages(String op, String hashedKey) throws NoSuchAlgorithmException {
        return "cliente;" + op + ";" + hashedKey;
    }

    private static String getFileMessage(String filename) throws FileNotFoundException {
        File file = new File(filename);
        String str = "";
        Scanner myReader = new Scanner(file);
        while (myReader.hasNextLine()) {
            String data = myReader.nextLine();
            str += data + "---";
        }
        myReader.close();

        return str;
    }

    private static String getFileName(String filename){
        String[] str = filename.split("/");
        return str[str.length - 1];
    }

    private static String[] arrangeFileContent(String content){
        return content.split("---", -1);
    }

    public static Boolean processReadySet(Set readySet, String op)
            throws Exception {
        SelectionKey key = null;
        Iterator iterator = null;
        iterator = readySet.iterator();
        while (iterator.hasNext()) {
            key = (SelectionKey) iterator.next();
            iterator.remove();
        }
        if (key.isConnectable()) {
            Boolean connected = processConnect(key);
            if (!connected) {
                return true;
            }
        }
        if (key.isReadable()) {
            System.out.println("READABLE");
            SocketChannel sc = (SocketChannel) key.channel();
            ByteBuffer bb = ByteBuffer.allocate(1024);
            sc.read(bb);
            String result = new String(bb.array()).trim();
            //System.out.println("Message received from Server: " + result + " Message length= "
                   // + result.length());
            return true;
        }
        if (key.isWritable()) {
            String msg = "cliente;" + op + ";" + hashedId;;
            SocketChannel sc = (SocketChannel) key.channel();
            ByteBuffer bb = ByteBuffer.wrap(msg.getBytes());
            sc.write(bb);

        }
        return false;
    }

    public static Boolean processReadySet(Set readySet, String op, String content)
            throws Exception {
        SelectionKey key = null;
        Iterator iterator = null;
        iterator = readySet.iterator();
        while (iterator.hasNext()) {
            key = (SelectionKey) iterator.next();
            iterator.remove();
        }
        if (key.isConnectable()) {
            Boolean connected = processConnect(key);
            if (!connected) {
                return true;
            }
        }
        if (key.isReadable()) {
            SocketChannel sc = (SocketChannel) key.channel();
            ByteBuffer bb = ByteBuffer.allocate(1024);
            sc.read(bb);
            String result = new String(bb.array()).trim();
            //System.out.println("Message received from Server: " + result + " Message length= "
                  //  + result.length());
        }
        if (key.isWritable()) {
            String msg = "";
            switch (op) {
                case "put":
                    msg = putOp(op, content);
                    break;
                case "delete":
                    msg = del_messages(op, content);
                    break;
                case "get":
                    msg = del_messages(op, content);
                    break;
                default:

            }
            SocketChannel sc = (SocketChannel) key.channel();
            ByteBuffer bb = ByteBuffer.wrap(msg.getBytes());
            sc.write(bb);
        }
        return false;
    }

    public static Boolean processConnect(SelectionKey key) {
        SocketChannel sc = (SocketChannel) key.channel();
        try {
            while (sc.isConnectionPending()) {
                sc.finishConnect();
            }
        } catch (IOException e) {
            key.cancel();
            e.printStackTrace();
            return false;
        }
        return true;
    }
}