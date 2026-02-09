/*
 * nassh-relay - Relay Server for tunneling ssh through a http endpoint
 *
 * Website: https://github.com/zyclonite/nassh-relay
 *
 * Copyright 2014-2020   zyclonite    networx
 *                       http://zyclonite.net
 * Developer: Lukas Prettenthaler
 */
package net.zyclonite.nassh.util;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * @author zyclonite
 */
public class NetworkHelper {

    private final InetAddress inetAddress;
    private InetAddress startAddress;
    private InetAddress endAddress;
    private final int prefixLength;

    public NetworkHelper(final String cidr) throws UnknownHostException {
        if (cidr.contains("/")) {
            var index = cidr.indexOf("/");
            var addressPart = cidr.substring(0, index);
            var networkPart = cidr.substring(index + 1);
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
        var mask = (new BigInteger(1, maskBuffer.array())).not().shiftRight(prefixLength);
        var buffer = ByteBuffer.wrap(inetAddress.getAddress());
        var ipVal = new BigInteger(1, buffer.array());
        var startIp = ipVal.and(mask);
        var endIp = startIp.add(mask.not());
        var startIpArr = toBytes(startIp.toByteArray(), targetSize);
        var endIpArr = toBytes(endIp.toByteArray(), targetSize);
        this.startAddress = InetAddress.getByAddress(startIpArr);
        this.endAddress = InetAddress.getByAddress(endIpArr);

    }

    private byte[] toBytes(final byte[] array, final int targetSize) {
        var counter = 0;
        var newArr = new ArrayList<Byte>();
        while (counter < targetSize && (array.length - 1 - counter >= 0)) {
            newArr.addFirst(array[array.length - 1 - counter]);
            counter++;
        }
        var size = newArr.size();
        for (var i = 0; i < (targetSize - size); i++) {
            newArr.addFirst((byte) 0);
        }
        var ret = new byte[newArr.size()];
        for (var i = 0; i < newArr.size(); i++) {
            ret[i] = newArr.get(i);
        }
        return ret;
    }

    public boolean isInRange(final InetAddress address) {
        var start = new BigInteger(1, this.startAddress.getAddress());
        var end = new BigInteger(1, this.endAddress.getAddress());
        var target = new BigInteger(1, address.getAddress());
        var st = start.compareTo(target);
        var te = target.compareTo(end);
        return (st < 0 || st == 0) && (te < 0 || te == 0);
    }
}
