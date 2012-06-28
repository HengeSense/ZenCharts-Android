package zencharts.data;

import java.io.Serializable;
import java.util.Date;

import android.text.format.DateFormat;

public class DatePoint implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public long timeStamp;
	public int value;
	public String label = "";
	public String dateString = "";
	
	public DatePoint(long inTimestamp, int inValue, String inlabel) {
		super();
		timeStamp = inTimestamp;
		value = inValue;
		label = inlabel;
		
		Date date = new Date();
		date.setTime(timeStamp);
		dateString = DateFormat.format("MM/dd/yy", date).toString();
	}
}