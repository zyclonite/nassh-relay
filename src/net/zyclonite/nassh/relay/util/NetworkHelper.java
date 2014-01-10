/*
 * nassh-relay - Relay Server for tunneling ssh through a http endpoint
 * 
 * Website: http://relay.wsn.at
 *
 * Copyright 2014   zyclonite    networx
 *                  http://zyclonite.net
 * Developer: Lukas Prettenthaler
 */
package net.zyclonite.nassh.relay.util;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author zyclonite
 */
public class NetworkHelper {

    private final String cidr;
    private InetAddress inetAddress;
    private InetAddress startAddress;
    private InetAddress endAddress;
    private final int prefixLength;

    public NetworkHelper(final String cidr) throws UnknownHostException {
        this.cidr = cidr;
        if (this.cidr.contains("/")) {
            final int index = this.cidr.indexOf("/");
            final String addressPart = this.cidr.substring(0, index);
            final String networkPart = this.cidr.substring(index + 1);
            inetAddress = InetAddress.getByName(addressPart);
            prefixLength = Integer.parseInt(networkPart);
            calculate();
        } else {
            throw new IllegalArgumentException("not valid range");
        }
    }

    private void calculate() throws UnknownHostException {
        final ByteBuffer maskBuffer;
        final int targetSize;
        if (inetAddress.getAddress().length == 4) {
            maskBuffer = ByteBuffer.allocate(4).putInt(-1);
            targetSize = 4;
        } else {
            maskBuffer = ByteBuffer.allocate(16).putLong(-1L).putLong(-1L);
            targetSize = 16;
        }
        final BigInteger mask = (new BigInteger(1, maskBuffer.array())).not().shiftRight(prefixLength);
        final ByteBuffer buffer = ByteBuffer.wrap(inetAddress.getAddress());
        final BigInteger ipVal = new BigInteger(1, buffer.array());
        final BigInteger startIp = ipVal.and(mask);
        final BigInteger endIp = startIp.add(mask.not());
        final byte[] startIpArr = toBytes(startIp.toByteArray(), targetSize);
        final byte[] endIpArr = toBytes(endIp.toByteArray(), targetSize);
        this.startAddress = InetAddress.getByAddress(startIpArr);
        this.endAddress = InetAddress.getByAddress(endIpArr);

    }

    private byte[] toBytes(final byte[] array, final int targetSize) {
        int counter = 0;
        final List<Byte> newArr = new ArrayList<>();
        while (counter < targetSize && (array.length - 1 - counter >= 0)) {
            newArr.add(0, array[array.length - 1 - counter]);
            counter++;
        }
        final int size = newArr.size();
        for (int i = 0; i < (targetSize - size); i++) {
            newArr.add(0, (byte) 0);
        }
        final byte[] ret = new byte[newArr.size()];
        for (int i = 0; i < newArr.size(); i++) {
            ret[i] = newArr.get(i);
        }
        return ret;
    }

    public String getNetworkAddress() {
        return this.startAddress.getHostAddress();
    }

    public String getBroadcastAddress() {
        return this.endAddress.getHostAddress();
    }

    public boolean isInRange(final InetAddress address) {
        final BigInteger start = new BigInteger(1, this.startAddress.getAddress());
        final BigInteger end = new BigInteger(1, this.endAddress.getAddress());
        final BigInteger target = new BigInteger(1, address.getAddress());
        final int st = start.compareTo(target);
        final int te = target.compareTo(end);
        return (st == -1 || st == 0) && (te == -1 || te == 0);
    }
}