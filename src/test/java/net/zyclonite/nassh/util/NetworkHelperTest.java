package net.zyclonite.nassh.util;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NetworkHelperTest {
    @Test
    public void testWholeIP4Range() throws UnknownHostException {
        NetworkHelper nwh = new NetworkHelper("0.0.0.0/0");
        assertTrue(nwh.isInRange(InetAddress.getByName("0.0.0.0")));
        assertTrue(nwh.isInRange(InetAddress.getByName("0.0.0.1")));
        assertTrue(nwh.isInRange(InetAddress.getByName("10.1.2.3")));
        assertTrue(nwh.isInRange(InetAddress.getByName("169.254.1.2")));
        assertTrue(nwh.isInRange(InetAddress.getByName("172.16.1.2")));
        assertTrue(nwh.isInRange(InetAddress.getByName("192.168.1.2")));
        assertTrue(nwh.isInRange(InetAddress.getByName("52.48.192.30")));
        assertTrue(nwh.isInRange(InetAddress.getByName("127.0.0.1")));
        assertTrue(nwh.isInRange(InetAddress.getByName("255.255.255.254")));
        assertTrue(nwh.isInRange(InetAddress.getByName("255.255.255.255")));
    }

    @Test
    public void testLocal192IP4Range() throws UnknownHostException {
        NetworkHelper nwh = new NetworkHelper("192.168.0.0/16");
        assertFalse(nwh.isInRange(InetAddress.getByName("0.0.0.0")));
        assertFalse(nwh.isInRange(InetAddress.getByName("0.0.0.1")));
        assertFalse(nwh.isInRange(InetAddress.getByName("10.1.2.3")));
        assertFalse(nwh.isInRange(InetAddress.getByName("169.254.1.2")));
        assertFalse(nwh.isInRange(InetAddress.getByName("172.16.1.2")));
        assertTrue(nwh.isInRange(InetAddress.getByName("192.168.1.2")));
        assertFalse(nwh.isInRange(InetAddress.getByName("52.48.192.30")));
        assertFalse(nwh.isInRange(InetAddress.getByName("127.0.0.1")));
        assertFalse(nwh.isInRange(InetAddress.getByName("255.255.255.254")));
        assertFalse(nwh.isInRange(InetAddress.getByName("255.255.255.255")));
    }

    @Test
    public void testLocal172IP4Range() throws UnknownHostException {
        NetworkHelper nwh = new NetworkHelper("172.16.0.0/16");
        assertFalse(nwh.isInRange(InetAddress.getByName("0.0.0.0")));
        assertFalse(nwh.isInRange(InetAddress.getByName("0.0.0.1")));
        assertFalse(nwh.isInRange(InetAddress.getByName("10.1.2.3")));
        assertFalse(nwh.isInRange(InetAddress.getByName("169.254.1.2")));
        assertTrue(nwh.isInRange(InetAddress.getByName("172.16.1.2")));
        assertFalse(nwh.isInRange(InetAddress.getByName("192.168.1.2")));
        assertFalse(nwh.isInRange(InetAddress.getByName("52.48.192.30")));
        assertFalse(nwh.isInRange(InetAddress.getByName("127.0.0.1")));
        assertFalse(nwh.isInRange(InetAddress.getByName("255.255.255.254")));
        assertFalse(nwh.isInRange(InetAddress.getByName("255.255.255.255")));
    }

    @Test
    public void testLocal10IP4Range() throws UnknownHostException {
        NetworkHelper nwh = new NetworkHelper("10.0.0.0/8");
        assertFalse(nwh.isInRange(InetAddress.getByName("0.0.0.0")));
        assertFalse(nwh.isInRange(InetAddress.getByName("0.0.0.1")));
        assertTrue(nwh.isInRange(InetAddress.getByName("10.1.2.3")));
        assertFalse(nwh.isInRange(InetAddress.getByName("169.254.1.2")));
        assertFalse(nwh.isInRange(InetAddress.getByName("172.16.1.2")));
        assertFalse(nwh.isInRange(InetAddress.getByName("192.168.1.2")));
        assertFalse(nwh.isInRange(InetAddress.getByName("52.48.192.30")));
        assertFalse(nwh.isInRange(InetAddress.getByName("127.0.0.1")));
        assertFalse(nwh.isInRange(InetAddress.getByName("255.255.255.254")));
        assertFalse(nwh.isInRange(InetAddress.getByName("255.255.255.255")));
    }
}
