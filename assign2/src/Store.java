import java.io.*;
import java.util.*;

public class Store {
    private static String ID;
    private static String hashID;
    private static String IPaddress;
    private static String multicastAddr;
    private static int multicastPort;
    private static int port;
    private static int membershipCounter;
    private static Map<String, Integer> clusterMembers = new HashMap<String, Integer>();

    public static void main(String[] args) throws Exception {
        if (args.length < 1) return;
        //System.out.println(args[1]);
        HashFunction hashFunction = new HashFunction(args[2], Integer.parseInt(args[3]), true);
        port = Integer.parseInt(args[3]);
        multicastAddr = args[0];
        multicastPort = Integer.parseInt(args[1]);
        hashID = hashFunction.createHashNode();
        IPaddress = args[2];
        String path = "../nodes/"  +  hashID + "/log.txt";


        ID = args[2];


        boolean udpRunning = false;
        boolean tcpRunning = false;
        boolean antiFail = false;
        
        TCPReceiver tcpReceiver = new TCPReceiver(port, IPaddress, hashID, multicastAddr, multicastPort);
        MulticastReceiver receiver = new MulticastReceiver(multicastAddr, multicastPort, tcpReceiver, IPaddress);
        //JA TA TUDO DIREITO NOS LOGS
        System.out.println("Server is listening on port " + port);

        while (true) {

            if (!udpRunning)
            {
                Thread thread = new Thread(receiver);
                thread.start();
                udpRunning = true;
                System.out.println("multicast server started");
            }

            /*if(receiver.getMessage() != null){
                System.out.println(receiver.getMessage());
                MulticastMessage received = receiver.getMessage();
                if(received.getAntiFail() && (!received.getIpAddress().equals(IPaddress))){
                    tcpReceiver.handleLogCycleMessages(received);
                    System.out.println("Received anti failure control message");
                }else {
                    switch (received.getOperation()) {
                        case 0:
                            System.out.println("JOIN OPERATION");
                            tcpReceiver.udp_membership_message(received);
                            break;
                        default:
                            System.out.println("LEAVE OPERATION");
                            tcpReceiver.udp_membership_message(received);
                            break;
                    }
                }
            }*/


            if (!tcpRunning) {
                Thread thread1 = new Thread(tcpReceiver);
                thread1.start();
                tcpRunning = true;
                System.out.println("TCP server started");
            }

            if (!antiFail && tcpReceiver.getIsConnected()) {
                System.out.println("Anti failure control started!");
                TimerTask timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        MulticastSender sender = new MulticastSender(multicastAddr, multicastPort);
                        MulticastMessage antiFailMsg = new MulticastMessage(membershipCounter, hashID, port, true, IPaddress);
                        try {
                            sender.multicast(antiFailMsg.toString());
                            //System.out.println(antiFailMsg.toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                };
                Timer timer = new Timer(true);
                timer.scheduleAtFixedRate(timerTask, 0, 1000);
                antiFail = true;
            }

        }

    }



    private static void writeToFile(String path, String content) throws IOException {
        File fout = new File(path);
        FileOutputStream fos = new FileOutputStream(fout);

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
        String[] fix = arrangeFileContent(content);
        for (int i = 0; i < fix.length; i++) {
            bw.write(fix[i]);
            bw.newLine();
        }

        bw.close();
    }

    private static String[] arrangeFileContent(String content){
        return content.split("---", -1);
    }

}