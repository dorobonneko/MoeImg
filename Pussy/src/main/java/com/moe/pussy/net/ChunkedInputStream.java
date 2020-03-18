package com.moe.pussy.net;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

public class ChunkedInputStream extends InputStream
{
		private  InputStream hostStream; //要解析的input流
		private  boolean isCkunked = true;
		private  int readMetaSize = 0; //读取的实际总长度
		private  long contentLength = 0; //读取到的内容长度
		private  long chunkedNextLength = -1L; //指示要读取得字节流长度
		private  long chunkedCurrentLength = 0L; //指示当前已经读取的字节流长度

		public ChunkedInputStream(InputStream inputStream) throws IOException 
		{
			this.hostStream = inputStream;
		}
		public int getReadMetaSize() {
			return readMetaSize;
		}
		public long getContentLength() 
		{
			return contentLength;
		}
		/**
		 * 解析响应头
		 * @throws IOException
		 */
		
		/**
		 * 字节流读取
		 */
		@Override
		public int read() throws IOException {
			return  readChunked();
		}

		private int readChunked() throws IOException {


			byte[] chunkedFlagBuf  = new byte[0]; //用于缓冲chunked编码数据的length标志行
			int crlf_nums = 0;
			int byteCode = -1;

			if(chunkedNextLength==-1L) // -1表示需要获取 chunkedNextLength大小，也就是chunked数据length标志
			{
				byteCode = hostStream.read();
				readMetaSize++;

				while(byteCode!=-1)
				{
					int outLength = chunkedFlagBuf.length+1;
					byte[] tempBuf = chunkedFlagBuf;
					chunkedFlagBuf = new byte[outLength];
					System.arraycopy(tempBuf, 0, chunkedFlagBuf,0, tempBuf.length);
					System.arraycopy(new byte[]{(byte) byteCode}, 0, chunkedFlagBuf,chunkedFlagBuf.length-1, 1);

					if(byteCode==0x0D || byteCode==0x0A) //记录回车换行
					{
						crlf_nums++;
						if(crlf_nums==2) //如果回车换行计数为2，进行检测
						{
							String lineNo = "0x"+ (new String(chunkedFlagBuf,0,chunkedFlagBuf.length)).trim();
							chunkedNextLength = Long.decode(lineNo);
							contentLength+=chunkedNextLength;

							if(chunkedNextLength>0)
							{
								byteCode = hostStream.read();
								readMetaSize++;
							}

							break;
						}

					}else{
						crlf_nums=0;
					}
					byteCode = hostStream.read();
					readMetaSize++;
				}
			}
			else if(chunkedNextLength>0) //表示要读取得片段长度
			{
				if(chunkedCurrentLength<chunkedNextLength)
				{
					byteCode = hostStream.read();
					readMetaSize++;
					chunkedCurrentLength++; //读取时加一，记录长度
				}
				if(chunkedCurrentLength==chunkedNextLength){  //内容长度和标志长度相同，说明长度为chunkedCurrentLength的数据已经被读取到了
					chunkedNextLength = -1L;
					chunkedCurrentLength = 0L;
				}

			}else{
		        //读取结束，此时更新内容总长度到header中
				return -1;//chunked流不会返回-1，这里针对chunkedLength=0强制返回-1
			}

			return byteCode;
		}



		@Override
		public long skip(long n) throws IOException {
			return hostStream.skip(n);
		}

		@Override
		public boolean markSupported() {
			return hostStream.markSupported();
		}

		@Override
		public int available() throws IOException {
			return hostStream.available();
		}

		@Override
		public synchronized void mark(int readlimit) {
			hostStream.mark(readlimit);
		}


		@Override
		public void close() throws IOException {
			hostStream.close();
		}

		@Override
		public synchronized void reset() throws IOException {
			hostStream.reset();
		}
	
}
