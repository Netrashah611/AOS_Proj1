import java.util.Objects;

/**
 * Class to represent node connection information

 */
public class NodeDetails {
    private final int portNo;

    private final String hostName;

    public NodeDetails(final String hostName, final int portNo) {
        this.portNo = portNo;
        this.hostName = hostName;
    }

    public int getPortNo() {
        return portNo;
    }

    public String getHostName() {
        return hostName;
    }

    @Override
    public boolean equals(Object other) {
        if(!(other instanceof NodeDetails))
            return false;

        NodeDetails that = (NodeDetails) other;
        return this.hostName.equalsIgnoreCase(that.hostName)
            && this.portNo == that.portNo;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostName, portNo);
    }

    @Override
    public String toString() {
        return hostName + ":" + portNo + "\n";
    }
}
