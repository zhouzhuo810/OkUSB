package me.zhouzhuo810.okusb.tools;

/**
 *
 * Created by zz on 2016/11/10.
 */
public class DataUtil {

    /**
     * byte[] to string,  for print byte[].
     *
     * @param bytes byte[]
     * @return string str
     */
    public static String byteArrayToString(byte[] bytes) {
        String a = "";
        for (byte aByte : bytes) {
            a = a + " " + byteToHexString(aByte);
        }
        return a;
    }

    /**
     * byte to decimal string
     *
     * @param b byte , like 0x0A
     * @return decimal stirng , like 10
     */
    public static String byteToDecimalString(byte b) {
        return Integer.parseInt(byteToHexString(b), 16) + "";
    }

    /**
     * decimal string to byte
     *
     * @param decimalStr decimalStr , like 10
     * @return byte, like 0x0A
     */
    public static byte decStringToByte(String decimalStr) {
        int value = Integer.parseInt(decimalStr);
        return (byte)value;
    }

    /**
     * byte to hexString, 0x2B to 2B
     *
     * @param b byte
     * @return hexString
     */
    public static String byteToHexString(byte b) {
        String hex = Integer.toHexString(b & 0xFF);
        if (hex.length() == 1) {
            hex = '0' + hex;
        }
        return hex;
    }

    /**
     * byte[] to int
     *
     * @param b byte[] length = 4
     * @return int
     */
    public static int byteArrayToInt(byte[] b) {
        return b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    /**
     * int to byte[]
     *
     * @param value int
     * @return byte[] length = 4
     */
    public static byte[] intToBytes(int value) {
        byte[] src = new byte[4];
        src[3] = (byte) ((value >> 24) & 0xFF);
        src[2] = (byte) ((value >> 16) & 0xFF);
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[0] = (byte) (value & 0xFF);
        return src;
    }

    /**
     * short to int
     *
     * @param s short
     * @return byte[] length = 2
     */
    public static byte[] shortToByteArray(short s) {
        byte[] targets = new byte[2];
        for (int i = 0; i < 2; i++) {
            int offset = (targets.length - 1 - i) * 8;
            targets[i] = (byte) ((s >>> offset) & 0xff);
        }
        return targets;
    }
}
