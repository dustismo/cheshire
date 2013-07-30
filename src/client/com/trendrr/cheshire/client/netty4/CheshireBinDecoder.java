/**
 * 
 */
package com.trendrr.cheshire.client.netty4;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.trendrr.oss.exceptions.TrendrrException;
import com.trendrr.oss.strest.models.DefaultStrestResponse;
import com.trendrr.oss.strest.models.StrestHeader.ContentEncoding;
import com.trendrr.oss.strest.models.StrestHeader.TxnStatus;
import com.trendrr.oss.strest.models.StrestResponse;


/**
 * @author Dustin Norlander
 * @created Jul 29, 2013
 * 
 */
public class CheshireBinDecoder extends ByteToMessageDecoder {

	protected static Log log = LogFactory.getLog(CheshireBinDecoder.class);
	
	protected StrestResponse response = new DefaultStrestResponse();
	
	protected Position position = Position.TXN_ID_LEN;
	
	//if we read the length of a string, but not the string, yet
	protected int length;
	
//	[length][txn_id (string)]
//	[txn_status(int8)]
//	[status (int16)]
//	[length][status_message (string)]
//	[content_encoding (int8)]
//	[content_length (int32)][content (array)]
					
	protected enum Position {
		TXN_ID_LEN,
		TXN_ID,
		TXN_STATUS,
		STATUS,
		STATUS_MESSAGE_LEN,
		STATUS_MESSAGE,
		CONTENT_ENCODING,
		CONTENT_LEN,
		CONTENT,
		FINISHED
	}
	
	
	
	 @Override
     public void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out)
             throws Exception {
		 Position pos = this.next(this.position, buf);
		 while(pos != this.position) {
			 this.position = pos;
			 if (pos == Position.FINISHED) {
				 //send the response
				 out.add(this.response);
				 this.response = new DefaultStrestResponse();
				 this.position = Position.TXN_ID_LEN;
			 }
			 pos = this.next(this.position, buf);
		 }
		 this.position = pos;
     }
	 
	 /**
	  * reads the next section.  returns true if read, false if there are not enough bytes in the buffer.
	  * @param last
	  * @param buf
	  * @return
	  */
	 protected Position next(Position pos, ByteBuf buf) throws Exception {
		 int readable = buf.readableBytes();
		 if (readable < 2) {
			 return pos;
		 }
		 switch(pos) {
		 case TXN_ID_LEN :
			 this.length = buf.readShort();
			 return Position.TXN_ID;
		 case TXN_ID:
			 if (readable < this.length) {
				 return pos;
			 }
			 byte[] bytes = buf.readBytes(this.length).array();
			 this.response.setTxnId(new String(bytes, "utf8"));
			 return Position.TXN_STATUS;
		 case TXN_STATUS:
			 byte b = buf.readByte();
			 TxnStatus status = TxnStatus.instance(b);
			 if (status == null) {
				 throw new TrendrrException("Bad txn status! " + b);
			 }
			 this.response.setTxnStatus(status);
			 return Position.STATUS;
		 case STATUS:
			 short statusCode = buf.readShort();
			 this.response.setStatus(statusCode, "");
			 return Position.STATUS_MESSAGE_LEN;
		 case STATUS_MESSAGE_LEN:
			 this.length = buf.readShort();
			 if (this.length == 0) {
				 return Position.CONTENT_ENCODING;
			 }
			 return Position.STATUS_MESSAGE;
		 case STATUS_MESSAGE:
			 if (readable < this.length) {
				 return pos;
			 }
			 byte[] bt = buf.readBytes(this.length).array();
			 this.response.setStatus(this.response.getStatusCode(), new String(bt, "utf8"));
			 return Position.CONTENT_ENCODING;
		 case CONTENT_ENCODING:
			 byte ce = buf.readByte();
			 ContentEncoding contE = ContentEncoding.instance(ce);
			 if (contE == null) {
				 throw new TrendrrException("Bad content encoding " + ce);
			 }
			 this.response.setContent(contE, 0, null);
			 return Position.CONTENT_LEN;
		 case CONTENT_LEN:
			 if (readable < 4) {
				 return pos;
			 }
			 this.length = buf.readInt();
			 if (this.length == 0) {
				 return Position.FINISHED;
			 }
			 return Position.CONTENT;
		 case CONTENT:
			 if (readable < this.length) {
				 return pos;
			 }
			 
			 ByteBuf content = buf.readBytes(this.length);
			 this.response.setContent(this.response.getContentEncoding(), this.length, new ByteBufInputStream(content));
			 return Position.FINISHED;
		 }
		 
		 return pos;
	 }
	 
}
