package com.ld.core.pack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.io.EndianUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ld.core.pack.LDPack.PChar;
import com.ld.core.pack.LDPack.PFieldName;
import com.ld.core.pack.utils.CastUtils;


/**
 * 结果集合
 * type 看 @see org.ld.net.data.DataType <br>
 * 字段头：
 * |  version | column | row | name1       | type1 | name2       | type2 | ... | name[column] | type[column] |
 * |  byte    |   int  | int | byte+string | byte  | byte+string | byte  | ... | byte+string  | byte        |
 * 行1：| col_value | ... | col_value |
 * 值1：| int,char,double | short+string | int+bytes |
 * ...
 * 行[row]: | col_value | ... | col_value |
 * 
 * 
 * @author zhangcb
 *
 */
public class DataSet {
	protected int version;
	
	protected final HashMap<String, Character> fields;
	
	protected final HashMap<String, Integer> fieldIndexs;
	
	protected ArrayList<String> fieldNames = new ArrayList<String>();
	
	protected ArrayList<Object> values = new ArrayList<Object>();
	
	static Logger logger = LoggerFactory.getLogger(DataSet.class);
	
	public DataSet() {
		fields = new HashMap<String, Character>();
		fieldIndexs = new HashMap<String, Integer>();
	}
	
	public HashMap<String, Character> getFields(){
		return this.fields;
	}
	
	public HashMap<String, Integer> getFieldIndex() {
		return this.fieldIndexs;
	}
	
	public ArrayList<String> getFieldNames(){
		return this.fieldNames;
	}
	
	public ArrayList<Object> getValues(){
		return this.values;
	}
	
	public void setValues(ArrayList<Object> value) {
		this.values = value;
	}
	
	protected int cur_row = 0;
	
	public boolean hasNextRow(){
		return this.rowCount>cur_row;
	}
	
	public int currentRowIndex() {
		return cur_row;
	}
	
	public List<Object> nextRow() {
		int colCount = this.getColumnCount();
		List<Object> ret = this.values.subList(this.cur_row*colCount, (++this.cur_row)*colCount);
		while(this.removeRowSet.contains(cur_row)) {
			cur_row++;
		}
		return ret;
	}
	
	public List<Object> getRow(int row) {
		if(row<1)
			row = 1;
		int colCount = this.getColumnCount();
		return this.values.subList((row-1)*colCount, row*colCount);
	}
	
	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public int getColumnCount() {
		return fieldNames.size();
	}

	protected int rowCount = 0;
	
	public int getRowCount() {
		return rowCount-removeRowSet.size();
	}
	
	public void setRowCount(int count) {
		this.rowCount = count;
	}
	
	/**
	 * 添加一个Field
	 * @param name 字段名
	 * @param type 字段类型 LDDataType里面的
	 */
	public void addField(String name,char type){
		fields.put(name, type);
		fieldIndexs.put(name, fieldNames.size());
		fieldNames.add(name);
	}
	
	public void setFieldData(String name,Object value,Class cls){
		char type = LDDataType.getType(cls);
		setFieldData(name, value, type);
	}
	
	public void setFieldData(String name,Object value,char type) {
		if(type == LDDataType.UNKNOW){
			if(value != null) {
				LDConvert convert = LDConvert.getConvert(value.getClass());
				addField(name, convert.getType());
			} else {
				//默认全部是字符串
				addField(name, LDDataType.STRING);
			}
		} else {
			addField(name, type);
		}
		nextValue(value);
	}
	
	public void setValue(String name,Object value,int row) {
		int findex = this.fieldNames.indexOf(name);
		if(findex==-1) {
			logger.info("setValue failed,name={},value={},row={}",name,value,row);
			return;
		}
		if(row<1) {
			row = 1;
		}
		this.values.set(fieldNames.size()*(row-1)+findex, value);
	}
	
	public boolean containField(String fieldName) {
		return this.fields.containsKey(fieldName);
	}
	
	protected int curColumn;
	
	/**
	 * 逐行逐个往下加入参数
	 * @param value int, String,byte[] 等值
	 */
	public void nextValue(Object value){
		if(value == null){
			_nextValue(value);
		}
		else
		{
			LDConvert convert = LDConvert.getConvert(value.getClass());
			if(convert == null) {
				_nextValue(value);
			}
			else
				_nextValue(convert.convert(value).value);
		}
	}
	
	public void nextValue(int value) {
		_nextValue(value);
	}
	
	public void nextValue(String value) {
		_nextValue(value);
	}
	
	public void nextValue(char value) {
		_nextValue(value);
	}
	
	public void nextValue(byte[] value) {
		_nextValue(value);
	}
	
	public void nextValue(BigDecimal value) {
		_nextValue(value);
	}
	
	protected void _nextValue(Object value){
		if(rowCount == 0){
			rowCount = 1;
		}
		curColumn++;
		int colCount = this.getColumnCount();
		if(curColumn>colCount) {
			curColumn = 1;
			rowCount++;
		}
		this.values.add(value);
	}
	
	public Object getValue(String fieldName) {
		return this.getValue(fieldName, 1);
	}
	
	public Object getValue(String fieldName,int rowIndex) {
		int fIndex = this.fieldNames.indexOf(fieldName);
		if(fIndex == -1){
			return null;
		}
		if(rowIndex<1)
			rowIndex = 1;
		return this.values.get((rowIndex-1)*this.getColumnCount()+fIndex);
	}
	
	public Object getValue(int colIndex, int rowIndex) {
		if(rowIndex<1)
			rowIndex = 1;
		return this.values.get((rowIndex-1)*this.getColumnCount()+colIndex-1);
	}
	
	
	static PFieldName ps = new PFieldName();
	static PChar pc = new PChar();
	
	public DataSet setBytes(byte[] bytes) throws IOException{
		ByteArrayInputStream input = new ByteArrayInputStream(bytes);
		return setBytes(input);
	}
	public DataSet setBytes(ByteArrayInputStream input) throws IOException{
		this.version = input.read();
		byte colCount = (byte) input.read();
		this.rowCount = EndianUtils.readSwappedInteger(input);
		this.fieldNames.clear();
		this.fieldIndexs.clear();
		this.fields.clear();
		this.values.clear();
		
		ArrayList<LDPack> plist = new ArrayList<LDPack>();
		
		for(int i=0;i<colCount;i++) {
			String name = ps.unpack(input);
			Character type = pc.unpack(input);
			this.fieldNames.add(name);
			this.fields.put(name, type);
			this.fieldIndexs.put(name, i);
			plist.add(LDPack.getPack(type));
		}
		
		for(int y=0;y<rowCount;y++){
			for(int x=0;x<colCount;x++){
				LDPack pack = plist.get(x);
				this.values.add(pack.unpack(input));
			}
		}
		return this;
	}
	
	public byte[] getBytes() throws IOException{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		output.write(this.getVersion());
		byte colCount = (byte) this.getColumnCount();
		output.write(colCount);
		EndianUtils.writeSwappedInteger(output,getRowCount());
		
		ArrayList<LDPack> plist = new ArrayList<LDPack>();
		
		for(String name : fieldNames){
			ps.pack(output, name);
			Character type = fields.get(name);
			pc.pack(output, type);
			plist.add(LDPack.getPack(type));
		}
		
		for(int y=0;y<rowCount;y++){
			if(removeRowSet.contains(y)) {
				continue;
			}
			for(int x=0;x<colCount;x++){
				LDPack pack = plist.get(x);
				Object value = CastUtils.cast(this.values.get(y*colCount+x),pack.getType());
				pack.pack(output, value);
			}
		}
		output.flush();
		return output.toByteArray();
	}
	
	protected HashSet<Integer> removeRowSet = new HashSet<Integer>();
	
	public void deleteRow(int row) {
		removeRowSet.add(row);
	}
	
	@Override
	public String toString() {
		StringBuffer sbuf = new StringBuffer("DataSet:");
		int colCount = this.getColumnCount();
		int rowCount = this.getRowCount();
		sbuf.append("{version="+this.version+",column="+colCount+",row="+rowCount+"}\n");
		for(String field : fieldNames){
			sbuf.append(field+"\t");
		}
		sbuf.append('\n');
		for(int i=0;i<this.values.size();i++) {
			sbuf.append(this.values.get(i)+"\t");
			if(i%colCount==colCount-1){
				sbuf.append('\n');
			}
		}
		return sbuf.toString();
	}
	
}
