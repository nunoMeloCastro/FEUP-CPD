import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;


public class TCPReceiver extends Thread {

    private static String id; //hashid
    private static Socket socket = null;
    private static String IPaddres;
    private static Map<String, Integer> clusterMembers = new HashMap<String, Integer>();
    //map -> clusterMembers = [f3436f50b2f7f1613ad142dbce1d24801d9daaabc45ecb2db909251a214c9842;8012;127.0.0.1 , 70]
    private static Integer counter;
    private String receivedMessageTCP;
    private static String multicastAddr;
    private static int multicastPort;
    private static int servicePort;
    private static boolean isConnected = false;
    private static String logPath;
    private static String directoryPath;
    private static InetSocketAddress SS;
    private static ServerSocketChannel serverSocketChannel;
    private static Selector selector;
    private static InetAddress host;
    private static SelectionKey key;
    private static SocketChannel sc;
    private static String messageToBeSent;


    //new TCPReceiver(port, IPaddress, hashID, clients, clusterMembers, membershipCounter, multicastAddr, multicastPort)
    public TCPReceiver(int port, String IPaddress, String id, String multicastAddr, Integer multicastPort) throws IOException {
        logPath = "../nodes/" + id + "/log.txt";
        directoryPath = "../nodes/" + id;
        this.counter = -1;
        this.servicePort = port;
        this.id = id;
        this.IPaddres = IPaddress;
        this.multicastAddr = multicastAddr;
        this.multicastPort = multicastPort;


    }


    public void run() {

        try {
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            serverSocketChannel = ServerSocketChannel.open();
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            serverSocketChannel.configureBlocking(false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //System.out.println("IP ADDRESS : " + IPaddres);
        try {
            serverSocketChannel.bind(new InetSocketAddress(IPaddres, servicePort));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            serverSocketChannel.register(selector, SelectionKey.
                    OP_ACCEPT);
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        }
        while (true) {
            try {
                if (selector.select() <= 0)
                    continue;
            } catch (IOException e) {
                e.printStackTrace();
            }
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();
            while (iterator.hasNext()) {
                key = (SelectionKey) iterator.next();
                iterator.remove();
                if (key.isAcceptable()) {

                    try {
                        sc = serverSocketChannel.accept();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        sc.configureBlocking(false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {

                        sc.register(selector, SelectionKey.OP_READ);
                    } catch (ClosedChannelException e) {
                        e.printStackTrace();
                    }
                    try {
                        System.out.println("Connection Accepted: "
                                + sc.getLocalAddress());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (key.isReadable()) {

                    SocketChannel sc = (SocketChannel) key.channel();

                    try {
                        sc.register(selector, SelectionKey.OP_WRITE);
                    } catch (ClosedChannelException e) {
                        e.printStackTrace();
                    }

                    ByteBuffer bb = ByteBuffer.allocate(1024);
                    try {
                        sc.read(bb);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    String result = new String(bb.array()).trim();

                    System.out.println("RESULT: " + result);
                    try {
                        if (!(result.length() <= 0)) {
                            System.out.println("RES: " + result);
                            takeCareOfMessages(result);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
                if (key.isWritable()) {
                    //System.out.println("Mensagem a enviar: " + messageToBeSent);
                    SocketChannel sc = (SocketChannel) key.channel();

                    if (!Objects.equals(messageToBeSent, null)) {
                        ByteBuffer bb = ByteBuffer.wrap(messageToBeSent.getBytes());
                        try {
                            //System.out.println("Mensagem a enviar2: " + messageToBeSent);

                            sc.write(bb);
                            messageToBeSent = "";
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        sc.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }


            }
        }
    }


    public String getReceivedMessageTCP() {
        return this.receivedMessageTCP;
    }

    public static void initThread() throws IOException {

        ExecutorService executorService = Executors.newSingleThreadExecutor();;
        executorService.execute(new InitStore(SS.getPort(), id, clusterMembers, counter));

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10000, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {

        }

        System.out.println("Able to get clients");
        return;
    }

    public static void sendLogMessages(String IPaddress, int port) throws Exception {
        String path = logPath;
        ArrayList<String> content = getMessageLog(path);

        int i = 1;

        if (content.size() > 33) {
            i = content.size() - 33;
        }

        String message = "server;" + id + ";" + counter + ";";
        for (int j = i; j < content.size(); j++) {
            message += content.get(j) + "---";
        }
        //System.out.println("LOG1 : " + message);
        //System.out.println("IP: "+ IPaddress);
        //System.out.println("PORT: " + port);

        InetSocketAddress addr = new InetSocketAddress("127.0.0.9", port);
        Selector selector = Selector.open();
        SocketChannel sc = SocketChannel.open();
        sc.configureBlocking(false);
        sc.connect(addr);
        sc.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE);

        while (true) {
            if (selector.select() > 0) {
                Boolean doneStatus = processReadySet
                        (selector.selectedKeys(), message);
                if (doneStatus) {
                    break;
                }
            }
        }
        sc.close();
    }





    private static void takeCareOfMessages(String message) throws Exception {
        String[] str = message.split(";", -1);

        if(str[0].equals("cliente")){
            //System.out.println("ENTREI no client tasks");
            clientMessages(message);
        }else if(str[0].equals("sucessor")){
            //System.out.println("ENTREI no succesor tasks");

            //sendKeys(message);
        }else if(str[0].equals("retsucessor")){
            //System.out.println("ENTREI no retsuccesor tasks");

            receiveKeysFromStore(message);
        }else if(str[0].equals("store")){
            //System.out.println("ENTREI no store tasks");

            update_log_messages(message);
        }
    }

    private static void sendKeys(String message) throws Exception {

        //sucessor;id;IPaddress;port
        var fields = message.split(";");

        Map<String,String> filteredKeys = new HashMap<>();
        BigInteger hashId = new BigInteger(fields[1],16);

        try {
            for (Map.Entry<String,String> pair: get_key_values().entrySet()){

                BigInteger bgKey = new BigInteger(pair.getKey(),16);

                BigInteger diff = hashId.subtract(bgKey);

                if(diff.compareTo(new BigInteger("0")) == 1){
                    filteredKeys.put(pair.getKey(),pair.getValue());
                }
                else{
                    continue;
                }


            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        sendKeysByTCP(fields[2],fields[3],filteredKeys);

    }

    private static void sendKeysByTCP(String ip,String port, Map<String, String> filteredKeys) throws Exception {

        var str = new StringBuilder();

        str.append("retsucessor;");

        for (Map.Entry<String, String> pair : filteredKeys.entrySet()) {
            str.append(pair.getKey()).append(";");
            str.append(pair.getValue()).append(";");
        }
        String message = str.toString();
        InetSocketAddress addr1 = new InetSocketAddress(InetAddress.getByName(String.valueOf(ip)), Integer.parseInt(port));
        Selector selector = Selector.open();
        SocketChannel sc1 = SocketChannel.open();
        sc1.configureBlocking(false);
        sc1.connect(addr1);
        sc1.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE);

        while (true) {

            if (selector.select() > 0) {
                Boolean doneStatus = processReadySet(selector.selectedKeys(), message);
                if (doneStatus) {
                    break;
                }
            }
            sc.close();

        }
    }

    private static void receiveKeysFromStore(String message){
        String[] str = message.split(";", 3);



        String[] key_values = str[2].split(";", -1);
        for (int i = 0; i < key_values.length; i += 2){

            String path = directoryPath + key_values[i] + ".txt";
            createFile(path, str[i+1]);

        }



    }

    private static void update_log_messages(String message) throws Exception {
        String[] str = message.split(";", 3);

        String path = logPath;

        String[] logMessages = arrangeFileContent(str[2]);

        ArrayList<String> newLog = compareLogs(logMessages);

        writeToFile_Log(path, newLog);
    }

    private static void clientMessages(String message) throws Exception {
        String[] str = message.split(";", -1);

        String host = str[2];
        switch (str[1]) {
            case "join":
                if(!isConnected)
                    joinOp(str[2]);
                break;
            case "leave":
                if (isConnected)
                    leaveOp();
                break;
            case "put":
                if (isConnected) {
                    String[] str1 = message.split(";", 4);
                    putOp(str1[2], str1[3]);
                }
                break;
            case "delete":
                if (isConnected)
                    deleteOp(str[2]);
                break;
            case "get":
                if (isConnected)
                    getOp(str[2]);
                break;
            default:
                System.out.println("Not a client");;
        }
        System.out.println("IS CONNECTED: " + isConnected);

    }

    //join event da store
    //todo: pedir ao sucessor pelas keys-values
    private static void joinOp(String s) throws IOException, InterruptedException {
        createDirectory();
        createFile(logPath);
        initializeLogVar(logPath);
        get_key_values();

        Random rand = new Random();
        int portRandom = rand.nextInt(13000-11000) + 11000;
        SS = new InetSocketAddress("127.0.0.9", portRandom);
        System.out.println("Join Operation : " + ++counter);
        KeyValueTransfer kvt = new KeyValueTransfer();
        Map<String,Integer> filteredClusterOnlineMembers = filterOnlineNodes();
        HashFunction hash = new HashFunction(s, servicePort ,true);
        kvt.joinEvent(id,servicePort,IPaddres, filteredClusterOnlineMembers);

        MulticastSender sender = new MulticastSender(multicastAddr, multicastPort);
        MulticastMessage initMessageUDP = new MulticastMessage(counter, id, servicePort , false, IPaddres, portRandom);
        //System.out.println("EVIDADO AQUI: " + initMessageUDP.toString());
        sender.multicast(initMessageUDP.toString());


        initThread();


        isConnected = true;

        updateServerLogInfo_fromFile();
        messageToBeSent = "Join Operation Successful";
        //counter++;

    }

    //operação leave da Store
    //todo: mandar ao sucessor as keys
    private static void leaveOp() throws IOException {
        System.out.println("Leave Operation");
        KeyValueTransfer kvt =  new KeyValueTransfer();

        Map<String, Integer> filteredClusterOnlineMembers = filterOnlineNodes();

        kvt.leaveEvent(id,filteredClusterOnlineMembers,get_key_values());

        MulticastSender sender = new MulticastSender(multicastAddr, multicastPort);
        MulticastMessage initMessageUDP = new MulticastMessage(++counter, id, getServicePort(), false, IPaddres);
        sender.multicast(initMessageUDP.toString());

        leave_write_log();
        isConnected = false;
        //counter++;
    }

    //função que trata da operação put do cliente
    private static void putOp(String key, String value) {

        String path = directoryPath + "/" + key + ".txt";

        createFile(path, value);

        messageToBeSent = key;
    }

    //função que trata da operação delete do cliente
    private static void deleteOp(String key){
        String path = directoryPath + "/" + key + ".txt";
        File myObj = new File(path);
        if (myObj.delete()) {
            messageToBeSent = "Delete Operation Successful";

        } else {
            messageToBeSent = "Delete Operation not Successful";

        }
    }

    //função que trata da operação get do cliente
    private static void getOp(String key) throws IOException {

        String path = directoryPath + "/" + key + ".txt";
        String message = getFileMessage(path);

        messageToBeSent = message;

    }



    //cria um ficheiro com o content indicado
    private static void createFile(String path, String content){
        try {
            File myObj = new File(path);
            if (myObj.createNewFile()) {
                writeToFile(path,content);
            } else {
                System.out.println("already created");
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    private static void createFile(String path){
        String content = "0";
        try {
            File myObj = new File(path);
            if (myObj.createNewFile()) {
                writeToFile_init(path, content);
            } else {
                System.out.println("already created");
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }


    private static void writeToFile_init(String path, String content) throws IOException {
        File fout = new File(path);
        FileOutputStream fos = new FileOutputStream(fout);

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
        bw.write(content);
        bw.newLine();



        bw.close();
    }

    //ao receber uma mensagem TCP, escre no log essa mensagem
    private static void writeToFile(String path, String content) throws IOException {
        File fout = new File(path);
        FileOutputStream fos = new FileOutputStream(fout);

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
        String[] fix = arrangeFileContent(content);
        for (int i = 0; i < fix.length - 1 ; i++) {
            bw.write(fix[i]);
            bw.newLine();
        }

        bw.close();
    }

    //atualiza no log com as mensagens recebidas periodicamente
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


    //como as novas linhas sao representadas como "---" nas mensagens TCP
    //esta funcao separa-as numa String[]
    private static String[] arrangeFileContent(String content){
        return content.split("---", -1);
    }


    //vai buscar o value de uma key e poe numa string cada linha separada com "---"
    private static String getFileMessage(String path) throws FileNotFoundException {

        try{
            File file = new File(path);
            String str = "";
            Scanner myReader = new Scanner(file);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                str += data + "---";
            }
            myReader.close();

            return str;
        } catch (FileNotFoundException e) {
        }
        return "No key value";
    }


    private static void updateServerLogInfo_fromFile() throws IOException {
        String content = getFileMessage(logPath);
        if(!content.equals("")) {
            String[] aux = content.split("---", -1);
            String[]  all = content.split("---", 2);

            //System.out.println("SIZE: " + aux[1]);
            if(!aux[1].equals("")) {
                String[] logContent = arrangeFileContent(aux[1]);

                compareLogs(logContent);
            }
        }else
            System.out.println(" ");
    }


    //função para comparar os logs recebidos inicialmente e tambem periodicamente
    private static ArrayList<String> compareLogs(String[] conteudo){
        ArrayList<String> newLogs = new ArrayList<>();

        for(int i = 0; i < conteudo.length; i++){
            //System.out.println("TATATA: " + conteudo[i]);
            String[] line = conteudo[i].split(";", -1);

            String key = line[0] + ";" + line[1] + ";" + line[2];

            if(clusterMembers.containsKey(key)){
                if(clusterMembers.get(key) < Integer.parseInt(line[3])){
                    String lineLog = key + ";" + Integer.parseInt(line[3]);
                    newLogs.add(lineLog);
                    clusterMembers.put(key, Integer.parseInt(line[3]));
                }else if(clusterMembers.get(key) == Integer.parseInt(line[3])) {
                    String lineLog = key + ";" + clusterMembers.get(key);
                    newLogs.add(lineLog);
                }
            }else{

                String lineLog = key + ";" + line[3];
                //System.out.println("KEY: " + key);
                newLogs.add(lineLog);
                clusterMembers.put(key, Integer.parseInt(line[3]));
            }
        }

        return newLogs;

    }

    //chamada quando a store sai do cluster
    //atualiza no ficheiro log todas as informações
    private static void leave_write_log() throws IOException {
        String path = logPath;
        String[] all = arrangeFileContent(getFileMessage(path));

        ArrayList<String> aux = new ArrayList<>();
        for(String a : all){
            aux.add(a);
        }
        writeToFile_Log(path, aux);

    }

    // vai buscar os logs e mete num array
    private static ArrayList<String> getMessageLog(String path){
        ArrayList<String> ret = new ArrayList<>();
        try{
            File file = new File(path);
            Scanner myReader = new Scanner(file);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                ret.add(data);
            }
            myReader.close();

            return ret;
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    public String getMulticastAddr(){
        return this.multicastAddr;
    }

    public int getMulticastPort(){
        return this.multicastPort;
    }

    public void setMulticastAddr(String multicastAddr){
        this.multicastAddr = multicastAddr;
    }

    public void setMulticastPort(int multicastPort){
        this.multicastPort = multicastPort;
    }

    private static int getServicePort() {
        return servicePort;
    }

    public static Map<String, Integer> getClusterMembers() {
        return clusterMembers;
    }

    public static Map<String, Integer> filterOnlineNodes(){

        Map<String, Integer> newMap = new HashMap<>();

        for(Map.Entry<String,Integer> pair: clusterMembers.entrySet()){

            var memCount = clusterMembers.get(pair.getKey());

            if(memCount % 2 == 0) { //Online
                newMap.put(pair.getKey(), pair.getValue());
            }
        }

        return newMap;
    }



    //retorna o mapa de key_values
    public static Map<String, String> get_key_values() throws FileNotFoundException {
        Map<String, String> res = new HashMap<>();
        String path = directoryPath;
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        ArrayList<String> filenames = new ArrayList<>();
        ArrayList<String> values = new ArrayList<>();
        ArrayList<String> keys = new ArrayList<>();
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String[] aux = listOfFiles[i].getName().split("\\.",-1);
                if(!aux[0].equals("log")){
                    filenames.add(listOfFiles[i].getName());
                    keys.add(aux[0]);
                }
            }
        }

        for(String a : filenames){
            String newPath = directoryPath + "/" + a;
            values.add(getFileMessage(newPath));
        }
        for(int i = 0; i < values.size(); i++ )
            res.put(keys.get(i), keys.get(i));

        return res;
    }

    public static void appendStrToFile(String path,
                                       String str) throws IOException {
        // Try block to check for exceptions
        FileWriter fw = new FileWriter(path, true);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(str);
        bw.newLine();
        bw.close();
    }



    public void handleLogCycleMessages(MulticastMessage message) throws IOException {

        String key = message.getStoreKey() + ";" + servicePort + ";" + message.getIpAddress() + ";";
        String path = logPath;
        //System.out.println(key);
        int old_counter = clusterMembers.get(key);

        int new_counter = message.getMemCnt();

        if(new_counter > old_counter){
            clusterMembers.put(key, new_counter);
            String mess = key + ";" + new_counter;
            appendStrToFile(path, mess);
        }else{
            //todo: se calhar mandar uma logmessage para esse (decerto nao e preciso)
        }

    }

    public void udp_membership_message(MulticastMessage message) {
        String path = logPath;
        String mess = message.toLog();

        try {
            appendStrToFile(path, mess);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public boolean getIsConnected(){
        return isConnected;
    }

    public static Boolean processReadySet(Set readySet, String message)
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
        if (key.isWritable()) {

            SocketChannel sc = (SocketChannel) key.channel();
            ByteBuffer bb = ByteBuffer.wrap(message.getBytes());
            sc.write(bb);
        }
        return false;
    }

    public static Boolean processConnect(SelectionKey key) {
        var sc = (SocketChannel) key.channel();
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

    private static void initializeLogVar(String path){
        int i = 0;

        try{
            File file = new File(path);
            Scanner myReader = new Scanner(file);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                if(i == 0){
                    counter = Integer.parseInt(data);
                }
                else{
                    String[] str = data.split(";", -1);
                    String key = str[0] + ";" + str[1] + ";" + str[2];

                    if(clusterMembers.containsKey(key)) {
                        int oldcounter = clusterMembers.get(key);

                        if (oldcounter < Integer.parseInt(str[3])) {
                            clusterMembers.put(key, Integer.parseInt(str[3]));
                        }
                    }else{
                        clusterMembers.put(key, Integer.parseInt(str[3]));
                    }
                }
                i++;
            }
            myReader.close();

        } catch (FileNotFoundException e) {

        }
    }

    private static void createDirectory(){
        String path = directoryPath;
        File f = new File(path);

        // check if the directory can be created
        // using the specified path name
        if (f.mkdir()) {
            System.out.println("Directory has been created successfully") ;
        }
        else {
            System.out.println("Directory cannot be created") ;
        }
    }

    public String getIPaddres() {
        return IPaddres;
    }

    public void addToClusterMembership(MulticastMessage message){
        String key = message.toKey();
        int counter = message.getMemCnt();

        if(clusterMembers.get(key) > counter)
            clusterMembers.put(key, counter);
    }

    public static String getHId() {
        return id;
    }
}