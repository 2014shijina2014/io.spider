package com.ld.core.pack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.HashMap;

import org.apache.commons.io.EndianUtils;

import com.ld.core.pack.utils.CastUtils;

public abstract class LDPack<T> {
	public static String CHARSET = "GBK";
	public abstract void pack(OutputStream output,T value) throws IOException;
	public abstract T unpack(InputStream input) throws IOException;
	public abstract Character getType();
	
	protected static HashMap<Character, LDPack> packs = new HashMap<Character, LDPack>();
	
	static {
		packs.put(LDDataType.BYTE_ARRAY, new PByteArray());
		packs.put(LDDataType.CHAR, new PChar());
		packs.put(LDDataType.LONG, new PLong());
		packs.put(LDDataType.DOUBLE, new PDouble());
		packs.put(LDDataType.DECIMAL, new PDecimal());
		packs.put(LDDataType.INT, new PInt());
		packs.put(LDDataType.STRING, new PString());
	}
	
	public static LDPack getPack(Character type) {
		return packs.get(type);
	}
	
	protected static class PInt extends LDPack<Integer> {
		public void pack(OutputStream output, Integer value) throws IOException{
			if(value == null)
				value = 0;
			EndianUtils.writeSwappedInteger(output, value);
		}
		public Integer unpack(InputStream input) throws IOException {
			return EndianUtils.readSwappedInteger(input);
		}
		public Character getType() {
			return LDDataType.INT;
		}
	}
	
	protected static class PChar extends LDPack<Character> {
		public void pack(OutputStream output, Character value) throws IOException{
			if(value == null)
				value = 0;
			output.write(value&0xFF);
		}
		public Character unpack(InputStream input) throws IOException {
			return (char)input.read();
		}
		public Character getType() {
			return LDDataType.CHAR;
		}
	}
	
	protected static class PLong extends LDPack<Long> {
		public void pack(OutputStream output, Long value) throws IOException{
			if(value == null)
				EndianUtils.writeSwappedLong(output, 0);
			else
				EndianUtils.writeSwappedLong(output, value);
		}
		public Long unpack(InputStream input) throws IOException {
			return EndianUtils.readSwappedLong(input);
		}
		public Character getType() {
			return LDDataType.LONG;
		}
	}
	
	
	protected static class PDouble extends LDPack<Double> {
		public void pack(OutputStream output, Double value) throws IOException{
			if(value == null){
				EndianUtils.writeSwappedDouble(output, 0);
			}
			else
			{
				EndianUtils.writeSwappedDouble(output, value.doubleValue());
			}
		}
		public Double unpack(InputStream input) throws IOException {
			return EndianUtils.readSwappedDouble(input);
		}
		public Character getType() {
			return LDDataType.DECIMAL;
		}
	}
	
	protected static class PDecimal extends LDPack<BigDecimal> {
		
		protected PString string = new PString();
		
		public void pack(OutputStream output, BigDecimal value) throws IOException{
			if(value == null)
				string.pack(output, null);
			else
				string.pack(output, CastUtils.cast(value, LDDataType.STRING));
		}
		public BigDecimal unpack(InputStream input) throws IOException {
			String ret = string.unpack(input);
			if(ret == null)
				return null;
			return new BigDecimal(ret);
		}
		public Character getType() {
			return LDDataType.DECIMAL;
		}
	}
	
	protected static class PByteArray extends LDPack<byte[]> {

		@Override
		public void pack(OutputStream output, byte[] value) throws IOException {
			int len = 0;
			if(value == null){
				len = -1;
				EndianUtils.writeSwappedInteger(output, len);
			}
			else if(value.length == 0){
				len = 0;
				EndianUtils.writeSwappedInteger(output, len);
			}
			else
			{
				len = value.length;
				EndianUtils.writeSwappedInteger(output, len);
				output.write(value);
			}
		}

		@Override
		public byte[] unpack(InputStream input) throws IOException {
			int len = EndianUtils.readSwappedInteger(input);
			if(len>0){
				byte[] bytes = new byte[len];
				input.read(bytes);
				return bytes;
			}
			else if(len == 0){
				return new byte[0];
			}
			else
			{
				return null;
			}
		}
		
		public Character getType() {
			return LDDataType.BYTE_ARRAY;
		}
		
	}
	
	protected static class PString extends LDPack<Object> {
		public void pack(OutputStream output, Object value) throws IOException{
			short len = 0;
			byte[] bytes;
			if(value == null){
				len = -1;
				EndianUtils.writeSwappedShort(output, len);
			}
			else if(value.toString().isEmpty()){
				len = 0;
				EndianUtils.writeSwappedShort(output, len);
			}
			else
			{
				bytes = value.toString().getBytes(CHARSET);
				len = (short) bytes.length;
				EndianUtils.writeSwappedShort(output, len);
				output.write(bytes);
			}
		}
		public String unpack(InputStream input) throws IOException {
			short len = EndianUtils.readSwappedShort(input);
			if(len>0){
				byte[] bytes = new byte[len];
				input.read(bytes);
				return new String(bytes,CHARSET);
			}
			else if(len == 0){
				return "";
			}
			else
			{
				return null;
			}
		}
		public Character getType() {
			return LDDataType.STRING;
		}
	}
	
	protected static class PFieldName extends LDPack<String> {
		public void pack(OutputStream output, String value) throws IOException{
			byte len = 0;
			byte[] bytes;
			if(value == null){
				len = -1;
				output.write(len&0xFF);
			}
			else if(value.isEmpty()){
				len = 0;
			}
			else
			{
				bytes = value.getBytes(CHARSET);
				len = (byte) bytes.length;
				output.write(len);
				output.write(bytes);
			}
		}
		public String unpack(InputStream input) throws IOException {
			byte len = (byte) input.read();
			if(len>0){
				byte[] bytes = new byte[len];
				input.read(bytes);
				return new String(bytes,CHARSET);
			}
			else if(len == 0){
				return "";
			}
			else
			{
				return null;
			}
		}
		public Character getType() {
			return LDDataType.STRING;
		}
	}
}
