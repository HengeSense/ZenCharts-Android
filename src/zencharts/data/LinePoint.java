package zencharts.data;

import java.io.Serializable;
import java.util.Date;

import android.text.format.DateFormat;

public class LinePoint implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public int value;
	public String label = "";
	public String xstring = "";
	
	public LinePoint(int inValue, String inlabel, String inxlabel) {
		super();
		value = inValue;
		label = inlabel;
		
		xstring = inxlabel;
	}
}