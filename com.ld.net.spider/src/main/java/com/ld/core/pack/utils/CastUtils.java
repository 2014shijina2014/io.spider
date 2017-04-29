package com.ld.core.pack.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ld.core.pack.DataSetException;
import com.ld.core.pack.LDDataType;

public class CastUtils {
	
	static Logger logger = LoggerFactory.getLogger(CastUtils.class);
	
	public static String toString(Object value) {
		if (value == null) {
			return null;
		}

		return value.toString();
	}
	
	public static Byte toByte(Object value) {
		if (value == null) {
			return null;
		}

		if (value instanceof Number) {
			return ((Number) value).byteValue();
		}

		if (value instanceof String) {
			String strVal = (String) value;
			if (strVal.length() == 0) {
				return null;
			}

			if ("null".equals(strVal) || "NULL".equals(strVal)) {
				return null;
			}

			return Byte.parseByte(strVal);
		}

		throw new DataSetException("can not cast to byte, value : " + value);
	}

	public static Character toChar(Object value) {
		if (value == null) {
			return null;
		}

		if (value instanceof Character) {
			return (Character) value;
		}

		if (value instanceof String) {
			String strVal = (String) value;

			if (strVal.length() == 0) {
				return null;
			}

			if (strVal.length() != 1) {
				throw new DataSetException("can not cast to byte, value : " + value);
			}

			return strVal.charAt(0);
		}

		throw new DataSetException("can not cast to byte, value : " + value);
	}
	public static Integer toInteger(Object value) {
		if (value == null) {
			return null;
		}

		if (value instanceof Integer) {
			return (Integer) value;
		}

		if (value instanceof Number) {
			return ((Number) value).intValue();
		}

		if (value instanceof String) {
			String strVal = (String) value;

			if (strVal.length() == 0) {
				return null;
			}

			if ("null".equals(strVal)) {
				return null;
			}

			if ("null".equals(strVal) || "NULL".equals(strVal)) {
				return null;
			}

			return Integer.parseInt(strVal);
		}
		throw new DataSetException("can't cast to integer value:"+value);
	}

	public static Long toLong(Object value) {
		if (value == null) {
			return null;
		}

		if (value instanceof Number) {
			return ((Number) value).longValue();
		}

		if (value instanceof String) {
			String strVal = (String) value;
			if (strVal.length() == 0) {
				return null;
			}

			if ("null".equals(strVal) || "NULL".equals(strVal)) {
				return null;
			}

			try {
				return Long.parseLong(strVal);
			} catch (NumberFormatException ex) {
				//
			}

		}

		throw new DataSetException("can not cast to long, value : " + value);
	}
	 public static Double toDouble(Object value) {
	        if (value == null) {
	            return null;
	        }

	        if (value instanceof Number) {
	            return ((Number) value).doubleValue();
	        }

	        if (value instanceof String) {
	            String strVal = value.toString();
	            if (strVal.length() == 0) {
	                return null;
	            }
	            
	            if ("null".equals(strVal) || "NULL".equals(strVal)) {
	                return null;
	            }
	            
	            return Double.parseDouble(strVal);
	        }

	        throw new DataSetException("can not cast to double, value : " + value);
	    }
	public static BigDecimal toBigDecimal(Object value) {
		if (value == null) {
			return null;
		}

		if (value instanceof BigDecimal) {
			return (BigDecimal) value;
		}

		if (value instanceof BigInteger) {
			return new BigDecimal((BigInteger) value);
		}

		String strVal = value.toString().trim();
		if (strVal.length() == 0) {
			return null;
		}

		return new BigDecimal(strVal);
	}

	public static byte[] toBytes(Object value) {
		if (value instanceof byte[]) {
			return (byte[]) value;
		}

		if (value instanceof String) {
			return ((String) value).getBytes();
		}
		throw new DataSetException("can not cast to int, value : " + value);
	}
	
	public static <K,V> Map<K,V> toMap(Object value,Class<Map<K, V>> cls) {
		Map<K, V> ret = new HashMap<K, V>();
		if(value instanceof Map) {
			ret.putAll((Map<? extends K, ? extends V>) value);
		}
		
		return ret;
	}
	
	public static Object cast(Object value,Character type) {
		switch(type){
		case LDDataType.BYTE_ARRAY:
			return toBytes(value);
		case LDDataType.CHAR:
			return toChar(value);
		case LDDataType.DOUBLE:
			return toDouble(value);
		case LDDataType.DECIMAL:
			return toBigDecimal(value);
		case LDDataType.INT:
			return toInteger(value);
		case LDDataType.LONG:
			return toLong(value);
		case LDDataType.STRING:
			return toString(value);
		}
		return value;
	}
	
	public static <T> T cast(Object obj,Class<T> clazz) {
		if(obj == null)
			return null;
		if(obj.getClass() == clazz) {
			return (T)obj;
		}

        if (clazz == char.class || clazz == Character.class) {
            return (T) toChar(obj);
        }

        if (clazz == int.class || clazz == Integer.class) {
            return (T) toInteger(obj);
        }

        if (clazz == long.class || clazz == Long.class) {
            return (T) toLong(obj);
        }

        if (clazz == double.class || clazz == Double.class) {
            return (T) toDouble(obj);
        }

        if (clazz == String.class) {
            return (T) toString(obj);
        }

        if (clazz == BigDecimal.class) {
            return (T) toBigDecimal(obj);
        }
        
        logger.debug("cast to "+clazz.getName()+" failed,value="+obj);
        
		return null;
	}

}
