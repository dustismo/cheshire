/**
 * 
 */
package com.trendrr.cheshire.client.netty4;

import java.io.UnsupportedEncodingException;

import com.trendrr.oss.exceptions.TrendrrException;
import com.trendrr.oss.exceptions.TrendrrOverflowException;
import io.netty.buffer.ByteBuf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * @author Dustin Norlander
 * @created Jul 29, 2013
 * 
 */
public class BinaryHelper {

	protected static Log log = LogFactory.getLog(BinaryHelper.class);

    public static void writeString32(ByteBuf out, String str) {
        if (str == null) {
            out.writeInt(0);
            return;
        }
        try {
            byte[] bytes = str.getBytes("utf8");
            out.writeInt(bytes.length);
            out.writeBytes(bytes);
        } catch (UnsupportedEncodingException e) {
            out.writeShort((short)0);
            log.error("Caught", e);
        }
    }
	public static void writeString(ByteBuf out, String str) throws TrendrrException {
		if (str == null) {
			out.writeShort((short)0);
			return;
		}
		
		try {
			byte[] bytes = str.getBytes("utf8");
            if (bytes.length > Short.MAX_VALUE) {
                throw new TrendrrOverflowException("String too long!");
            }

			out.writeShort((short)bytes.length);
			out.writeBytes(bytes);
		} catch (UnsupportedEncodingException e) {
			throw new TrendrrException("Bad string encoding!", e);
		}
	}
}
