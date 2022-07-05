import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MulticastSender {
    private DatagramSocket socket;
    private InetAddress group;
    private byte[] buf;
    private int port;
    private String address;

    public MulticastSender(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public void multicast(String multicastMessage) throws IOException {
        socket = new DatagramSocket();
        group = InetAddress.getByName(address);
        buf = multicastMessage.getBytes();

        DatagramPacket packet
                = new DatagramPacket(buf, buf.length, group, port);
        socket.send(packet);
        socket.close();
    }
}
