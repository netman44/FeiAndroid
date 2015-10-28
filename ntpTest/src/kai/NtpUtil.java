package kai;

import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * @author hongkai.qian
 * @version 1.0.0
 * @since 15-10-22
 */
public class NtpUtil {


    static String ntpServer = null;
    static Integer retry = null;
    static Integer port = null;
    static Integer timeout = null;

    /**
     * 读取time.txt中的配置，返回标准时间与本地时间的差值，单位秒
     *
     * @return
     */
    public static Double localClockOffset() {

        if (ntpServer == null) {
            ntpServer = "0.asia.pool.ntp.org";
            retry = 2;
            port = 123;
            timeout = 3000;
        }

        // get the address and NTP address request
        //
        InetAddress ipv4Addr = null;
        try {
            ipv4Addr = InetAddress.getByName(ntpServer);// 更多NTP时间服务器参考附注
        } catch (UnknownHostException e1) {
            e1.printStackTrace();
        }

        int serviceStatus = -1;
        DatagramSocket socket = null;
        long responseTime = -1;
        double localClockOffset = 0;
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(timeout); // will force the
            // InterruptedIOException

            for (int attempts = 0; attempts <= retry && serviceStatus != 1; attempts++) {
                try {
                    // Send NTP request
                    //
                    byte[] data = new NtpMessage().toByteArray();
                    DatagramPacket outgoing = new DatagramPacket(data,
                            data.length, ipv4Addr, port);
                    long sentTime = System.currentTimeMillis();
                    socket.send(outgoing);

                    // Get NTP Response
                    //
                    // byte[] buffer = new byte[512];
                    DatagramPacket incoming = new DatagramPacket(data,
                            data.length);
                    socket.receive(incoming);
                    responseTime = System.currentTimeMillis() - sentTime;
                    double destinationTimestamp = (System.currentTimeMillis() / 1000.0) + 2208988800.0;
                    // 这里要加2208988800，是因为获得到的时间是格林尼治时间，所以要变成东八区的时间，否则会与与北京时间有8小时的时差

                    // Validate NTP Response
                    // IOException thrown if packet does not decode as expected.
                    NtpMessage msg = new NtpMessage(incoming.getData());
                    localClockOffset = ((msg.receiveTimestamp - msg.originateTimestamp) + (msg.transmitTimestamp - destinationTimestamp)) / 2;
                    // System.out
                    // .println("poll: valid NTP request received the local clock offset is "
                    // + localClockOffset
                    // + ", responseTime= "
                    // + responseTime + "ms");
                    // System.out.println("poll: NTP message : " +
                    // msg.toString());
                    serviceStatus = 1;
                } catch (InterruptedIOException ex) {
                    // Ignore, no response received.
                }
            }
        } catch (NoRouteToHostException e) {
            System.out.println("No route to host exception for address: "
                    + ipv4Addr);
        } catch (ConnectException e) {
            // Connection refused. Continue to retry.
            e.fillInStackTrace();
            System.out.println("Connection exception for address: " + ipv4Addr);
        } catch (IOException ex) {
            ex.fillInStackTrace();
            System.out
                    .println("IOException while polling address: " + ipv4Addr);
        } finally {
            if (socket != null)
                socket.close();
        }

        // Store response time if available
        //
        // if (serviceStatus == 1) {
        // System.out.println("responsetime==" + responseTime);
        // }
        return localClockOffset;
    }
}

class NtpMessage {
    /** */
    /**
     * This is a two-bit code warning of an impending leap second to be
     * inserted/deleted in the last minute of the current day. It''s values may
     * be as follows:
     *
     * Value Meaning ----- ------- 0 no warning 1 last minute has 61 seconds 2
     * last minute has 59 seconds) 3 alarm condition (clock not synchronized)
     */
    public byte leapIndicator = 0;

    /** */
    /**
     * This value indicates the NTP/SNTP version number. The version number is 3
     * for Version 3 (IPv4 only) and 4 for Version 4 (IPv4, IPv6 and OSI). If
     * necessary to distinguish between IPv4, IPv6 and OSI, the encapsulating
     * context must be inspected.
     */
    public byte version = 3;

    /** */
    /**
     * This value indicates the mode, with values defined as follows:
     *
     * Mode Meaning ---- ------- 0 reserved 1 symmetric active 2 symmetric
     * passive 3 client 4 server 5 broadcast 6 reserved for NTP control message
     * 7 reserved for private use
     *
     * In unicast and anycast modes, the client sets this field to 3 (client) in
     * the request and the server sets it to 4 (server) in the reply. In
     * multicast mode, the server sets this field to 5 (broadcast).
     */
    public byte mode = 0;

    /** */
    /**
     * This value indicates the stratum level of the local clock, with values
     * defined as follows:
     *
     * Stratum Meaning ---------------------------------------------- 0
     * unspecified or unavailable 1 primary reference (e.g., radio clock) 2-15
     * secondary reference (via NTP or SNTP) 16-255 reserved
     */
    public short stratum = 0;

    /** */
    /**
     * This value indicates the maximum interval between successive messages, in
     * seconds to the nearest power of two. The values that can appear in this
     * field presently range from 4 (16 s) to 14 (16284 s); however, most
     * applications use only the sub-range 6 (64 s) to 10 (1024 s).
     */
    public byte pollInterval = 0;

    /** */
    /**
     * This value indicates the precision of the local clock, in seconds to the
     * nearest power of two. The values that normally appear in this field range
     * from -6 for mains-frequency clocks to -20 for microsecond clocks found in
     * some workstations.
     */
    public byte precision = 0;

    /** */
    /**
     * This value indicates the total roundtrip delay to the primary reference
     * source, in seconds. Note that this variable can take on both positive and
     * negative values, depending on the relative time and frequency offsets.
     * The values that normally appear in this field range from negative values
     * of a few milliseconds to positive values of several hundred milliseconds.
     */
    public double rootDelay = 0;

    /** */
    /**
     * This value indicates the nominal error relative to the primary reference
     * source, in seconds. The values that normally appear in this field range
     * from 0 to several hundred milliseconds.
     */
    public double rootDispersion = 0;

    /** */
    /**
     * This is a 4-byte array identifying the particular reference source. In
     * the case of NTP Version 3 or Version 4 stratum-0 (unspecified) or
     * stratum-1 (primary) servers, this is a four-character ASCII string, left
     * justified and zero padded to 32 bits. In NTP Version 3 secondary servers,
     * this is the 32-bit IPv4 address of the reference source. In NTP Version 4
     * secondary servers, this is the low order 32 bits of the latest transmit
     * timestamp of the reference source. NTP primary (stratum 1) servers should
     * set this field to a code identifying the external reference source
     * according to the following list. If the external reference is one of
     * those listed, the associated code should be used. Codes for sources not
     * listed can be contrived as appropriate.
     *
     * Code External Reference Source ---- ------------------------- LOCL
     * uncalibrated local clock used as a primary reference for a subnet without
     * external means of synchronization PPS atomic clock or other
     * pulse-per-second source individually calibrated to national standards
     * ACTS NIST dialup modem service USNO USNO modem service PTB PTB (Germany)
     * modem service TDF Allouis (France) Radio 164 kHz DCF Mainflingen
     * (Germany) Radio 77.5 kHz MSF Rugby (UK) Radio 60 kHz WWV Ft. Collins (US)
     * Radio 2.5, 5, 10, 15, 20 MHz WWVB Boulder (US) Radio 60 kHz WWVH Kaui
     * Hawaii (US) Radio 2.5, 5, 10, 15 MHz CHU Ottawa (Canada) Radio 3330,
     * 7335, 14670 kHz LORC LORAN-C radionavigation system OMEG OMEGA
     * radionavigation system GPS Global Positioning Service GOES Geostationary
     * Orbit Environment Satellite
     */
    public byte[] referenceIdentifier = { 0, 0, 0, 0 };

    /** */
    /**
     * This is the time at which the local clock was last set or corrected, in
     * seconds since 00:00 1-Jan-1900.
     */
    public double referenceTimestamp = 0;

    /** */
    /**
     * This is the time at which the request departed the client for the server,
     * in seconds since 00:00 1-Jan-1900.
     */
    public double originateTimestamp = 0;

    /** */
    /**
     * This is the time at which the request arrived at the server, in seconds
     * since 00:00 1-Jan-1900.
     */
    public double receiveTimestamp = 0;

    /** */
    /**
     * This is the time at which the reply departed the server for the client,
     * in seconds since 00:00 1-Jan-1900.
     */
    public double transmitTimestamp = 0;

    /** */
    /**
     * Constructs a new NtpMessage from an array of bytes.
     */
    public NtpMessage(byte[] array) {
        // See the packet format diagram in RFC 2030 for details
        leapIndicator = (byte) ((array[0] >> 6) & 0x3);
        version = (byte) ((array[0] >> 3) & 0x7);
        mode = (byte) (array[0] & 0x7);
        stratum = unsignedByteToShort(array[1]);
        pollInterval = array[2];
        precision = array[3];

        rootDelay = (array[4] * 256.0) + unsignedByteToShort(array[5])
                + (unsignedByteToShort(array[6]) / 256.0)
                + (unsignedByteToShort(array[7]) / 65536.0);

        rootDispersion = (unsignedByteToShort(array[8]) * 256.0)
                + unsignedByteToShort(array[9])
                + (unsignedByteToShort(array[10]) / 256.0)
                + (unsignedByteToShort(array[11]) / 65536.0);

        referenceIdentifier[0] = array[12];
        referenceIdentifier[1] = array[13];
        referenceIdentifier[2] = array[14];
        referenceIdentifier[3] = array[15];

        referenceTimestamp = decodeTimestamp(array, 16);
        originateTimestamp = decodeTimestamp(array, 24);
        receiveTimestamp = decodeTimestamp(array, 32);
        transmitTimestamp = decodeTimestamp(array, 40);
    }

    /** */
    /**
     * Constructs a new NtpMessage
     */
    public NtpMessage(byte leapIndicator, byte version, byte mode,
                      short stratum, byte pollInterval, byte precision, double rootDelay,
                      double rootDispersion, byte[] referenceIdentifier,
                      double referenceTimestamp, double originateTimestamp,
                      double receiveTimestamp, double transmitTimestamp) {
        // ToDo: Validity checking
        this.leapIndicator = leapIndicator;
        this.version = version;
        this.mode = mode;
        this.stratum = stratum;
        this.pollInterval = pollInterval;
        this.precision = precision;
        this.rootDelay = rootDelay;
        this.rootDispersion = rootDispersion;
        this.referenceIdentifier = referenceIdentifier;
        this.referenceTimestamp = referenceTimestamp;
        this.originateTimestamp = originateTimestamp;
        this.receiveTimestamp = receiveTimestamp;
        this.transmitTimestamp = transmitTimestamp;
    }

    /** */
    /**
     * Constructs a new NtpMessage in client -> server mode, and sets the
     * transmit timestamp to the current time.
     */
    public NtpMessage() {
        // Note that all the other member variables are already set with
        // appropriate default values.
        this.mode = 3;
        this.transmitTimestamp = (System.currentTimeMillis() / 1000.0) + 2208988800.0;
    }

    /** */
    /**
     * This method constructs the data bytes of a raw NTP packet.
     */
    public byte[] toByteArray() {
        // All bytes are automatically set to 0
        byte[] p = new byte[48];

        p[0] = (byte) (leapIndicator << 6 | version << 3 | mode);
        p[1] = (byte) stratum;
        p[2] = (byte) pollInterval;
        p[3] = (byte) precision;

        // root delay is a signed 16.16-bit FP, in Java an int is 32-bits
        int l = (int) (rootDelay * 65536.0);
        p[4] = (byte) ((l >> 24) & 0xFF);
        p[5] = (byte) ((l >> 16) & 0xFF);
        p[6] = (byte) ((l >> 8) & 0xFF);
        p[7] = (byte) (l & 0xFF);

        // root dispersion is an unsigned 16.16-bit FP, in Java there are no
        // unsigned primitive types, so we use a long which is 64-bits
        long ul = (long) (rootDispersion * 65536.0);
        p[8] = (byte) ((ul >> 24) & 0xFF);
        p[9] = (byte) ((ul >> 16) & 0xFF);
        p[10] = (byte) ((ul >> 8) & 0xFF);
        p[11] = (byte) (ul & 0xFF);

        p[12] = referenceIdentifier[0];
        p[13] = referenceIdentifier[1];
        p[14] = referenceIdentifier[2];
        p[15] = referenceIdentifier[3];

        encodeTimestamp(p, 16, referenceTimestamp);
        encodeTimestamp(p, 24, originateTimestamp);
        encodeTimestamp(p, 32, receiveTimestamp);
        encodeTimestamp(p, 40, transmitTimestamp);

        return p;
    }

    /** */
    /**
     * Returns a string representation of a NtpMessage
     */
    public String toString() {
        String precisionStr = new DecimalFormat("0.#E0").format(Math.pow(2,
                precision));
        return "Leap indicator: "
                + leapIndicator
                + " "
                + "Version: "
                + version
                + " "
                + "Mode: "
                + mode
                + " "
                + "Stratum: "
                + stratum
                + " "
                + "Poll: "
                + pollInterval
                + " "
                + "Precision: "
                + precision
                + " ("
                + precisionStr
                + " seconds) "
                + "Root delay: "
                + new DecimalFormat("0.00").format(rootDelay * 1000)
                + " ms "
                + "Root dispersion: "
                + new DecimalFormat("0.00").format(rootDispersion * 1000)
                + " ms "
                + "Reference identifier: "
                + referenceIdentifierToString(referenceIdentifier, stratum,
                version) + " " + "Reference timestamp: "
                + timestampToString(referenceTimestamp) + " "
                + "Originate timestamp: "
                + timestampToString(originateTimestamp) + " "
                + "Receive timestamp:   " + timestampToString(receiveTimestamp)
                + " " + "Transmit timestamp: "
                + timestampToString(transmitTimestamp);
    }

    /** */
    /**
     * Converts an unsigned byte to a short. By default, Java assumes that a
     * byte is signed.
     */
    public static short unsignedByteToShort(byte b) {
        if ((b & 0x80) == 0x80)
            return (short) (128 + (b & 0x7f));
        else
            return (short) b;
    }

    /** */
    /**
     * Will read 8 bytes of a message beginning at <code>pointer</code> and
     * return it as a double, according to the NTP 64-bit timestamp format.
     */
    public static double decodeTimestamp(byte[] array, int pointer) {
        double r = 0.0;

        for (int i = 0; i < 8; i++) {
            r += unsignedByteToShort(array[pointer + i])
                    * Math.pow(2, (3 - i) * 8);
        }

        return r;
    }

    /** */
    /**
     * Encodes a timestamp in the specified position in the message
     */
    public static void encodeTimestamp(byte[] array, int pointer,
                                       double timestamp) {
        // Converts a double into a 64-bit fixed point
        for (int i = 0; i < 8; i++) {
            // 2^24, 2^16, 2^8, .. 2^-32
            double base = Math.pow(2, (3 - i) * 8);

            // Capture byte value
            array[pointer + i] = (byte) (timestamp / base);

            // Subtract captured value from remaining total
            timestamp = timestamp
                    - (double) (unsignedByteToShort(array[pointer + i]) * base);
        }

        // From RFC 2030: It is advisable to fill the non-significant
        // low order bits of the timestamp with a random, unbiased
        // bitstring, both to avoid systematic roundoff errors and as
        // a means of loop detection and replay detection.
        array[7] = (byte) (Math.random() * 255.0);
    }

    /** */
    /**
     * Returns a timestamp (number of seconds since 00:00 1-Jan-1900) as a
     * formatted date/time string.
     */
    public static String timestampToString(double timestamp) {
        if (timestamp == 0)
            return "0";

        // timestamp is relative to 1900, utc is used by Java and is relative
        // to 1970
        double utc = timestamp - (2208988800.0);

        // milliseconds
        long ms = (long) (utc * 1000.0);

        // date/time
        String date = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss")
                .format(new Date(ms));

        // fraction
        double fraction = timestamp - ((long) timestamp);
        String fractionSting = new DecimalFormat(".000000").format(fraction);

        return date + fractionSting;
    }

    /** */
    /**
     * Returns a string representation of a reference identifier according to
     * the rules set out in RFC 2030.
     */
    public static String referenceIdentifierToString(byte[] ref, short stratum,
                                                     byte version) {
        // From the RFC 2030:
        // In the case of NTP Version 3 or Version 4 stratum-0 (unspecified)
        // or stratum-1 (primary) servers, this is a four-character ASCII
        // string, left justified and zero padded to 32 bits.
        if (stratum == 0 || stratum == 1) {
            return new String(ref);
        }

        // In NTP Version 3 secondary servers, this is the 32-bit IPv4
        // address of the reference source.
        else if (version == 3) {
            return unsignedByteToShort(ref[0]) + "."
                    + unsignedByteToShort(ref[1]) + "."
                    + unsignedByteToShort(ref[2]) + "."
                    + unsignedByteToShort(ref[3]);
        }

        // In NTP Version 4 secondary servers, this is the low order 32 bits
        // of the latest transmit timestamp of the reference source.
        else if (version == 4) {
            return ""
                    + ((unsignedByteToShort(ref[0]) / 256.0)
                    + (unsignedByteToShort(ref[1]) / 65536.0)
                    + (unsignedByteToShort(ref[2]) / 16777216.0) + (unsignedByteToShort(ref[3]) / 4294967296.0));
        }
        return "";
    }
}