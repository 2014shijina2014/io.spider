package com.ld.core.pack;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.EndianUtils;

import com.ld.core.pack.LDPack.PChar;
import com.ld.core.pack.LDPack.PFieldName;

public class DataSetReader {
	
	protected ByteArrayInputStream input;
	
	protected ArrayList<String> fieldNames;
	protected ArrayList<Integer> fieldSizes;
	protected ArrayList<LDPack> plist = new ArrayList<LDPack>();
	
	public DataSetReader(byte[] input) {
		this.input = new ByteArrayInputStream(input);
	}
	
	public DataSetReader(ByteArrayInputStream input) {
		this.input = input;
	}
	
	protected int colCount;
	protected int rowCount;
	
	static PFieldName ps = new PFieldName();
	static PChar pc = new PChar();
	
	protected int valueStartPosition = 0;
	
	public void startRead() throws IOException {
		this.colCount = input.read();
		this.rowCount = EndianUtils.readSwappedInteger(input);
		
		this.fieldNames = new ArrayList<String>();
		this.fieldSizes = new ArrayList<Integer>();
		
		ArrayList<LDPack> plist = new ArrayList<LDPack>();
		
		for(int i=0;i<colCount;i++) {
			String name = ps.unpack(input);
			char type = pc.unpack(input);
			switch(type) {
			case LDDataType.STRING:
			case LDDataType.BYTE_ARRAY:
				this.fieldSizes.add(-1);
				break;
			case LDDataType.DECIMAL:
				this.fieldSizes.add(8);
				break;
			case LDDataType.INT:
				this.fieldSizes.add(4);
				break;
			case LDDataType.CHAR:
				this.fieldSizes.add(1);
				break;
			}
			this.fieldNames.add(name);
			plist.add(LDPack.getPack(type));
		}
		
	}
	
	
}
