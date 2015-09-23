package com.sf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by caojianbo on 15/8/10.
 */
public class DwnMd5 {

    /**
     * 对一个文件获取md5值
     * @return md5串
     */
    public static String getMD5(File file) {
        FileInputStream fileInputStream = null;
        try {

            MessageDigest MD5 = MessageDigest.getInstance("MD5");

            fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fileInputStream.read(buffer)) != -1) {
                MD5.update(buffer, 0, length);
            }
            byte [] md5byte = MD5.digest();
            int byteCounts = md5byte.length;
            char [] hexChar = new char[2 * byteCounts];
            int hexIndex = 0;
            for (int i = 0; i < byteCounts; i++) {
                hexChar[hexIndex++] = Integer.toHexString(((md5byte[i] >> 4) & 0x0f)).charAt(0);
                hexChar[hexIndex++] = Integer.toHexString((md5byte[i] & 0x0f)).charAt(0);
            }
            return new String(hexChar);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }   catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }  catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (fileInputStream != null)
                    fileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
