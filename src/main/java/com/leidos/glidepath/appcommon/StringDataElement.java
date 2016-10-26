package com.leidos.glidepath.appcommon;

public class StringDataElement extends DataElement {

	public StringDataElement(String val){
		super();
		value_ = val;
	}
	
	public String				value(){
		//returns the value of the data element
		
		return value_;
	}
	
	////////////////////////////////////////////////
	
	protected String value_; //element value
}
