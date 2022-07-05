import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MulticastReceiver extends Thread {
    private int port;
    private String address;
    private MulticastMessage message;
    protected MulticastSocket socket = null;
    protected byte[] buf = new byte[256];
    private TCPReceiver tcpReceiver;
    private String ipAddress;

    public MulticastReceiver(String address, int port, TCPReceiver tcpReceiver, String ipAddress) {
        this.address = address;
        this.port = port;
        this.tcpReceiver = tcpReceiver;
        this.ipAddress = ipAddress;
    }

    public void run() {

        InetAddress group;

        try {
            socket = new MulticastSocket(port);
            group = InetAddress.getByName(address);
            socket.joinGroup(group);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        while (true) {

            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            try {
                socket.receive(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            //System.out.println("UDP ESTOU AQUI");
            String received = new String(packet.getData(), 0, packet.getLength());
            //System.out.println(new String(packet.getData()));
            //System.out.println(received);
            if (received != null) {
                this.message = new MulticastMessage(received);
                //System.out.println("UDP ESTOU AQUI2");
                //System.out.println(message.getStoreKey() + " " + tcpReceiver.getHId());
                if (!message.getStoreKey().equals(tcpReceiver.getHId()))
                {
                    if (message.getAntiFail()) {
                        tcpReceiver.addToClusterMembership(message);
                        try {
                            tcpReceiver.handleLogCycleMessages(message);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        System.out.println("Received anti failure control message");
                    } else {
                        //System.out.println("UDP ESTOU AQUI3");
                        switch (message.getOperation()) {
                            case 0:
                                System.out.println("JOIN OPERATION");
                                tcpReceiver.udp_membership_message(message);
                                try {
                                    //System.out.println(message.getTcpPort());
                                    tcpReceiver.sendLogMessages(message.getIpAddress(), message.getTcpPort());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                break;
                            default:
                                System.out.println("LEAVE OPERATION");
                                tcpReceiver.udp_membership_message(message);
                                break;
                        }
                    }
                }
                this.message = null;
            }

            if ("end".equals(received)) {
                break;
            }
        }

        try {
            socket.leaveGroup(group);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        socket.close();
    }


    public String receive() {

        InetAddress group;

        try {
            socket = new MulticastSocket(port);
            group = InetAddress.getByName(address);
            socket.joinGroup(group);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        while (true) {

            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            try {
                socket.receive(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            String received = new String(packet.getData(), 0, packet.getLength());

            if (received != null) {
                try {
                    socket.leaveGroup(group);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                socket.close();
                return received;
            }
        }
    }

    public MulticastMessage getMessage() {
        return message;
    }

    public void setMessage(MulticastMessage message) {
        this.message = message;
    }
}