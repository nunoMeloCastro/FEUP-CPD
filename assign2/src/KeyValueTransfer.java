import java.io.*;
import java.util.*;
import java.math.*;
import java.net.*;

public class KeyValueTransfer {

    /**
     *
     *Join Event:
     *
     * the successor of the joining node should transfer to the latter the keys that are smaller or equal to the id of the joining node;
     *
     */

    /**
     *
     *Leave Event:
     *
     * before leaving the cluster, i.e. multicasting the LEAVE message, the node should transfer its key-value pairs to its successor.
     *
     *
     */


    public KeyValueTransfer(){}

    public void joinEvent(String hashID,Integer port, String ipAdd, Map<String, Integer> clusterMembers){
        //Find the successor of the node with the hashID
        //Send a TCP message requesting the key-Value Pair
        List<String> clusterMembersIDs = getClusterMembersIDs(clusterMembers);

        if(clusterMembersIDs.isEmpty()){
            System.out.println("Empty Cluster Members");
            return;
        }

        var hashIdSuccessor = getSuccessor(hashID,clusterMembersIDs);

        var ipAddressSucc = getIpAddress(hashIdSuccessor,clusterMembers);

        var portSucc = getPortSuccessor(hashIdSuccessor,clusterMembers);

        sendTcpJoinMessage(hashID,ipAddressSucc,portSucc ,ipAdd,port);
    }

    public void leaveEvent(String hashID, Map<String,Integer> clusterMembers ,Map<String,String> keyValuePairs){
        //Find the successor of the node with the hashID
        //send a TCP message with the key-Value Pairs

        List<String> clusterMembersIDs = getClusterMembersIDs(clusterMembers);

        var hashIdSuccessor = getSuccessor(hashID,clusterMembersIDs);

        var ipAddressSucc = getIpAddress(hashIdSuccessor,clusterMembers);

        var portSucc = getPortSuccessor(hashIdSuccessor,clusterMembers);

        sendTcpLeaveMessage(ipAddressSucc,portSucc,keyValuePairs);

    }

    private void sendTcpJoinMessage(String hashId, String ipRec, int portRec , String ipSend, int portSend){

        var str = new StringBuilder();

        str.append("sucessor;").append(hashId)
                .append(ipSend).append(String.valueOf(portSend));

        try (Socket socket = new Socket(ipRec, portRec)) {
            String message = str.toString();
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            writer.println(message);

        } catch (UnknownHostException ex) {

            System.out.println("Server not found: " + ex.getMessage());

        } catch (IOException ex) {

            System.out.println("I/O error: " + ex.getMessage());
        }

    }

    private void sendTcpLeaveMessage(String ip, int port, Map<String,String> KeyValuePairs){

        var str = new StringBuilder();

        str.append("retsucessor;");

        for (Map.Entry<String,String> pair : KeyValuePairs.entrySet()){
            str.append(pair.getKey()).append(";");
            str.append(pair.getValue()).append(";");
        }
        try (Socket socket = new Socket(ip, port)) {
            String message = str.toString();
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            writer.println(message);

        } catch (UnknownHostException ex) {

            System.out.println("Server not found: " + ex.getMessage());

        } catch (IOException ex) {

            System.out.println("I/O error: " + ex.getMessage());
        }

    }

    /**
     * UtilFuncs
     */

    private Integer getPortSuccessor(String hashID, Map<String,Integer> clusterMembers){

        int port = 0;

        for (Map.Entry<String,Integer> pair : clusterMembers.entrySet()) {
            var splitted = pair.getKey().split(";");

            var portNode = splitted[1];

            if (splitted[0].equals(hashID)) {
                port = Integer.parseInt(portNode);
            }
        }

        return port;

    }

    private String getIpAddress(String hashID, Map<String,Integer> clusterMembers){
        var ip = new String();

        for (Map.Entry<String,Integer> pair : clusterMembers.entrySet()){
            var splitted = pair.getKey().split(";");

            var ipAddress = splitted[2];

            if(splitted[0].equals(hashID)){
                ip = ipAddress;
            }
        }

        return ip;
    }

    private String getSuccessor(String hashID, List<String> clusterMembersIDs){
        String str = new String();

        BigInteger hashIdBi = new BigInteger(hashID,16);

        BigInteger minDiff = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF",16);

        for (var id: clusterMembersIDs){
            BigInteger bi = new BigInteger(id,16);

            BigInteger diff = bi.subtract(hashIdBi);

            if(diff.compareTo(new BigInteger("0")) == -1){
                continue;
            }
            else{
                if(diff.compareTo(minDiff) == -1){
                    minDiff = diff;
                    str = id;
                }
            }

        }
        return str;
    }

    private List<String> getClusterMembersIDs(Map<String, Integer> clusterMembers){
        List<String> clusterMembersIDs = new ArrayList<>();

        for (Map.Entry<String,Integer> pair: clusterMembers.entrySet()){

            var splittedKey = pair.getKey().split(";");

            clusterMembersIDs.add(splittedKey[0]);

        }

        return clusterMembersIDs;

    }


    /**
     *
     *
     */





}