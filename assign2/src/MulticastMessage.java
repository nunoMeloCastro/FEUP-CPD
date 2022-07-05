public class MulticastMessage {

    private int memCnt;
    private String storeKey;
    private int port;
    private boolean antiFail;
    private String ipAddress;
    private int tcpPort;

    public MulticastMessage(String msg) {
        String[] aux = msg.split(";");
        setMemCnt(Integer.parseInt(aux[0]));
        setStoreKey(aux[1]);
        setPort(Integer.parseInt(aux[2]));
        setAntiFail(Boolean.parseBoolean(aux[3]));
        setIpAddress(aux[4]);
        setTcpPort(Integer.parseInt(aux[5]));
    }

    public MulticastMessage(int memCnt, String storeKey, int port, boolean antiFail, String ipAddress) {
        this.memCnt = memCnt;
        this.storeKey = storeKey;
        this.port = port;
        this.antiFail = antiFail;
        this.ipAddress = ipAddress;
    }

    public MulticastMessage(int memCnt, String storeKey, int port, boolean antiFail, String ipAddress, int tcpPort) {
        this.memCnt = memCnt;
        this.storeKey = storeKey;
        this.port = port;
        this.antiFail = antiFail;
        this.ipAddress = ipAddress;
        this.tcpPort = tcpPort;
    }

    @Override
    public String toString() {
        return getMemCnt() + ";" + getStoreKey() + ";" + getPort() + ";" + getAntiFail() + ";" + getIpAddress() + ";" + getTcpPort();
    }

    public String toKey() {
        return getStoreKey() + ";" + getPort() + ";" + getIpAddress() + ";";
    }

    public String toLog() {
        return getStoreKey() + ";" + getPort() + ";" + getIpAddress() + ";" + getMemCnt() + ";";
    }

    public int getMemCnt() {
        return memCnt;
    }

    public void setMemCnt(int memCnt) {
        this.memCnt = memCnt;
    }

    public String getStoreKey() {
        return storeKey;
    }

    public void setStoreKey(String storeKey) {
        this.storeKey = storeKey;
    }

    public int getPort() {
        return port;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public void setTcpPort(int port) {
        this.tcpPort = port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getOperation() {
        return (memCnt % 2);
    }

    public boolean getAntiFail() {
        return antiFail;
    }

    public void setAntiFail(boolean antiFail) {
        this.antiFail = antiFail;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}